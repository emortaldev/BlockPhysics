package dev.emortal;

import com.github.stephengold.joltjni.Body;
import com.github.stephengold.joltjni.BodyCreationSettings;
import com.github.stephengold.joltjni.Vec3;
import com.github.stephengold.joltjni.enumerate.EActivation;
import dev.emortal.commands.*;
import dev.emortal.objects.BlockRigidBody;
import dev.emortal.objects.MinecraftPhysicsObject;
import dev.emortal.tools.*;
import dev.emortal.utils.PolarChainFix;
import dev.emortal.worldmesh.ChunkMesher;
import electrostatic4j.snaploader.LibraryInfo;
import electrostatic4j.snaploader.LoadingCriterion;
import electrostatic4j.snaploader.NativeBinaryLoader;
import electrostatic4j.snaploader.filesystem.DirectoryPath;
import electrostatic4j.snaploader.platform.NativeDynamicLibrary;
import electrostatic4j.snaploader.platform.util.PlatformPredicate;
import net.hollowcube.polar.PolarLoader;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import net.minestom.server.ServerFlag;
import net.minestom.server.command.CommandManager;
import net.minestom.server.coordinate.CoordConversion;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.metadata.other.PrimedTntMeta;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.server.ServerTickMonitorEvent;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.batch.AbsoluteBlockBatch;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.ExplosionPacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.world.DimensionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import static dev.emortal.utils.CoordinateUtils.*;

