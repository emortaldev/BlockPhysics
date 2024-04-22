package dev.emortal.objects;

import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.joints.ConeJoint;
import com.jme3.bullet.joints.PhysicsJoint;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import dev.emortal.MinecraftPhysicsHandler;
import dev.emortal.NoTickingEntity;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static dev.emortal.objects.BlockRigidBody.*;

public class LanternPhysics implements MinecraftPhysicsObject {

    private final Vector3f size;
    private Entity entity;
    private ItemDisplayMeta meta = null;


    private final MinecraftPhysicsHandler physicsHandler;
    private final PhysicsRigidBody rigidBody;
    private final PhysicsJoint joint;

    public LanternPhysics(MinecraftPhysicsHandler physicsHandler, @Nullable PhysicsRigidBody parent, Instance instance, Vector3f size, float mass, Vector3f jointPos) {
        this.size = size;
        this.physicsHandler = physicsHandler;

        physicsHandler.objects.add(this);

        BoxCollisionShape boxShape = new BoxCollisionShape(size.x, size.y, size.z);
        rigidBody = new PhysicsRigidBody(boxShape, mass);
        physicsHandler.getPhysicsSpace().addCollisionObject(rigidBody);
        rigidBody.setAngularDamping(0.1f);
        rigidBody.setLinearDamping(0.3f);
        rigidBody.setPhysicsLocation(jointPos);
        rigidBody.setPhysicsLocation(jointPos.subtract(0, 1f, 0f));

        if (parent == null) {
            CollisionShape parentBoxShape = new BoxCollisionShape(0.1f);
            PhysicsRigidBody parentlessRigidBody = new PhysicsRigidBody(parentBoxShape, PhysicsRigidBody.massForStatic);
            parentlessRigidBody.setPhysicsLocation(jointPos);
            physicsHandler.getPhysicsSpace().addCollisionObject(parentlessRigidBody);
            parent = parentlessRigidBody;
        }

        joint = new ConeJoint(parent, rigidBody, new Vector3f(0, -0.5f, 0), new Vector3f(0, 0.5f, 0));
        physicsHandler.getPhysicsSpace().addJoint(joint);

        entity = spawnEntity(instance);
        physicsHandler.entityObjectMap.put(entity, this);
    }

    private Entity spawnEntity(Instance instance) {
        // Uses an ITEM_DISPLAY instead of a BLOCK_DISPLAY as it is centered around the middle instead of the corner
        Entity entity = new NoTickingEntity(EntityType.ITEM_DISPLAY);

        entity.setBoundingBox(0.2, size.y, 0.2);

        Transform transform = new Transform();
        rigidBody.getTransform(transform);

        meta = (ItemDisplayMeta) entity.getEntityMeta();
        meta.setNotifyAboutChanges(false);
        meta.setWidth(2);
        meta.setHeight(2);
        meta.setTranslation(new Vec(0, 0.3, 0));
        meta.setItemStack(ItemStack.of(Material.LANTERN));
        meta.setLeftRotation(toFloats(transform.getRotation()));
        meta.setNotifyAboutChanges(true);

        entity.setInstance(instance, toVec(transform.getTranslation()));

        return entity;
    }

    @Override
    public void updateEntity() {
        if (meta == null) return;
        if (entity == null) return;

        rigidBody.activate(); // Rigid bodies have to be constantly active in order to be pushed by the player
        // Lanterns stop weirdly when this is off

        Transform transform = new Transform();
        rigidBody.getTransform(transform);

        meta.setNotifyAboutChanges(false);
        meta.setTransformationInterpolationDuration(1);
        meta.setPosRotInterpolationDuration(1);
        meta.setTransformationInterpolationStartDelta(0);

        entity.teleport(toPos(transform.getTranslation()));

        // size not updated as it doesn't change
        meta.setLeftRotation(toFloats(transform.getRotation()));
        meta.setNotifyAboutChanges(true);
    }

    @Override
    public @NotNull PhysicsRigidBody getRigidBody() {
        return rigidBody;
    }
    @Override
    public @Nullable Entity getEntity() {
        return entity;
    }
    @Override
    public @Nullable ItemDisplayMeta getMeta() {
        return meta;
    }

    @Override
    public void destroy() {
        if (entity != null) entity.remove();
        entity = null;

        physicsHandler.objects.remove(this);
        physicsHandler.getPhysicsSpace().removeCollisionObject(rigidBody);
        physicsHandler.getPhysicsSpace().removeJoint(joint);
    }

}
