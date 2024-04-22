package dev.emortal.objects;

import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.joints.ConeJoint;
import com.jme3.bullet.joints.PhysicsJoint;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import dev.emortal.MinecraftPhysicsHandler;
import dev.emortal.NoTickingEntity;
import dev.emortal.PlayerDisplayPart;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.metadata.PlayerHeadMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static dev.emortal.commands.PlayerSizeCommand.PLAYER_SIZE;
import static dev.emortal.objects.BlockRigidBody.*;

public class RagdollPhysics implements MinecraftPhysicsObject {

    private final Vector3f size;
    private Entity entity;
    private ItemDisplayMeta meta = null;


    private final @NotNull MinecraftPhysicsHandler physicsHandler;
    private final @NotNull PhysicsRigidBody rigidBody;
    private final @Nullable PhysicsJoint joint;

    private final @NotNull UUID playerUUID;
    private final @NotNull PlayerSkin playerSkin;

    private final @NotNull PlayerDisplayPart part;

    public RagdollPhysics(MinecraftPhysicsHandler physicsHandler, Player spawner, @Nullable PhysicsRigidBody torso, PlayerDisplayPart part, Instance instance, Vector3f position, Vector3f size, float mass) {
        this.size = size;
        this.physicsHandler = physicsHandler;
        this.part = part;

        this.playerUUID = spawner.getUuid();
        this.playerSkin = spawner.getSkin();

        Vec torsoSize = new Vec(8.0/16.0, 12.0/16.0, 4.0/16.0);
        Vec headSize = new Vec(8.0/16.0, 8.0/16.0, 8.0/16.0);
        Vec limbSize = new Vec(4.0/16.0, 12.0/16.0, 4.0/16.0);

        physicsHandler.objects.add(this);

        BoxCollisionShape boxShape = new BoxCollisionShape(size.x, size.y, size.z);
        rigidBody = new PhysicsRigidBody(boxShape, mass);
        physicsHandler.getPhysicsSpace().addCollisionObject(rigidBody);
        rigidBody.setAngularDamping(0.1f);
        rigidBody.setLinearDamping(0.3f);
        rigidBody.setPhysicsLocation(position);

        if (torso != null) {
            assert (part != PlayerDisplayPart.TORSO);

            // From torso
            Vector3f firstThing = switch (part) {
                case HEAD -> new Vector3f(0f, (float)torsoSize.y() / 2f, 0f);
                case RIGHT_ARM -> new Vector3f((float)torsoSize.x() / 1.35f, (float)torsoSize.y() / 2f, 0f);
                case LEFT_ARM -> new Vector3f((float)-torsoSize.x() / 1.35f, (float)torsoSize.y() / 2f, 0f);
                case RIGHT_LEG -> new Vector3f(0.13f, (float)-torsoSize.y() / 2f, 0f);
                case LEFT_LEG -> new Vector3f(-0.13f, (float)-torsoSize.y() / 2f, 0f);
                default -> throw new IllegalStateException("Unexpected value: " + part);
            };
            // From part
            Vector3f secondThing = switch (part) {
                case HEAD -> new Vector3f(0.0f, (float)-headSize.y() / 2f, 0f);
                case RIGHT_ARM -> new Vector3f(0, (float)limbSize.y() / 2f, 0f);
                case LEFT_ARM -> new Vector3f(0, (float)limbSize.y() / 2f, 0f);
                case RIGHT_LEG -> new Vector3f(0f, (float)limbSize.y() / 2f, 0f);
                case LEFT_LEG -> new Vector3f(0f, (float)limbSize.y() / 2f, 0f);
                default -> throw new IllegalStateException("Unexpected value: " + part);
            };

            joint = new ConeJoint(torso, rigidBody, toVector3(toVec(firstThing).mul(PLAYER_SIZE)), toVector3(toVec(secondThing).mul(PLAYER_SIZE)));
            physicsHandler.getPhysicsSpace().addJoint(joint);
        } else {
            joint = null;
        }


        entity = spawnEntity(instance);
        physicsHandler.entityObjectMap.put(entity, this);
    }

    private Entity spawnEntity(Instance instance) {
        // Uses an ITEM_DISPLAY instead of a BLOCK_DISPLAY as it is centered around the middle instead of the corner
        Entity entity = new NoTickingEntity(EntityType.ITEM_DISPLAY);

//        entity.setBoundingBox(size.x, size.y, size.z);
        entity.setBoundingBox(size.y, size.y, size.y);

        Transform transform = new Transform();
        rigidBody.getTransform(transform);

        meta = (ItemDisplayMeta) entity.getEntityMeta();
        meta.setNotifyAboutChanges(false);
        meta.setWidth(2);
        meta.setHeight(2);
//        meta.setItemStack(ItemStack.of(block.registry().material()));
        meta.setDisplayContext(ItemDisplayMeta.DisplayContext.THIRD_PERSON_RIGHT_HAND);
        meta.setItemStack(ItemStack.of(Material.PLAYER_HEAD).withMeta(PlayerHeadMeta.class, meta -> {
            meta.skullOwner(this.playerUUID);
            meta.playerSkin(this.playerSkin);
            meta.customModelData(this.part.getCustomModelData());
        }));
        meta.setScale(new Vec(PLAYER_SIZE));
        meta.setTranslation(new Vec(0, this.part.getYTranslation(), 0));

//        meta.setScale(toVec(transform.getScale()).mul(size.x * 2, size.y * 2, size.z * 2));
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

        physicsHandler.getPhysicsSpace().removeCollisionObject(rigidBody);
        if (joint != null) physicsHandler.getPhysicsSpace().removeJoint(joint);
    }

}
