package dev.emortal.objects;

import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import dev.emortal.MinecraftPhysicsHandler;
import dev.emortal.NoTickingEntity;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BlockRigidBody implements MinecraftPhysicsObject {


    private final Vector3f size;
    private @Nullable Entity entity;
    private ItemDisplayMeta meta = null;

    private final MinecraftPhysicsHandler physicsHandler;
    private final PhysicsRigidBody rigidBody;

    public BlockRigidBody(MinecraftPhysicsHandler physicsHandler, Instance instance, Vector3f position, Vector3f size, float mass, boolean visible, Block block) {
        this.size = size;
        this.physicsHandler = physicsHandler;

        physicsHandler.objects.add(this);

        BoxCollisionShape boxShape = new BoxCollisionShape(size.x, size.y, size.z);
        rigidBody = new PhysicsRigidBody(boxShape, mass);
        physicsHandler.getPhysicsSpace().addCollisionObject(rigidBody);
        rigidBody.setAngularDamping(0.1f);
        rigidBody.setLinearDamping(0.3f);
        rigidBody.setPhysicsLocation(position);

        if (visible) {
            entity = spawnEntity(instance, block);
            physicsHandler.entityObjectMap.put(entity, this);
        }
        else entity = null;
    }

    private Entity spawnEntity(Instance instance, Block block) {
        // Uses an ITEM_DISPLAY instead of a BLOCK_DISPLAY as it is centered around the middle instead of the corner
        Entity entity = new NoTickingEntity(EntityType.ITEM_DISPLAY);

        entity.setBoundingBox(size.x, size.y, size.z);

        Transform transform = new Transform();
        rigidBody.getTransform(transform);

        meta = (ItemDisplayMeta) entity.getEntityMeta();
        meta.setNotifyAboutChanges(false);
        meta.setWidth(2);
        meta.setHeight(2);
        meta.setItemStack(ItemStack.of(block.registry().material()));
        meta.setScale(toVec(transform.getScale()).mul(size.x * 2, size.y * 2, size.z * 2));
        meta.setLeftRotation(toFloats(transform.getRotation()));
        meta.setNotifyAboutChanges(true);

        entity.setInstance(instance, toVec(transform.getTranslation()));

        return entity;
    }

    @Override
    public void updateEntity() {
        if (meta == null) return;
        if (entity == null) return;

//        rigidBody.activate(); // Rigid bodies have to be constantly active in order to be pushed by the player

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
    public @Nullable Entity getEntity() {
        return entity;
    }

    @Override
    public void destroy() {
        if (entity != null) entity.remove();
        entity = null;

        physicsHandler.objects.remove(this);
        physicsHandler.getPhysicsSpace().removeCollisionObject(rigidBody);
    }

    @Override
    public @Nullable ItemDisplayMeta getMeta() {
        return meta;
    }

    public static @NotNull Vec toVec(Vector3f vector3) {
        return new Vec(vector3.x, vector3.y, vector3.z);
    }
    public static @NotNull Pos toPos(Vector3f vector3) {
        return new Pos(vector3.x, vector3.y, vector3.z);
    }
    public static @NotNull Vector3f toVector3(Point vec) {
        return new Vector3f((float)vec.x(), (float)vec.y(), (float)vec.z());
    }
    public static float[] toFloats(Quaternion rotation) {
        return new float[] { rotation.getX(), rotation.getY(), rotation.getZ(), rotation.getW() };
    }

}