public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private static final Set<Point> BLOCKS_IN_SPHERE = SphereUtil.getBlocksInSphere(5);
    public static final Map<Long, Integer> CHUNK_MESH_MAP = new ConcurrentHashMap<>();
    public static final Map<Long, BodyCreationSettings> CHUNK_MESH_SETTINGS = new ConcurrentHashMap<>();

    void main() {
        LibraryInfo info = new LibraryInfo(null, "joltjni", DirectoryPath.USER_DIR);
        NativeBinaryLoader loader = new NativeBinaryLoader(info);
        NativeDynamicLibrary[] libraries = {
//                new NativeDynamicLibrary("linux/aarch64/com/github/stephengold", PlatformPredicate.LINUX_ARM_64),
//                new NativeDynamicLibrary("linux/armhf/com/github/stephengold", PlatformPredicate.LINUX_ARM_32),
                new NativeDynamicLibrary("linux/x86-64/com/github/stephengold", PlatformPredicate.LINUX_X86_64),
//                new NativeDynamicLibrary("osx/aarch64/com/github/stephengold", PlatformPredicate.MACOS_ARM_64),
//                new NativeDynamicLibrary("osx/x86-64/com/github/stephengold", PlatformPredicate.MACOS_X86_64),
                new NativeDynamicLibrary("windows/x86-64/com/github/stephengold", PlatformPredicate.WIN_X86_64)
        };
        loader.registerNativeLibraries(libraries).initPlatformLibrary();
        try {
            loader.loadLibrary(LoadingCriterion.CLEAN_EXTRACTION);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to load a Jolt-JNI native library!");
        }

        System.setProperty("minestom.tps", "20");
        System.setProperty("blockphysics.fps", "60");

//        MinecraftServer server = MinecraftServer.init(); - Offline mode
        MinecraftServer server = MinecraftServer.init(new Auth.Online());
        MinecraftServer.setCompressionThreshold(0); // Use only for local servers!

        DimensionType fullbrightDimension = DimensionType.builder().ambientLight(1f).build();
        var fullbright = MinecraftServer.getDimensionTypeRegistry().register(Key.key("fullbright"), fullbrightDimension);

        InstanceContainer instance = MinecraftServer.getInstanceManager().createInstanceContainer(fullbright);
        instance.enableAutoChunkLoad(true);

        MinecraftPhysics physicsHandler = new MinecraftPhysics(instance);

        byte[] polarBytes;
        try {
            polarBytes = Files.readAllBytes(Path.of("./emclobby.polar"));
            PolarLoader.streamLoad(instance, Channels.newChannel(new ByteArrayInputStream(polarBytes)), polarBytes.length, new PolarChainFix(), null, true).join();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        int chunkLoadRadius = 3;
        for (int x = -chunkLoadRadius; x < chunkLoadRadius; x++) {
            for (int z = -chunkLoadRadius; z < chunkLoadRadius; z++) {
                instance.loadChunk(x, z).thenAccept(c -> {
                    instance.scheduleNextTick(a -> {
                        refreshChunk(physicsHandler, c);
                    });
                });
            }
        }

        instance.setTimeSynchronizationTicks(0);
        instance.setTimeRate(0);

        BossBar bossBar = BossBar.bossBar(Component.empty(), 1f, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS);
        BossBar bossBar2 = BossBar.bossBar(Component.empty(), 1f, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS);

        GlobalEventHandler global = MinecraftServer.getGlobalEventHandler();

        global.addListener(PlayerSpawnEvent.class, e -> {
            e.getPlayer().showBossBar(bossBar);
            e.getPlayer().showBossBar(bossBar2);
            e.getPlayer().setGameMode(GameMode.CREATIVE);

            e.getPlayer().sendMessage(Component.text("Welcome to your physics playground!"));
            e.getPlayer().sendMessage(Component.text("Check your inventory for more tools"));
            e.getPlayer().sendMessage(Component.text("Use /clear to clear all objects in the world"));

            e.getPlayer().getInventory().setItemStack(9, new DiamondLayerTool(e.getPlayer(), physicsHandler).getItem());
            e.getPlayer().getInventory().setItemStack(7, new DeleteTool(e.getPlayer(), physicsHandler).getItem());
            e.getPlayer().getInventory().setItemStack(6, new PlayerSpawnerTool(e.getPlayer(), physicsHandler).getItem());
            e.getPlayer().getInventory().setItemStack(5, new GrabberTool(e.getPlayer(), physicsHandler).getItem());
            e.getPlayer().getInventory().setItemStack(4, new WeldTool(e.getPlayer(), physicsHandler).getItem());
            e.getPlayer().getInventory().setItemStack(3, new ChainTool(e.getPlayer(), physicsHandler).getItem());

            e.getPlayer().getInventory().setItemStack(2, ItemStack.of(Material.TNT));
            e.getPlayer().getInventory().setItemStack(1, ItemStack.of(Material.STONE));

//            CollisionShape boxShape = new BoxCollisionShape((float) (e.getPlayer().getBoundingBox().width()/2f), (float) (e.getPlayer().getBoundingBox().height()/2f), (float) (e.getPlayer().getBoundingBox().depth()/2f));
//            PhysicsRigidBody playerRigidBody = new PhysicsRigidBody(boxShape, PhysicsRigidBody.massForStatic);
//            physicsHandler.getPhysicsSystem().addCollisionObject(playerRigidBody);
//            e.getPlayer().setTag(MinecraftPhysics.PLAYER_RIGID_BODY_TAG, playerRigidBody);
//            e.getPlayer().scheduler().buildTask(() -> {
//                playerRigidBody.activate();
//                playerRigidBody.setPhysicsLocation(toVec3(e.getPlayer().getPosition().add(0, 1, 0)));
//            }).repeat(TaskSchedule.tick(1)).schedule();
        });

        global.addListener(AsyncPlayerConfigurationEvent.class, e -> {
            e.setSpawningInstance(instance);
            e.getPlayer().setRespawnPoint(new Pos(0, 80, 0));
        });

        global.addListener(ItemDropEvent.class, e -> {
            e.setCancelled(true);
        });

        int physicsFps = Integer.parseInt(System.getProperty("blockphysics.fps"));
        int physicsFrameMs = 1000 / physicsFps;
        DecimalFormat dec = new DecimalFormat("0.00");
        global.addListener(ServerTickMonitorEvent.class, e -> {
            double tickTime = Math.floor(e.getTickMonitor().getTickTime() * 100.0) / 100.0;
            double physicsFpsTime = physicsHandler.getLastNanos().get() / 1_000_000.0;

            bossBar.name(
                    Component.text()
                            .append(Component.text("MSPT: " + dec.format(tickTime)))
            );
            bossBar.progress((float) Math.min(tickTime / MinecraftServer.TICK_MS, 1f));

            bossBar2.name(
                    Component.text()
                            .append(Component.text("Physics MSPT: " + dec.format(physicsFpsTime)))
            );
            bossBar2.progress((float) Math.min(physicsFpsTime / physicsFrameMs, 1f));

            if (tickTime > MinecraftServer.TICK_MS) {
                bossBar.color(BossBar.Color.RED);
            } else {
                bossBar.color(BossBar.Color.GREEN);
            }

            if (tickTime > MinecraftServer.TICK_MS) {
                bossBar2.color(BossBar.Color.RED);
            } else {
                bossBar2.color(BossBar.Color.GREEN);
            }
        });

        // TODO: kill barrier / floor

        global.addListener(PlayerBlockPlaceEvent.class, e -> {
            Point blockPos = e.getBlockPosition();

            if (e.getBlock().compare(Block.PLAYER_HEAD) || e.getBlock().compare(Block.PLAYER_WALL_HEAD)) {
                e.setCancelled(true);
            }

//            if (e.getBlock().compare(Block.TRAPDOOR)) {
//                e.setCancelled(true);
//
//                new TrapdoorPhysics(physicsHandler, null, instance, new Vector3f(0.5f, 0.5f, 0.1f), 1, new Vector3f(e.getBlockPosition().blockX() + 0.5f, e.getBlockPosition().blockY() + 1.5f, e.getBlockPosition().blockZ() + 0.5f));
//            }

//            if (e.getBlock().compare(Block.LANTERN)) {
//                e.setCancelled(true);
//
//                LanternPhysics lantern = new LanternPhysics(physicsHandler, null, new Vec(0.1f, 0.4f, 0.1f), 1, new Vec3(e.getBlockPosition().blockX() + 0.5f, e.getBlockPosition().blockY() + 1.5f, e.getBlockPosition().blockZ() + 0.5f));
//                lantern.setInstance();
//            }

            if (e.getBlock().compare(Block.TNT)) {
                e.setCancelled(true);

                Entity entity = new Entity(EntityType.TNT);
                entity.setNoGravity(true);
                entity.editEntityMeta(PrimedTntMeta.class, meta -> {
                    meta.setFuseTime(60);
                });
                entity.setInstance(instance, e.getBlockPosition().add(0.5, 0, 0.5));

                instance.scheduler().buildTask(() -> {
                    ExplosionPacket packet = new ExplosionPacket(entity.getPosition(), 2f, 0, null, Particle.EXPLOSION_EMITTER, SoundEvent.ENTITY_GENERIC_EXPLODE, List.of());
                    instance.sendGroupedPacket(packet);
                    entity.remove();

                    ThreadLocalRandom rand = ThreadLocalRandom.current();

                    for (MinecraftPhysicsObject cube : physicsHandler.getObjects()) {
                        if (cube.getEntity() == null) continue;
                        if (cube.getEntity().getPosition().distanceSquared(blockPos.add(0.5)) > 5*5) continue;

                        Body body = cube.getBody();

                        cube.activate();

                        Vec velocity = cube.getEntity().getPosition().sub(blockPos.add(0.5)).asVec().normalize().mul(4, 8, 4).mul(rand.nextDouble(1.2, 2)).mul(TntStrengthCommand.TNT_STRENGTH);

                        Vec3 linearVelocity = body.getLinearVelocity();
                        body.setLinearVelocity(toVec3(velocity.add(toVec(linearVelocity))));
                        body.setAngularVelocity(toVec3(velocity)); // probably completely wrong but it looks nice
                    }

                    List<WorldBlock> nearbyBlocks = SphereUtil.getNearbyBlocks(blockPos.add(0.5), BLOCKS_IN_SPHERE, instance, block -> !block.block().isAir() && !block.block().compare(Block.GRASS_BLOCK));
                    AbsoluteBlockBatch batch = new AbsoluteBlockBatch();
                    for (WorldBlock nearbyBlock : nearbyBlocks) {
                        var cube = new BlockRigidBody(physicsHandler, toRVec3(nearbyBlock.position()), new Vec(0.5), true, nearbyBlock.block());
                        cube.setInstance();
                        Body cubeBody = cube.getBody();
                        cube.activate();

                        Vec velocity = nearbyBlock.position().sub(blockPos.add(0.5)).asVec().normalize().mul(4, 8, 4).mul(rand.nextDouble(1.2, 2)).mul(TntStrengthCommand.TNT_STRENGTH);

                        Vec3 linearVelocity = cubeBody.getLinearVelocity();
                        cubeBody.setLinearVelocity(toVec3(velocity.add(toVec(linearVelocity))));
                        cubeBody.setAngularVelocity(toVec3(velocity)); // probably completely wrong but it looks nice
                        batch.setBlock(nearbyBlock.position(), Block.AIR);
                    }
                    batch.apply(instance, () -> {
                        for (int x = -1; x <= 1; x++) {
                            for (int z = -1; z <= 1; z++) {
                                refreshChunk(physicsHandler, instance.getChunkAt(blockPos.add(x * Chunk.CHUNK_SIZE_X, 0, z * Chunk.CHUNK_SIZE_Z)));
                            }
                        }
                    });
                }).delay(TaskSchedule.tick(3 * ServerFlag.SERVER_TICKS_PER_SECOND)).schedule();
            }

            if (!e.isCancelled()) {
                refreshChunk(physicsHandler, e.getInstance().getChunkAt(e.getBlockPosition()));
            }
        });

        global.addListener(PlayerBlockBreakEvent.class, e -> {
            refreshChunk(physicsHandler, e.getInstance().getChunkAt(e.getBlockPosition()));
        });

        CommandManager commandManager = MinecraftServer.getCommandManager();
        commandManager.register(new ChainLengthCommand());
        commandManager.register(new ClearCommand(physicsHandler));
        commandManager.register(new PerformanceCommand(MinecraftServer.getGlobalEventHandler(), physicsHandler));
        commandManager.register(new PlayerSizeCommand());
        commandManager.register(new TntStrengthCommand());

        server.start("0.0.0.0", 25565);
    }

    public static void refreshChunk(MinecraftPhysics physics, Chunk chunk) {
        long index = CoordConversion.chunkIndex(chunk.getChunkX(), chunk.getChunkZ());
        Integer previous = CHUNK_MESH_MAP.get(index);
        if (previous != null) {
            physics.getBodyInterface().removeBody(previous);
            physics.getBodyInterface().destroyBody(previous);
        }

        LOGGER.info("Refreshing chunk mesh at chunk X:{} Z:{}", chunk.getChunkX(), chunk.getChunkZ());

        long before = System.nanoTime();
        BodyCreationSettings settings = ChunkMesher.createChunk(chunk);
        long after = System.nanoTime();

        LOGGER.info("Took {}ns to regenerate chunk mesh", after - before);

        CHUNK_MESH_SETTINGS.put(index, settings);
        if (settings == null) return;
        Body body = physics.getBodyInterface().createBody(settings);
        CHUNK_MESH_MAP.put(index, body.getId());

        physics.getBodyInterface().addBody(body, EActivation.DontActivate);
    }

}