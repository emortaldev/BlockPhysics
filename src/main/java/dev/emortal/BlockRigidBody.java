package dev.emortal;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
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

public class BlockRigidBody {

    private final btCollisionShape boxShape;
    private final btCollisionObject boxObject;
    private final btRigidBody rigidBody;

    private final Vector3 size;
    private Entity entity = null;
    private ItemDisplayMeta meta = null;

    public BlockRigidBody(Instance instance, Vector3 translation, Vector3 size, float mass, boolean visible, Block block) {
        this.size = size;

        boxShape = new btBoxShape(size);
        boxObject = new btCollisionObject();
        boxObject.setCollisionShape(boxShape);
        Matrix4 transform = boxObject.getWorldTransform().setToTranslation(translation);
        boxObject.setWorldTransform(transform);

        Vector3 localInertia = new Vector3();
        boxShape.calculateLocalInertia(mass, localInertia);

        btRigidBody.btRigidBodyConstructionInfo info = new btRigidBody.btRigidBodyConstructionInfo(mass, null, boxShape, mass == 0 ? Vector3.Zero : localInertia);
        rigidBody = new btRigidBody(info);

        rigidBody.setWorldTransform(boxObject.getWorldTransform());

        if (mass > 0) {
            MotionState motionState = new MotionState(rigidBody.getWorldTransform());
            rigidBody.setMotionState(motionState);
        }

        if (visible) spawnEntity(instance, block);
    }

    public void setTransform(Point pos) {
        Matrix4 transform = boxObject.getWorldTransform().setToTranslation((float)pos.x(), (float)pos.y(), (float)pos.z());
        boxObject.setWorldTransform(transform);

        rigidBody.setWorldTransform(boxObject.getWorldTransform());
    }

    private void spawnEntity(Instance instance, Block block) {
        // Uses an ITEM_DISPLAY instead of a BLOCK_DISPLAY as it is centered around the middle instead of the corner
        entity = new NoTickingEntity(EntityType.ITEM_DISPLAY);
        meta = (ItemDisplayMeta) entity.getEntityMeta();

        meta.setWidth(2);
        meta.setHeight(2);

        meta.setNotifyAboutChanges(false);
        meta.setItemStack(ItemStack.of(block.registry().material()));
        Matrix4 transform = rigidBody.getWorldTransform();

        Vector3 translation = new Vector3();
        transform.getTranslation(translation);
        entity.setInstance(instance, toVec(translation));

        Vector3 scale = new Vector3();
        transform.getScale(scale);
        meta.setScale(toVec(scale).mul(size.x * 2, size.y * 2, size.z * 2));

        Quaternion rotation = new Quaternion();
        transform.getRotation(rotation);
        meta.setLeftRotation(toFloats(rotation));
        meta.setNotifyAboutChanges(true);
    }

    public void updateEntity() {
        if (meta == null) return;

//        rigidBody.activate(); // Rigid bodies have to be constantly active in order to be pushed by the player

        meta.setNotifyAboutChanges(false);
        meta.setTransformationInterpolationDuration(1);
        meta.setPosRotInterpolationDuration(1);
        meta.setTransformationInterpolationStartDelta(0);
        Matrix4 transform = rigidBody.getWorldTransform();

        Vector3 translation = new Vector3();
        transform.getTranslation(translation);
        entity.teleport(Pos.fromPoint(toVec(translation)));

        // size not updated as it doesn't change

        Quaternion rotation = new Quaternion();
        transform.getRotation(rotation);
        meta.setLeftRotation(toFloats(rotation));
        meta.setNotifyAboutChanges(true);
    }

    public @NotNull btCollisionShape getBoxShape() {
        return boxShape;
    }
    public @NotNull btCollisionObject getBoxObject() {
        return boxObject;
    }
    public @NotNull btRigidBody getRigidBody() {
        return rigidBody;
    }
    public @Nullable Entity getEntity() {
        return entity;
    }
    public @Nullable ItemDisplayMeta getMeta() {
        return meta;
    }

//    public void destroy() {
//        if (entity != null) entity.remove();
//
//        rigidBody.dispose();
//        boxObject.dispose();
//        boxShape.dispose();
//    }

    public static @NotNull Vec toVec(Vector3 vector3) {
        return new Vec(vector3.x, vector3.y, vector3.z);
    }
    public static @NotNull Vector3 toVector3(Point vec) {
        return new Vector3((float)vec.x(), (float)vec.y(), (float)vec.z());
    }
    public static float[] toFloats(Quaternion rotation) {
        return new float[] { rotation.x, rotation.y, rotation.z, rotation.w };
    }

}
