package dev.emortal.objects;

/*
import dev.emortal.MinecraftPhysics;
import dev.emortal.NoTickingEntity;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.Nullable;

public class ChainPhysics extends MinecraftPhysicsObject {

    public ChainPhysics(MinecraftPhysics mcPhysics, @Nullable PhysicsRigidBody parent, Vec size, float mass, Vector3f jointPos) {
        super(mcPhysics, new PhysicsRigidBody(new BoxCollisionShape((float)size.x(), (float)size.y(), (float)size.z()), mass), size);

        var rigidBody = (PhysicsRigidBody) getCollisionObject();
        rigidBody.setAngularDamping(0.1f);
        rigidBody.setLinearDamping(0.3f);
        rigidBody.setPhysicsLocation(jointPos);

        if (parent == null) return;

        New6Dof joint = new New6Dof(parent, rigidBody, new Vector3f(0, -0.52f, 0), new Vector3f(0, 0.52f, 0), Matrix3f.IDENTITY, Matrix3f.IDENTITY, RotationOrder.XYZ);
        joint.setBreakingImpulseThreshold(70);
        joint.setCollisionBetweenLinkedBodies(false);
        mcPhysics.getPhysicsSystem().addJoint(joint);
    }

    @Override
    public @Nullable Entity createEntity() {
        // Uses an ITEM_DISPLAY instead of a BLOCK_DISPLAY as it is centered around the middle instead of the corner
        Entity entity = new NoTickingEntity(EntityType.ITEM_DISPLAY);

//        entity.setBoundingBox(size.x, size.y, size.z);
        entity.setBoundingBox(0.2, getSize().y(), 0.2);

        entity.editEntityMeta(ItemDisplayMeta.class, meta -> {
            meta.setWidth(2);
            meta.setHeight(2);
            meta.setItemStack(ItemStack.of(Material.CHAIN));
//            meta.setItemStack(ItemStack.of(Material.DIAMOND_BLOCK));
//        meta.setScale(toVec(transform.getScale()).mul(size.x * 2, size.y * 2, size.z * 2));
            meta.setScale(new Vec(1.15));
//            meta.setScale(getSize().mul(2));
        });

        return entity;
    }

}
*/