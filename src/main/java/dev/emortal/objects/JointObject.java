package dev.emortal.objects;

import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.EmptyShape;
import com.jme3.bullet.joints.ConeJoint;
import com.jme3.bullet.joints.PhysicsJoint;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;
import dev.emortal.MinecraftPhysicsHandler;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JointObject implements MinecraftPhysicsObject {


    private final PhysicsJoint joint;

    private final MinecraftPhysicsHandler physicsHandler;
    private final PhysicsRigidBody rigidBody;

    public JointObject(MinecraftPhysicsHandler physicsHandler, Vector3f position, PhysicsRigidBody holding) {
        this.physicsHandler = physicsHandler;

        physicsHandler.objects.add(this);

        CollisionShape boxShape = new EmptyShape(false);
        rigidBody = new PhysicsRigidBody(boxShape, 0);
        physicsHandler.getPhysicsSpace().addCollisionObject(rigidBody);
        rigidBody.setPhysicsLocation(position);

        joint = new ConeJoint(rigidBody, holding, Vector3f.ZERO, Vector3f.ZERO);
        physicsHandler.getPhysicsSpace().addJoint(joint);
    }
    @Override
    public @NotNull PhysicsRigidBody getRigidBody() {
        return rigidBody;
    }

    @Override
    public void updateEntity() {}

    public @Nullable Entity getEntity() {
        return null;
    }

    @Override
    public void destroy() {
        physicsHandler.objects.remove(this);
        physicsHandler.getPhysicsSpace().removeCollisionObject(rigidBody);
        physicsHandler.getPhysicsSpace().removeJoint(joint);
    }

    @Override
    public @Nullable ItemDisplayMeta getMeta() {
        return null;
    }

}
