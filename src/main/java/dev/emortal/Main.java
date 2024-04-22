package dev.emortal;

import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.joints.ConeJoint;
import com.jme3.bullet.joints.PhysicsJoint;
import com.jme3.bullet.objects.PhysicsBody;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;
import com.jme3.system.NativeLibraryLoader;
import dev.emortal.rayfast.area.area3d.Area3d;
import dev.emortal.rayfast.area.area3d.Area3dRectangularPrism;
import dev.emortal.rayfast.vector.Vector3d;
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
import net.minestom.server.event.player.*;
import net.minestom.server.event.server.ServerTickMonitorEvent;
import net.minestom.server.extras.MojangAuth;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.batch.AbsoluteBlockBatch;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.ExplosionPacket;
import net.minestom.server.timer.Task;
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

import static dev.emortal.BlockRigidBody.toVec;
import static dev.emortal.BlockRigidBody.toVector3;

public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private static final Set<Point> BLOCKS_IN_SPHERE = SphereUtil.getBlocksInSphere(5);
    private static final Map<UUID, PhysicsRigidBody> PLAYER_OBJECT_MAP = new HashMap<>();

    private static final Map<Point, MinecraftPhysicsObject> PHYSICS_BLOCK_MAP = new HashMap<>();

    private static final Map<BoundingBox, Area3d> boundingBoxToArea3dMap = new HashMap<>();

    public static void main(String[] args) {
        System.setProperty("minestom.tps", "60");
        NativeLibraryLoader.loadLibbulletjme(true, new File("natives/"), "Release", "SpMt");

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

            e.getPlayer().sendMessage(Component.text("Welcome, crouch to spawn a player"));
//            e.getPlayer().sendMessage(Component.text("You can interact with the cubes by moving into them"));

            e.getPlayer().getInventory().setItemStack(6, ItemStack.of(Material.DIAMOND_SWORD));
            e.getPlayer().getInventory().setItemStack(5, ItemStack.of(Material.BLAZE_ROD));
            e.getPlayer().getInventory().setItemStack(4, ItemStack.of(Material.CHAIN));
            e.getPlayer().getInventory().setItemStack(3, ItemStack.of(Material.TNT));
            e.getPlayer().getInventory().setItemStack(2, ItemStack.of(Material.STONE));

            CollisionShape boxShape = new BoxCollisionShape(0.5f, 1f, 0.5f);
            PhysicsRigidBody playerRigidBody = new PhysicsRigidBody(boxShape, PhysicsRigidBody.massForStatic);

//            cube.getEntity().setBoundingBox(e.getPlayer().getBoundingBox());
            PLAYER_OBJECT_MAP.put(e.getPlayer().getUuid(), playerRigidBody);

            e.getPlayer().scheduler().buildTask(() -> {
                playerRigidBody.setPhysicsLocation(toVector3(e.getPlayer().getPosition().add(0, 1, 0)));
            }).repeat(TaskSchedule.tick(1)).schedule();
        });

        global.addListener(AsyncPlayerConfigurationEvent.class, e -> {
            e.setSpawningInstance(instance);
            e.getPlayer().setRespawnPoint(new Pos(0, 20, 0));
        });

        instance.scheduler().buildTask(() -> {
            physicsHandler.update(1f / (float) ServerFlag.SERVER_TICKS_PER_SECOND);
        }).repeat(TaskSchedule.tick(1)).schedule();


        DecimalFormat dec = new DecimalFormat("0.00");
        global.addListener(ServerTickMonitorEvent.class, e -> {
            double tickTime = Math.floor(e.getTickMonitor().getTickTime() * 100.0) / 100.0;
            bossBar.name(
                    Component.text()
                            .append(Component.text("MSPT: " + dec.format(tickTime)))
                            .append(Component.text(" | "))
                            .append(Component.text("Cubes: " + physicsHandler.objects.size()))
            );
            bossBar.progress(Math.min((float)tickTime / (float)MinecraftServer.TICK_MS, 1f));

            if (tickTime > MinecraftServer.TICK_MS) {
                bossBar.color(BossBar.Color.RED);
            } else {
                bossBar.color(BossBar.Color.GREEN);
            }
        });

        global.addListener(ItemDropEvent.class, e -> {
            e.setCancelled(true);
        });

        Map<UUID, Task> holdingBodyTaskMap = new HashMap<>();
        Map<UUID, MinecraftPhysicsObject> firstWeld = new HashMap<>();
        global.addListener(PlayerUseItemEvent.class, e -> {
            if (e.getItemStack().material() == Material.DIAMOND_SWORD) {
                Entity entity = raycastEntity(instance, e.getPlayer().getPosition().add(0, e.getPlayer().getEyeHeight(), 0), e.getPlayer().getPosition().direction(), 100000, (ent) -> {
                    if (ent == e.getPlayer()) return false;
                    return true;
                });

                if (entity == null) return;

                MinecraftPhysicsObject obj = physicsHandler.getFromEntity(entity);

                if (obj == null) return;

                obj.getRigidBody().activate();

                if (firstWeld.containsKey(e.getPlayer().getUuid())) {
                    PhysicsJoint joint = new ConeJoint(firstWeld.get(e.getPlayer().getUuid()).getRigidBody(), obj.getRigidBody(), Vector3f.ZERO, Vector3f.ZERO);
                    physicsHandler.getPhysicsSpace().addJoint(joint);

                    firstWeld.remove(e.getPlayer().getUuid());
                    e.getPlayer().sendMessage("Welded!");
                    return;
                }

                firstWeld.put(e.getPlayer().getUuid(), obj);
                e.getPlayer().sendMessage("Selected first object");
            }

            if (e.getItemStack().material() == Material.BLAZE_ROD) {
                if (holdingBodyTaskMap.containsKey(e.getPlayer().getUuid())) {
                    holdingBodyTaskMap.get(e.getPlayer().getUuid()).cancel();
                    holdingBodyTaskMap.remove(e.getPlayer().getUuid());
                    return;
                }

                Entity entity = raycastEntity(instance, e.getPlayer().getPosition().add(0, e.getPlayer().getEyeHeight(), 0), e.getPlayer().getPosition().direction(), 100000, (ent) -> {
                    if (ent == e.getPlayer()) return false;
                    return true;
                });

                if (entity == null) return;

                e.getPlayer().sendMessage("Hit entity");

                MinecraftPhysicsObject obj = physicsHandler.getFromEntity(entity);

                if (obj == null) return;

                e.getPlayer().sendMessage("Found object");

                obj.getRigidBody().activate();

                double distance = e.getPlayer().getPosition().distance(entity.getPosition());

                var task = e.getPlayer().scheduler().buildTask(() -> {
                    PhysicsRigidBody rigidBody = obj.getRigidBody();

                    Vector3f physicsVec = new Vector3f();
                    rigidBody.getPhysicsLocation(physicsVec);

                    Vec wantedPos = Vec.fromPoint(e.getPlayer().getPosition().add(0, e.getPlayer().getEyeHeight(), 0).add(e.getPlayer().getPosition().direction().mul(distance)));
                    Vec diff = Vec.fromPoint(wantedPos.sub(toVec(physicsVec)));
                    rigidBody.setLinearVelocity(toVector3(diff.mul(7)));
                }).repeat(TaskSchedule.tick(1)).schedule();

                holdingBodyTaskMap.put(e.getPlayer().getUuid(), task);
            }
        });


        global.addListener(PlayerBlockPlaceEvent.class, e -> {
            Point blockPos = e.getBlockPosition();

            /*
            if (e.getBlock().compare(Block.LANTERN)) {
                e.setCancelled(true);

                physicsHandler.addCube(new LanternPhysics(physicsHandler, instance, new Vector3(e.getBlockPosition().blockX(), e.getBlockPosition().blockY(), e.getBlockPosition().blockZ()), new Vector3(0.1f, 0.4f, 0.1f), 1, Block.LANTERN, new Vector3(e.getBlockPosition().blockX() + 0.5f, e.getBlockPosition().blockY() + 0.5f, e.getBlockPosition().blockZ() + 0.5f)));
            }*/
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

                        if (cube.getEntity().getPosition().distanceSquared(blockPos.add(0.5)) > 5*5) continue;
                        Vec velocity = Vec.fromPoint(cube.getEntity().getPosition().sub(blockPos.add(0.5))).normalize().mul(4, 8, 4).mul(rand.nextDouble(1, 2.5));
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

                        Vec velocity = Vec.fromPoint(nearbyBlock.position().sub(blockPos.add(0.5))).normalize().mul(4, 8, 4).mul(rand.nextDouble(1, 2.5));
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


        global.addListener(PlayerStartSneakingEvent.class, e -> {
            var startPos = e.getPlayer().getPosition().add(e.getPlayer().getPosition().direction().mul(5));
            // all halves \/
            Vector3f torsoSize = new Vector3f(4.0f/16.0f, 6.0f/16.0f, 2.0f/16.0f);
//            Vector3 headSize = new Vector3(4.0f/16.0f, 4.0f/16.0f, 4.0f/16.0f);
            Vector3f headSize = new Vector3f(1f/16.0f, 4.0f/16.0f, 1f/16.0f);
//            Vector3 limbSize = new Vector3(2.0f/16.0f, 6.0f/16.0f, 2.0f/16.0f);
            Vector3f limbSize = new Vector3f(0.5f/16.0f, 6.0f/16.0f, 0.5f/16.0f);

            MinecraftPhysicsObject torso = new RagdollPhysics(physicsHandler, e.getPlayer(),null, PlayerDisplayPart.TORSO, instance, toVector3(startPos.add(0, 1.4, 0)), torsoSize, 1);
            MinecraftPhysicsObject head = new RagdollPhysics(physicsHandler, e.getPlayer(), torso.getRigidBody(), PlayerDisplayPart.HEAD, instance, toVector3(startPos.add(0, 1.4, 0)), headSize, 1);
            MinecraftPhysicsObject rightArm = new RagdollPhysics(physicsHandler, e.getPlayer(), torso.getRigidBody(), PlayerDisplayPart.RIGHT_ARM, instance, toVector3(startPos.add(0, 1.4, 0)), limbSize, 1);
            MinecraftPhysicsObject leftArm = new RagdollPhysics(physicsHandler, e.getPlayer(), torso.getRigidBody(), PlayerDisplayPart.LEFT_ARM, instance, toVector3(startPos.add(0, 1.4, 0)), limbSize, 1);
            MinecraftPhysicsObject rightLeg = new RagdollPhysics(physicsHandler, e.getPlayer(), torso.getRigidBody(), PlayerDisplayPart.RIGHT_LEG, instance, toVector3(startPos.add(0, 1.4, 0)), limbSize, 1);
            MinecraftPhysicsObject leftLeg = new RagdollPhysics(physicsHandler, e.getPlayer(), torso.getRigidBody(), PlayerDisplayPart.LEFT_LEG, instance, toVector3(startPos.add(0, 1.4, 0)), limbSize, 1);
        });

        CommandManager commandManager = MinecraftServer.getCommandManager();
        commandManager.register(new ChainLengthCommand());
        commandManager.register(new ClearCommand(physicsHandler));

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