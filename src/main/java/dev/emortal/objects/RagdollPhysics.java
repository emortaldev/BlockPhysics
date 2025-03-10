package dev.emortal.objects;

import com.jme3.bullet.RotationOrder;
import com.jme3.bullet.joints.New6Dof;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import dev.emortal.MinecraftPhysics;
import dev.emortal.NoTickingEntity;
import dev.emortal.PlayerDisplayPart;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.HeadProfile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static dev.emortal.commands.PlayerSizeCommand.PLAYER_SIZE;
import static dev.emortal.utils.CoordinateUtils.toVec;
import static dev.emortal.utils.CoordinateUtils.toVector3;

public class RagdollPhysics extends BlockRigidBody {

    private final @Nullable PlayerSkin playerSkin;

    private final @NotNull PlayerDisplayPart part;

    public RagdollPhysics(MinecraftPhysics mcPhysics, @NotNull Player spawner, @Nullable PhysicsRigidBody torso, @NotNull PlayerDisplayPart part, Vector3f position, Vec size, float mass) {
        super(mcPhysics, position, size, mass, true, Block.AIR);
        this.part = part;

        this.playerSkin = spawner.getSkin();

        Vec torsoSize = new Vec(8.0/16.0, 12.0/16.0, 4.0/16.0);
        Vec headSize = new Vec(8.0/16.0, 8.0/16.0, 8.0/16.0);
        Vec limbSize = new Vec(4.0/16.0, 12.0/16.0, 4.0/16.0);

        var rigidBody = (PhysicsRigidBody) getCollisionObject();
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

            New6Dof joint = new New6Dof(torso, rigidBody, toVector3(toVec(firstThing).mul(PLAYER_SIZE)), toVector3(toVec(secondThing).mul(PLAYER_SIZE)), Matrix3f.IDENTITY, Matrix3f.IDENTITY, RotationOrder.XYZ);
            joint.setBreakingImpulseThreshold(60);
            mcPhysics.getPhysicsSpace().add(joint);
            addRelated(joint);
        }
    }

    @Override
    public Entity createEntity() {
        // Uses an ITEM_DISPLAY instead of a BLOCK_DISPLAY as it is centered around the middle instead of the corner
        // although causes issues with certain items, it works for most
        Entity entity = new NoTickingEntity(EntityType.ITEM_DISPLAY);

        entity.setBoundingBox(getSize().x(), getSize().y(), getSize().z());

        entity.editEntityMeta(ItemDisplayMeta.class, meta -> {
            meta.setWidth(2);
            meta.setHeight(2);
            meta.setDisplayContext(ItemDisplayMeta.DisplayContext.THIRD_PERSON_RIGHT_HAND);
            meta.setItemStack(
                    ItemStack.builder(Material.PLAYER_HEAD)
                            .itemModel(this.part.getCustomModelData())
                            .set(ItemComponent.PROFILE, new HeadProfile(this.playerSkin))
                            .customModelData(List.of(), List.of(), List.of("default"), List.of())
                            .build()
            );
            meta.setScale(new Vec(PLAYER_SIZE));
            meta.setTranslation(new Vec(0, this.part.getYTranslation(), 0));

//            meta.setItemStack(ItemStack.of(Material.DIAMOND_BLOCK));
//            meta.setScale(getSize().mul(2));
        });

        return entity;
    }

}
