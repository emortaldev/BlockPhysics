package dev.emortal;

import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.objects.PhysicsBody;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;
import com.jme3.system.NativeLibraryLoader;
import dev.emortal.commands.*;
import dev.emortal.objects.BlockRigidBody;
import dev.emortal.objects.ChainPhysics;
import dev.emortal.objects.LanternPhysics;
import dev.emortal.objects.MinecraftPhysicsObject;
import dev.emortal.rayfast.area.area3d.Area3d;
import dev.emortal.rayfast.area.area3d.Area3dRectangularPrism;
import dev.emortal.rayfast.vector.Vector3d;
import dev.emortal.tools.*;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.ServerFlag;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.command.CommandManager;
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
import net.minestom.server.extras.MojangAuth;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.batch.AbsoluteBlockBatch;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.ExplosionPacket;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.utils.NamespaceID;
import net.minestom.server.world.DimensionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import static dev.emortal.objects.BlockRigidBody.toVector3;

public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private static final Set<Point> BLOCKS_IN_SPHERE = SphereUtil.getBlocksInSphere(5);
    public static final Map<UUID, PhysicsRigidBody> PLAYER_OBJECT_MAP = new HashMap<>();

    private static final Map<Point, MinecraftPhysicsObject> PHYSICS_BLOCK_MAP = new HashMap<>();

    private static final Map<BoundingBox, Area3d> boundingBoxToArea3dMap = new HashMap<>();

    public static boolean paused = false;

    public static void main(String[] args) {
        System.setProperty("minestom.tps", "60");
        NativeLibraryLoader.loadLibbulletjme(true, new File("natives/"), "Release", "Sp");

        MinecraftServer server = MinecraftServer.init();
        MojangAuth.init();

        Area3d.CONVERTER.register(BoundingBox.class, box -> {
            boundingBoxToArea3dMap.computeIfAbsent(box, bb -> Area3dRectangularPrism.of(
                    bb.minX(), bb.minY(), bb.minZ(),
                    bb.maxX(), bb.maxY(), bb.maxZ()
            ));

            return boundingBoxToArea3dMap.get(box);
        });

        DimensionType fullbright = DimensionType.builder(NamespaceID.from("fullbright")).ambientLight(1f).build();
        MinecraftServer.getDimensionTypeManager().addDimension(fullbright);

        InstanceContainer instance = MinecraftServer.getInstanceManager().createInstanceContainer(fullbright);

        for (int x = -20; x < 20; x++) {
            for (int z = -20; z < 20; z++) {
                instance.setBlock(x, -1, z, Block.GRASS_BLOCK);
            }
        }
        instance.setTimeUpdate(null);
        instance.setTimeRate(0);

        MinecraftPhysicsHandler physicsHandler = new MinecraftPhysicsHandler(instance);

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

            e.getPlayer().getInventory().setItemStack(3, ItemStack.of(Material.CHAIN));
            e.getPlayer().getInventory().setItemStack(2, ItemStack.of(Material.TNT));
            e.getPlayer().getInventory().setItemStack(1, ItemStack.of(Material.STONE));

            CollisionShape boxShape = new BoxCollisionShape((float) (e.getPlayer().getBoundingBox().width()/2f), (float) (e.getPlayer().getBoundingBox().height()/2f), (float) (e.getPlayer().getBoundingBox().depth()/2f));
            PhysicsRigidBody playerRigidBody = new PhysicsRigidBody(boxShape, PhysicsRigidBody.massForStatic);
            physicsHandler.getPhysicsSpace().addCollisionObject(playerRigidBody);

            PLAYER_OBJECT_MAP.put(e.getPlayer().getUuid(), playerRigidBody);

            e.getPlayer().scheduler().buildTask(() -> {
                playerRigidBody.activate();
                playerRigidBody.setPhysicsLocation(toVector3(e.getPlayer().getPosition().add(0, 1, 0)));
            }).repeat(TaskSchedule.tick(1)).schedule();
        });

        global.addListener(AsyncPlayerConfigurationEvent.class, e -> {
            e.setSpawningInstance(instance);
            e.getPlayer().setRespawnPoint(new Pos(0, 20, 0));
        });

        instance.scheduler().buildTask(new Runnable() {
            long lastRan = System.nanoTime();
            @Override
            public void run() {
                long diff = System.nanoTime() - lastRan;
                float deltaTime = diff / 1_000_000_000f;

                lastRan = System.nanoTime();
                if (paused) return;
                physicsHandler.update(deltaTime);
            }
        }).repeat(TaskSchedule.tick(1)).schedule();

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
            bossBar.progress(Math.min((float)tickTime / (float)MinecraftServer.TICK_MS, 1f));

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

                new LanternPhysics(physicsHandler, null, instance, new Vector3f(0.1f, 0.4f, 0.1f), 1, new Vector3f(e.getBlockPosition().blockX() + 0.5f, e.getBlockPosition().blockY() + 1.5f, e.getBlockPosition().blockZ() + 0.5f));
            }
            if (e.getBlock().compare(Block.CHAIN)) {
                e.setCancelled(true);

                MinecraftPhysicsObject first = new ChainPhysics(physicsHandler, null, instance, new Vector3f(0.1f, 0.5f, 0.1f), 1, Block.DIAMOND_BLOCK, new Vector3f(e.getBlockPosition().blockX() + 0.5f, e.getBlockPosition().blockY() + 0.5f, e.getBlockPosition().blockZ() + 0.5f));
                MinecraftPhysicsObject lastLink = first;
                for (int links = 0; links < ChainLengthCommand.CHAIN_LENGTH; links++) {
                    lastLink = new ChainPhysics(physicsHandler, lastLink.getRigidBody(), instance, new Vector3f(0.1f, 0.5f, 0.1f), 1, Block.DIAMOND_BLOCK, new Vector3f(e.getBlockPosition().blockX() + 0.5f, e.getBlockPosition().blockY() + 0.5f, e.getBlockPosition().blockZ() + 0.5f));
                }
            }

            if (e.getBlock().compare(Block.TNT)) {
                e.setCancelled(true);

                Entity entity = new NoTickingEntity(EntityType.TNT);
                PrimedTntMeta tntMeta = (PrimedTntMeta) entity.getEntityMeta();
                tntMeta.setFuseTime(60);
                entity.setInstance(instance, e.getBlockPosition().add(0.5, 0, 0.5));

                instance.scheduler().buildTask(() -> {
                    ExplosionPacket packet = new ExplosionPacket(entity.getPosition().x(), entity.getPosition().y(), entity.getPosition().z(), 2f, new byte[0], 0f, 0f, 0f);
                    instance.sendGroupedPacket(packet);
                    entity.remove();

                    ThreadLocalRandom rand = ThreadLocalRandom.current();

                    for (MinecraftPhysicsObject cube : physicsHandler.objects) {
                        if (cube.getMeta() == null) continue;
                        if (cube.getEntity() == null) continue;

                        if (cube.getEntity().getPosition().distanceSquared(blockPos.add(0.5)) > 5*5) continue;
                        Vec velocity = Vec.fromPoint(cube.getEntity().getPosition().sub(blockPos.add(0.5))).normalize().mul(4, 8, 4).mul(rand.nextDouble(1.2, 2)).mul(TntStrengthCommand.TNT_STRENGTH);
                        cube.getRigidBody().activate();

                        Vector3f linearVelocity = new Vector3f();
                        cube.getRigidBody().getLinearVelocity(linearVelocity);
                        cube.getRigidBody().setLinearVelocity(linearVelocity.add(toVector3(velocity)));
                        cube.getRigidBody().setAngularVelocity(toVector3(velocity)); // probably completely wrong but it looks nice
                    }

                    List<WorldBlock> nearbyBlocks = SphereUtil.getNearbyBlocks(blockPos.add(0.5), BLOCKS_IN_SPHERE, instance, block -> !block.block().isAir() && !block.block().compare(Block.GRASS_BLOCK));
                    AbsoluteBlockBatch batch = new AbsoluteBlockBatch();
                    for (WorldBlock nearbyBlock : nearbyBlocks) {
                        var cube = new BlockRigidBody(physicsHandler, instance, new Vector3f((float)nearbyBlock.position().x(), (float)nearbyBlock.position().y(), (float)nearbyBlock.position().z()), new Vector3f(0.5f, 0.5f, 0.5f), 1, true, nearbyBlock.block());

                        Vec velocity = Vec.fromPoint(nearbyBlock.position().sub(blockPos.add(0.5))).normalize().mul(4, 8, 4).mul(rand.nextDouble(1.2, 2)).mul(TntStrengthCommand.TNT_STRENGTH);
                        cube.getRigidBody().activate();
                        Vector3f linearVelocity = new Vector3f();
                        cube.getRigidBody().getLinearVelocity(linearVelocity);
                        cube.getRigidBody().setLinearVelocity(linearVelocity.add(toVector3(velocity)));
                        cube.getRigidBody().setAngularVelocity(toVector3(velocity)); // probably completely wrong but it looks nice
                        batch.setBlock(nearbyBlock.position(), Block.AIR);

                        PHYSICS_BLOCK_MAP.get(nearbyBlock.position().sub(0.5)).destroy();
                        PHYSICS_BLOCK_MAP.remove(nearbyBlock.position());
                    }
                    batch.apply(instance, null);
                }).delay(TaskSchedule.tick(3 * ServerFlag.SERVER_TICKS_PER_SECOND)).schedule();
            }



            if (!e.isCancelled()) {
                MinecraftPhysicsObject object = new BlockRigidBody(physicsHandler, instance, toVector3(blockPos.add(0.5)), new Vector3f(0.5f, 0.5f, 0.5f), PhysicsBody.massForStatic, false, Block.BLUE_STAINED_GLASS);

                PHYSICS_BLOCK_MAP.put(blockPos, object);
            }
        });

        global.addListener(PlayerBlockBreakEvent.class, e -> {
            Point blockPos = e.getBlockPosition();

            if (PHYSICS_BLOCK_MAP.containsKey(blockPos)) {
                PHYSICS_BLOCK_MAP.get(blockPos).destroy();
                PHYSICS_BLOCK_MAP.remove(blockPos);
            }
        });

        CommandManager commandManager = MinecraftServer.getCommandManager();
        commandManager.register(new ChainLengthCommand());
        commandManager.register(new ClearCommand(physicsHandler));
        commandManager.register(new PerformanceCommand(MinecraftServer.getGlobalEventHandler(), physicsHandler));
        commandManager.register(new PlayerSizeCommand());
        commandManager.register(new TntStrengthCommand());

        server.start("0.0.0.0", 25563);
    }

    public static @Nullable Entity raycastEntity(@NotNull Instance instance, @NotNull Point startPoint, @NotNull Point direction,
                                                 double maxDistance, @NotNull Predicate<Entity> hitFilter) {
        for (Entity entity : instance.getEntities()) {
            if (!hitFilter.test(entity)) continue;
            if (entity.getPosition().distanceSquared(startPoint) > maxDistance * maxDistance) continue;

            Area3d area3d = Area3d.CONVERTER.from(entity.getBoundingBox());
            Pos entityPos = entity.getPosition();

            Vector3d intersection = area3d.lineIntersection(
                    startPoint.x() - entityPos.x(), startPoint.y() - entityPos.y(), startPoint.z() - entityPos.z(),
                    direction.x(), direction.y(), direction.z()
            );
            if (intersection != null) {
                return entity;
            }
        }

        return null;
    }

}