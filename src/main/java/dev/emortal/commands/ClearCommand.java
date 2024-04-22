package dev.emortal.commands;

import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.PlaneCollisionShape;
import com.jme3.bullet.joints.PhysicsJoint;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Plane;
import com.jme3.math.Vector3f;
import dev.emortal.MinecraftPhysicsHandler;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.timer.TaskSchedule;

import static dev.emortal.Main.PLAYER_OBJECT_MAP;
import static dev.emortal.objects.BlockRigidBody.toVector3;

public class ClearCommand extends Command {
    public ClearCommand(MinecraftPhysicsHandler physicsHandler) {
        super("clear");

        setDefaultExecutor((sender, ctx) -> {
            if (!(sender instanceof Player player)) return;

            for (PhysicsRigidBody rigidBody : physicsHandler.getPhysicsSpace().getRigidBodyList()) {
                physicsHandler.getPhysicsSpace().removeCollisionObject(rigidBody);
            }

            for (PhysicsJoint physicsJoint : physicsHandler.getPhysicsSpace().getJointList()) {
                physicsJoint.getPhysicsSpace().removeJoint(physicsJoint);
                physicsJoint.destroy();
            }

            for (Entity entity : player.getInstance().getEntities()) {
                if (entity instanceof Player) continue;
                entity.remove();
            }

            physicsHandler.entityObjectMap.clear();
            physicsHandler.objects.clear();

            // Re-add the floor
            CollisionShape planeShape = new PlaneCollisionShape(new Plane(Vector3f.UNIT_Y, 0f));
            PhysicsRigidBody floor = new PhysicsRigidBody(planeShape, PhysicsRigidBody.massForStatic);
            physicsHandler.getPhysicsSpace().addCollisionObject(floor);

            for (Player player1 : player.getInstance().getPlayers()) {
                CollisionShape boxShape = new BoxCollisionShape((float) (player1.getBoundingBox().width()/2f), (float) (player1.getBoundingBox().height()/2f), (float) (player1.getBoundingBox().depth()/2f));
                PhysicsRigidBody playerRigidBody = new PhysicsRigidBody(boxShape, PhysicsRigidBody.massForStatic);
                physicsHandler.getPhysicsSpace().addCollisionObject(playerRigidBody);

                PLAYER_OBJECT_MAP.put(player1.getUuid(), playerRigidBody);

                player1.scheduler().buildTask(() -> {
                    playerRigidBody.activate();
                    playerRigidBody.setPhysicsLocation(toVector3(player1.getPosition().add(0, 1, 0)));
                }).repeat(TaskSchedule.tick(1)).schedule();
            }

            System.gc();

//            sender.sendMessage("Cleared!");
        });
    }
}
