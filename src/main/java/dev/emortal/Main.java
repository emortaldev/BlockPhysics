package dev.emortal;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.metadata.other.PrimedTntMeta;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.event.player.PlayerStartSneakingEvent;
import net.minestom.server.event.server.ServerTickMonitorEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.batch.AbsoluteBlockBatch;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.ExplosionPacket;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.utils.NamespaceID;
import net.minestom.server.world.DimensionType;
import net.minestom.server.world.DimensionTypeManager;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static dev.emortal.BlockRigidBody.toPxVec;

public class Main {

    private static final Set<Point> BLOCKS_IN_SPHERE = SphereUtil.getBlocksInSphere(5);

    public static void main(String[] args) {
        System.setProperty("minestom.tps", "60");
        MinecraftServer server = MinecraftServer.init();


        DimensionTypeManager dm = MinecraftServer.getDimensionTypeManager();
        DimensionType dimension = DimensionType.builder(NamespaceID.from("fb")).ambientLight(1f).build();
        dm.addDimension(dimension);

        InstanceManager im = MinecraftServer.getInstanceManager();
        Instance instance = im.createInstanceContainer(dimension);

        for (int x = -200; x < 200; x++) {
            for (int z = -200; z < 200; z++) {
                instance.setBlock(x, -1, z, Block.GRASS_BLOCK);
            }
        }
        instance.setTimeUpdate(null);
        instance.setTimeRate(0);

        MinecraftPhysicsHandler physicsHandler = new MinecraftPhysicsHandler(instance);

        BossBar bossBar = BossBar.bossBar(Component.empty(), 1f, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS);

        GlobalEventHandler global = MinecraftServer.getGlobalEventHandler();

        global.addListener(PlayerLoginEvent.class, e -> {
            e.getPlayer().showBossBar(bossBar);
            e.setSpawningInstance(instance);
            e.getPlayer().setGameMode(GameMode.CREATIVE);
            e.getPlayer().setRespawnPoint(new Pos(0, 5, 0));

            e.getPlayer().sendMessage(Component.text("Welcome, crouch to spawn blocks"));
            e.getPlayer().sendMessage(Component.text("You can interact with the cubes by moving into them"));

            e.getPlayer().getInventory().setItemStack(3, ItemStack.of(Material.TNT));
            e.getPlayer().getInventory().setItemStack(2, ItemStack.of(Material.STONE));
        });


        DecimalFormat dec = new DecimalFormat("0.00");
        global.addListener(ServerTickMonitorEvent.class, e -> {
            physicsHandler.update(Math.max(1f / MinecraftServer.TICK_PER_SECOND, MinecraftServer.TICK_MS / 1000f));

            double tickTime = Math.floor(e.getTickMonitor().getTickTime() * 100.0) / 100.0;
            bossBar.name(
                    Component.text()
                            .append(Component.text("MSPT: " + dec.format(tickTime)))
                            .append(Component.text(" | "))
                            .append(Component.text("Cubes: " + physicsHandler.getCubes().size()))
            );
            bossBar.progress(Math.min((float)tickTime / (float)MinecraftServer.TICK_MS, 1f));

            if (tickTime > MinecraftServer.TICK_MS) {
                bossBar.color(BossBar.Color.RED);
            } else {
                bossBar.color(BossBar.Color.GREEN);
            }
        });

        global.addListener(PlayerBlockPlaceEvent.class, e -> {
            Point tntPos = e.getBlockPosition();

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

                    for (BlockRigidBody cube : physicsHandler.getCubes()) {
                        if (cube.getMeta() == null) continue;

                        if (cube.getMeta().getTranslation().distanceSquared(tntPos.add(0.5)) > 5*5) continue;
                        Vec velocity = Vec.fromPoint(cube.getMeta().getTranslation().sub(tntPos.add(0.5))).normalize().mul(4, 8, 4).mul(rand.nextDouble(1, 2.5));
                        cube.getBox().setLinearVelocity(toPxVec(velocity));
                        cube.getBox().setAngularVelocity(toPxVec(velocity)); // probably completely wrong but it looks nice
                    }

                    List<WorldBlock> nearbyBlocks = SphereUtil.getNearbyBlocks(tntPos.add(0.5), BLOCKS_IN_SPHERE, instance, block -> !block.block().isAir() && !block.block().compare(Block.GRASS_BLOCK));
                    AbsoluteBlockBatch batch = new AbsoluteBlockBatch();
                    for (WorldBlock nearbyBlock : nearbyBlocks) {
                        var cube = physicsHandler.spawnCube(nearbyBlock.position(), new Vec(1, 1, 1), 5, true, nearbyBlock.block());

                        Vec velocity = Vec.fromPoint(nearbyBlock.position().sub(tntPos.add(0.5))).normalize().mul(4, 8, 4).mul(rand.nextDouble(1, 2.5));
                        cube.getBox().setLinearVelocity(toPxVec(velocity));
                        cube.getBox().setAngularVelocity(toPxVec(velocity)); // probably completely wrong but it looks nice
                        batch.setBlock(nearbyBlock.position(), Block.AIR);
                    }
                    batch.apply(instance, null);
                }).delay(TaskSchedule.tick(3 * MinecraftServer.TICK_PER_SECOND)).schedule();
            }
        });

        global.addListener(PlayerStartSneakingEvent.class, e -> {
            for (int x = -5; x < 5; x++) {
                for (int y = 0; y < 4; y++) {
                    for (int z = -5; z < 5; z++) {
                        physicsHandler.spawnCube(new Vec(x, 30 + y, z), new Vec(1, 1, 1), 1, true, Block.DIAMOND_BLOCK);
                    }
                }
            }
        });

        server.start("0.0.0.0", 25565);
    }

}