package dev.emortal;

import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.objects.PhysicsBody;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;
import dev.emortal.commands.*;
import dev.emortal.objects.BlockRigidBody;
import dev.emortal.objects.LanternPhysics;
import dev.emortal.objects.MinecraftPhysicsObject;
import dev.emortal.tools.*;
import electrostatic4j.snaploader.LibraryInfo;
import electrostatic4j.snaploader.LoadingCriterion;
import electrostatic4j.snaploader.NativeBinaryLoader;
import electrostatic4j.snaploader.filesystem.DirectoryPath;
import electrostatic4j.snaploader.platform.NativeDynamicLibrary;
import electrostatic4j.snaploader.platform.util.PlatformPredicate;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.ServerFlag;
import net.minestom.server.command.CommandManager;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.server.ServerTickMonitorEvent;
import net.minestom.server.extras.MojangAuth;
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

import java.text.DecimalFormat;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static dev.emortal.utils.CoordinateUtils.toVector3;

public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private static final Set<Point> BLOCKS_IN_SPHERE = SphereUtil.getBlocksInSphere(5);

    public static void main(String[] args) throws Exception {
        LibraryInfo info = new LibraryInfo(
                new DirectoryPath("linux/x86-64/com/github/stephengold"),
                "bulletjme", DirectoryPath.USER_DIR);
        NativeBinaryLoader loader = new NativeBinaryLoader(info);
        NativeDynamicLibrary[] libraries = new NativeDynamicLibrary[]{
//                new NativeDynamicLibrary("native/linux/arm64", PlatformPredicate.LINUX_ARM_64),
//                new NativeDynamicLibrary("native/linux/arm32", PlatformPredicate.LINUX_ARM_32),
                new NativeDynamicLibrary("native/linux/x86_64", PlatformPredicate.LINUX_X86_64),
//                new NativeDynamicLibrary("native/osx/arm64", PlatformPredicate.MACOS_ARM_64),
//                new NativeDynamicLibrary("native/osx/x86_64", PlatformPredicate.MACOS_X86_64),
                new NativeDynamicLibrary("native/windows/x86_64", PlatformPredicate.WIN_X86_64)
        };
        loader.registerNativeLibraries(libraries).initPlatformLibrary();
        loader.loadLibrary(LoadingCriterion.CLEAN_EXTRACTION);

        System.setProperty("minestom.tps", "60");


        MinecraftServer server = MinecraftServer.init();
        MojangAuth.init();




        DimensionType fullbrightDimension = DimensionType.builder().ambientLight(1f).build();
        var fullbright = MinecraftServer.getDimensionTypeRegistry().register(Key.key("fullbright"), fullbrightDimension);

        InstanceContainer instance = MinecraftServer.getInstanceManager().createInstanceContainer(fullbright);

        for (int x = -20; x < 20; x++) {
            for (int z = -20; z < 20; z++) {
                instance.setBlock(x, -1, z, Block.GRASS_BLOCK);
            }
        }
        instance.setTimeSynchronizationTicks(0);
        instance.setTimeRate(0);

        MinecraftPhysics physicsHandler = new MinecraftPhysics(instance);

        BossBar bossBar = BossBar.bossBar(Component.empty(), 1f, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS);

        GlobalEventHandler global = MinecraftServer.getGlobalEventHandler();

        global.addListener(PlayerSpawnEvent.class, e -> {
            e.getPlayer().showBossBar(bossBar);
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

            CollisionShape boxShape = new BoxCollisionShape((float) (e.getPlayer().getBoundingBox().width()/2f), (float) (e.getPlayer().getBoundingBox().height()/2f), (float) (e.getPlayer().getBoundingBox().depth()/2f));
            PhysicsRigidBody playerRigidBody = new PhysicsRigidBody(boxShape, PhysicsRigidBody.massForStatic);
            physicsHandler.getPhysicsSpace().addCollisionObject(playerRigidBody);

            e.getPlayer().setTag(MinecraftPhysics.PLAYER_RIGID_BODY_TAG, playerRigidBody);

            e.getPlayer().scheduler().buildTask(() -> {
                playerRigidBody.activate();
                playerRigidBody.setPhysicsLocation(toVector3(e.getPlayer().getPosition().add(0, 1, 0)));
            }).repeat(TaskSchedule.tick(1)).schedule();
        });

        global.addListener(AsyncPlayerConfigurationEvent.class, e -> {
            e.setSpawningInstance(instance);
            e.getPlayer().setRespawnPoint(new Pos(0, 20, 0));
        });

        physicsHandler.start();

        global.addListener(ItemDropEvent.class, e -> {
            e.setCancelled(true);
        });

        DecimalFormat dec = new DecimalFormat("0.00");
        global.addListener(ServerTickMonitorEvent.class, e -> {
            double tickTime = Math.floor(e.getTickMonitor().getTickTime() * 100.0) / 100.0;
            bossBar.name(
                    Component.text()
                            .append(Component.text("MSPT: " + dec.format(tickTime)))
            );
            bossBar.progress(Math.min((float)tickTime / (float)(1000 / ServerFlag.SERVER_TICKS_PER_SECOND), 1f));

            if (tickTime > MinecraftServer.TICK_MS) {
                bossBar.color(BossBar.Color.RED);
            } else {
                bossBar.color(BossBar.Color.GREEN);
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

            if (e.getBlock().compare(Block.LANTERN)) {
                e.setCancelled(true);

                LanternPhysics lantern = new LanternPhysics(physicsHandler, null, new Vec(0.1f, 0.4f, 0.1f), 1, new Vector3f(e.getBlockPosition().blockX() + 0.5f, e.getBlockPosition().blockY() + 1.5f, e.getBlockPosition().blockZ() + 0.5f));
                lantern.setInstance();
            }

            if (e.getBlock().compare(Block.TNT)) {
                e.setCancelled(true);

                Entity entity = new Entity(EntityType.TNT);
//                entity.editEntityMeta(PrimedTntMeta.class, meta -> {
//                    meta.setFuseTime(60);
//                });
                // TODO: Causes network protocol error - minestom bug
                entity.setInstance(instance, e.getBlockPosition().add(0.5, 0, 0.5));

                instance.scheduler().buildTask(() -> {
                    ExplosionPacket packet = new ExplosionPacket(entity.getPosition(), Vec.ZERO, Particle.EXPLOSION_EMITTER, SoundEvent.ENTITY_GENERIC_EXPLODE);
                    instance.sendGroupedPacket(packet);
                    entity.remove();

                    ThreadLocalRandom rand = ThreadLocalRandom.current();

                    for (MinecraftPhysicsObject cube : physicsHandler.getObjects()) {
                        if (cube.getEntity() == null) continue;
                        if (cube.getEntity().getPosition().distanceSquared(blockPos.add(0.5)) > 5*5) continue;

                        PhysicsRigidBody cubeRigidBody = (PhysicsRigidBody) cube.getCollisionObject();

                        Vec velocity = cube.getEntity().getPosition().sub(blockPos.add(0.5)).asVec().normalize().mul(4, 8, 4).mul(rand.nextDouble(1.2, 2)).mul(TntStrengthCommand.TNT_STRENGTH);
                        cube.getCollisionObject().activate(true);

                        Vector3f linearVelocity = new Vector3f();
                        cubeRigidBody.getLinearVelocity(linearVelocity);
                        cubeRigidBody.setLinearVelocity(linearVelocity.add(toVector3(velocity)));
                        cubeRigidBody.setAngularVelocity(toVector3(velocity)); // probably completely wrong but it looks nice
                    }

                    List<WorldBlock> nearbyBlocks = SphereUtil.getNearbyBlocks(blockPos.add(0.5), BLOCKS_IN_SPHERE, instance, block -> !block.block().isAir() && !block.block().compare(Block.GRASS_BLOCK));
                    AbsoluteBlockBatch batch = new AbsoluteBlockBatch();
                    for (WorldBlock nearbyBlock : nearbyBlocks) {
                        var cube = new BlockRigidBody(physicsHandler, toVector3(nearbyBlock.position()), new Vec(0.5), 1, true, nearbyBlock.block());
                        cube.setInstance();
                        PhysicsRigidBody cubeRigidBody = (PhysicsRigidBody) cube.getCollisionObject();

                        Vec velocity = nearbyBlock.position().sub(blockPos.add(0.5)).asVec().normalize().mul(4, 8, 4).mul(rand.nextDouble(1.2, 2)).mul(TntStrengthCommand.TNT_STRENGTH);
                        cube.getCollisionObject().activate(true);
                        Vector3f linearVelocity = new Vector3f();
                        cubeRigidBody.getLinearVelocity(linearVelocity);
                        cubeRigidBody.setLinearVelocity(linearVelocity.add(toVector3(velocity)));
                        cubeRigidBody.setAngularVelocity(toVector3(velocity)); // probably completely wrong but it looks nice
                        batch.setBlock(nearbyBlock.position(), Block.AIR);

                        Block block = instance.getBlock(nearbyBlock.position().sub(0.5));
                        MinecraftPhysicsObject physicsBlock = block.getTag(MinecraftPhysics.PHYSICS_BLOCK_TAG);
                        if (physicsBlock != null) {
                            physicsBlock.destroy();
                        }
                    }
                    batch.apply(instance, null);
                }).delay(TaskSchedule.tick(3 * ServerFlag.SERVER_TICKS_PER_SECOND)).schedule();
            }



            if (!e.isCancelled()) {
                MinecraftPhysicsObject object = new BlockRigidBody(physicsHandler, toVector3(blockPos.add(0.5)), new Vec(0.5), PhysicsBody.massForStatic, false, Block.BLUE_STAINED_GLASS);
                object.setInstance();

                e.setBlock(e.getBlock().withTag(MinecraftPhysics.PHYSICS_BLOCK_TAG, object));
            }
        });

        global.addListener(PlayerBlockBreakEvent.class, e -> {
            MinecraftPhysicsObject physicsObject = e.getBlock().getTag(MinecraftPhysics.PHYSICS_BLOCK_TAG);
            if (physicsObject != null) {
                physicsObject.destroy();
            }
        });

        CommandManager commandManager = MinecraftServer.getCommandManager();
        commandManager.register(new ChainLengthCommand());
        commandManager.register(new ClearCommand(physicsHandler));
        commandManager.register(new PerformanceCommand(MinecraftServer.getGlobalEventHandler(), physicsHandler));
        commandManager.register(new PlayerSizeCommand());
        commandManager.register(new TntStrengthCommand());

        server.start("0.0.0.0", 25565);
    }

}