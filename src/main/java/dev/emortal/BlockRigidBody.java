package dev.emortal;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import physx.common.PxIDENTITYEnum;
import physx.common.PxQuat;
import physx.common.PxTransform;
import physx.common.PxVec3;
import physx.geometry.PxBoxGeometry;
import physx.physics.*;

public class BlockRigidBody {

    private final Point size;
    private Entity entity = null;
    private ItemDisplayMeta meta = null;

    private final PxRigidDynamic box;

    public BlockRigidBody(Instance instance, PxPhysics physics, PxScene scene, int mass, Point pos, Point size, boolean visible, Block block) {
        this.size = size;

        PxMaterial material = physics.createMaterial(0.5f, 0.5f, 0.5f);
        // create default simulation shape flags
        PxShapeFlags shapeFlags = new PxShapeFlags((byte) (PxShapeFlagEnum.eSCENE_QUERY_SHAPE.value | PxShapeFlagEnum.eSIMULATION_SHAPE.value));
        PxFilterData tmpFilterData = new PxFilterData(1, 1, 0, 0);


        // create a small dynamic box with size 1x1x1, which will fall on the ground
        PxVec3 tmpVec = toPxVec(pos);
        PxTransform tmpPose = new PxTransform(PxIDENTITYEnum.PxIdentity);
        tmpPose.setP(tmpVec);
        PxBoxGeometry boxGeometry = new PxBoxGeometry((float)size.x() / 2f, (float)size.y() / 2f, (float)size.z() / 2f); // PxBoxGeometry uses half-sizes
        PxShape boxShape = physics.createShape(boxGeometry, material, true, shapeFlags);
        box = physics.createRigidDynamic(tmpPose);
        boxShape.setSimulationFilterData(tmpFilterData);
        box.attachShape(boxShape);
        scene.addActor(box);

        box.setMass(mass);

        if (visible) spawnEntity(instance, block);
    }

    private void spawnEntity(Instance instance, Block block) {
        // Uses an ITEM_DISPLAY instead of a BLOCK_DISPLAY as it is centered around the middle instead of the corner
        entity = new NoTickingEntity(EntityType.ITEM_DISPLAY);
        meta = (ItemDisplayMeta) entity.getEntityMeta();
        entity.setInstance(instance, new Pos(0, -0.5, 0));

        meta.setNotifyAboutChanges(false);
        meta.setItemStack(ItemStack.of(block.registry().material()));
        meta.setInterpolationDuration(1);
        meta.setInterpolationStartDelta(0);

        meta.setTranslation(toVec(box.getGlobalPose().getP()));

        meta.setScale(Vec.fromPoint(size));

        meta.setLeftRotation(toFloats(box.getGlobalPose().getQ()));
        meta.setNotifyAboutChanges(true);
    }

    public void updateEntity() {
        if (meta == null) return;

        meta.setNotifyAboutChanges(false);
        meta.setInterpolationDuration(1);
        meta.setInterpolationStartDelta(0);

        meta.setTranslation(toVec(box.getGlobalPose().getP()));

        // size not updated as it doesn't change

        meta.setLeftRotation(toFloats(box.getGlobalPose().getQ()));
        meta.setNotifyAboutChanges(true);
    }

    public @Nullable Entity getEntity() {
        return entity;
    }
    public @Nullable ItemDisplayMeta getMeta() {
        return meta;
    }

    public PxRigidDynamic getBox() {
        return box;
    }

    public static Vec toVec(PxVec3 pxVec) {
        return new Vec(pxVec.getX(), pxVec.getY(), pxVec.getZ());
    }
    public static PxVec3 toPxVec(Point vec) {
        try (MemoryStack mem = MemoryStack.stackPush()) {
            // create an object of PxVec3. The native object is allocated in memory
            // provided by MemoryStack
            return PxVec3.createAt(mem, MemoryStack::nmalloc, (float) vec.x(), (float) vec.y(), (float) vec.z());
        }
//        return new PxVec3((float) vec.x(), (float) vec.y(), (float) vec.z());
    }
    public static float[] toFloats(PxQuat pxQuat) {
        return new float[] { pxQuat.getX(), pxQuat.getY(), pxQuat.getZ(), pxQuat.getW() };
    }

}
