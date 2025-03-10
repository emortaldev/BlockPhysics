package dev.emortal.objects;

import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.joints.ConeJoint;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;
import dev.emortal.MinecraftPhysics;
import dev.emortal.NoTickingEntity;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.Nullable;

public class LanternPhysics extends MinecraftPhysicsObject {

    public LanternPhysics(MinecraftPhysics mcPhysics, @Nullable PhysicsRigidBody parent, Vec size, float mass, Vector3f jointPos) {
        super(mcPhysics, new PhysicsRigidBody(new BoxCollisionShape((float)size.x(), (float)size.y(), (float)size.z()), mass), size);

        setAlwaysActive(true); // Lanterns may randomly stop otherwise

        var rigidBody = (PhysicsRigidBody) getCollisionObject();
        rigidBody.setAngularDamping(0.1f);
        rigidBody.setLinearDamping(0.3f);
        rigidBody.setPhysicsLocation(jointPos);
        rigidBody.setPhysicsLocation(jointPos.subtract(0, 1f, 0f));

        if (parent == null) {
            CollisionShape parentBoxShape = new BoxCollisionShape(0.1f);
            PhysicsRigidBody parentlessRigidBody = new PhysicsRigidBody(parentBoxShape, PhysicsRigidBody.massForStatic);
            parentlessRigidBody.setPhysicsLocation(jointPos);
            mcPhysics.getPhysicsSpace().addCollisionObject(parentlessRigidBody);
            parent = parentlessRigidBody;
            addRelated(parent);
        }

        ConeJoint joint = new ConeJoint(parent, rigidBody, new Vector3f(0, -0.5f, 0), new Vector3f(0, 0.5f, 0));
        addRelated(joint);
        mcPhysics.getPhysicsSpace().addJoint(joint);
    }

    @Override
    public Entity createEntity() {
        // Uses an ITEM_DISPLAY instead of a BLOCK_DISPLAY as it is centered around the middle instead of the corner
        Entity entity = new NoTickingEntity(EntityType.ITEM_DISPLAY);

        entity.setBoundingBox(0.2, getSize().y(), 0.2);

        entity.editEntityMeta(ItemDisplayMeta.class, meta -> {
            meta.setWidth(2);
            meta.setHeight(2);
            meta.setTranslation(new Vec(0, 0.3, 0));
            meta.setItemStack(ItemStack.of(Material.LANTERN));
        });

        return entity;
    }

}
