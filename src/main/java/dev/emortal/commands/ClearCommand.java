package dev.emortal.commands;

import dev.emortal.MinecraftPhysics;
import dev.emortal.worldmesh.ChunkMesher;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClearCommand extends Command {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClearCommand.class);

    public ClearCommand(MinecraftPhysics physics) {
        super("clear");

        setDefaultExecutor((sender, ctx) -> {
            if (!(sender instanceof Player player)) return;

            physics.getPhysicsSystem().removeAllConstraints();
            physics.getPhysicsSystem().destroyAllBodies();

            for (Entity entity : player.getInstance().getEntities()) {
                if (entity instanceof Player) continue;
                entity.remove();
            }

            physics.clear();

            // Regenerate chunk meshes
            Instance instance = physics.getInstance();
            instance.getChunks().forEach(c -> {
                instance.scheduleNextTick(a -> {
                    long before = System.nanoTime();
                    ChunkMesher.createChunk(physics, c);
                    long after = System.nanoTime();

                    LOGGER.info("Took " + (after - before) + "ns to generate chunk mesh");
                });
            });

            // Re-add the floor
//            physics.addFloorPlane();

//            for (Player player1 : player.getInstance().getPlayers()) {
//                CollisionShape boxShape = new BoxCollisionShape((float) (player1.getBoundingBox().width()/2f), (float) (player1.getBoundingBox().height()/2f), (float) (player1.getBoundingBox().depth()/2f));
//                PhysicsRigidBody playerRigidBody = new PhysicsRigidBody(boxShape, PhysicsRigidBody.massForStatic);
//                physicsHandler.getPhysicsSystem().addCollisionObject(playerRigidBody);
//
//                player1.setTag(MinecraftPhysics.PLAYER_RIGID_BODY_TAG, playerRigidBody);
//
//                player1.scheduler().buildTask(() -> {
//                    playerRigidBody.activate();
//                    playerRigidBody.setPhysicsLocation(toVec3(player1.getPosition().add(0, 1, 0)));
//                }).repeat(TaskSchedule.tick(1)).schedule();
//            }

            System.gc();

            sender.sendMessage("Cleared!");
        });
    }
}
