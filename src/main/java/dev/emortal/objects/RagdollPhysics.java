package dev.emortal.objects;

import com.github.stephengold.joltjni.*;
import com.github.stephengold.joltjni.enumerate.EAxis;
import com.github.stephengold.joltjni.enumerate.EMotionType;
import com.github.stephengold.joltjni.readonly.QuatArg;
import com.github.stephengold.joltjni.readonly.RVec3Arg;
import dev.emortal.MinecraftPhysics;
import dev.emortal.NoTickingEntity;
import dev.emortal.PlayerDisplayPart;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.HeadProfile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static dev.emortal.commands.PlayerSizeCommand.PLAYER_SIZE;
import static dev.emortal.utils.CoordinateUtils.toFloats;
import static dev.emortal.utils.CoordinateUtils.toRVec3;

public class RagdollPhysics extends MinecraftPhysicsObject {

    private final @Nullable PlayerSkin playerSkin;

    private final @NotNull PlayerDisplayPart part;

    public RagdollPhysics(MinecraftPhysics mcPhysics, @NotNull Player spawner, @Nullable Body torso, @NotNull PlayerDisplayPart part, RVec3Arg position, QuatArg rotation, Vec size) {
        super(
                mcPhysics,
                new BodyCreationSettings()
                        .setMotionType(EMotionType.Dynamic)
                        .setObjectLayer(MinecraftPhysics.objLayerMoving)
                        .setShape(new BoxShape((float)size.x(), (float)size.y(), (float)size.z()))
                        .setAngularDamping(0.1f)
                        .setLinearDamping(0.3f)
                        .setPosition(position)
                        .setRotation(rotation)
        );
        this.part = part;

        this.playerSkin = spawner.getSkin();

        Vec torsoSize = new Vec(8.0/16.0, 12.0/16.0, 4.0/16.0);
        Vec headSize = new Vec(8.0/16.0, 8.0/16.0, 8.0/16.0);
        Vec limbSize = new Vec(4.0/16.0, 12.0/16.0, 4.0/16.0);

        if (torso != null) {
            assert (part != PlayerDisplayPart.TORSO);

            // From torso
            Vec firstThing = switch (part) {
                case HEAD -> new Vec(0f, torsoSize.y() / 2f, 0f);
                case RIGHT_ARM -> new Vec(torsoSize.x() / 1.35f, torsoSize.y() / 2f, 0f);
                case LEFT_ARM -> new Vec(-torsoSize.x() / 1.35f, torsoSize.y() / 2f, 0f);
                case RIGHT_LEG -> new Vec(0.13f, -torsoSize.y() / 2f, 0f);
                case LEFT_LEG -> new Vec(-0.13f, -torsoSize.y() / 2f, 0f);
                default -> throw new IllegalStateException("Unexpected value: " + part);
            };
            // From part
            Vec secondThing = switch (part) {
                case HEAD -> new Vec(0.0f, -headSize.y() / 2f, 0f);
                case RIGHT_ARM -> new Vec(0, limbSize.y() / 2f, 0f);
                case LEFT_ARM -> new Vec(0, limbSize.y() / 2f, 0f);
                case RIGHT_LEG -> new Vec(0f, limbSize.y() / 2f, 0f);
                case LEFT_LEG -> new Vec(0f, limbSize.y() / 2f, 0f);
                default -> throw new IllegalStateException("Unexpected value: " + part);
            };

            RVec3 torsoPos = torso.getPosition();
            RVec3 firstThing2 = toRVec3(firstThing);
            firstThing2.rotateInPlace(torso.getRotation());
            firstThing2.scaleInPlace(PLAYER_SIZE);
            firstThing2.addInPlace(torsoPos.x(), torsoPos.y(), torsoPos.z());
            RVec3 secondThing2 = toRVec3(secondThing);
            secondThing2.rotateInPlace(rotation);
            secondThing2.scaleInPlace(PLAYER_SIZE);
            secondThing2.addInPlace(position.x(), position.y(), position.z());

//            firstThing.scaleInPlace(PLAYER_SIZE);
//            secondThing.scaleInPlace(PLAYER_SIZE);

            SixDofConstraintSettings jointSettings = new SixDofConstraintSettings();
            jointSettings.makeFixedAxis(EAxis.TranslationX);
            jointSettings.makeFixedAxis(EAxis.TranslationY);
            jointSettings.makeFixedAxis(EAxis.TranslationZ);
            jointSettings.setPosition1(firstThing2);
            jointSettings.setPosition2(secondThing2);
//            joint.setBreakingImpulseThreshold(60); TODO: this

            TwoBodyConstraint constraint = jointSettings.create(torso, getBody());
            mcPhysics.addConstraint(constraint);
        }
    }

    @Override
    public Entity createEntity() {
        // Uses an ITEM_DISPLAY instead of a BLOCK_DISPLAY as it is centered around the middle instead of the corner
        // although causes issues with certain items, it works for most
        Entity entity = new NoTickingEntity(EntityType.ITEM_DISPLAY);

        BoxShape shape = (BoxShape) getBody().getShape();
        Vec3 halfExtents = shape.getHalfExtent();
        entity.setBoundingBox(halfExtents.getX(), halfExtents.getY(), halfExtents.getZ());

        entity.editEntityMeta(ItemDisplayMeta.class, meta -> {
            meta.setWidth(2);
            meta.setHeight(2);
            meta.setTransformationInterpolationDuration(1);
            meta.setPosRotInterpolationDuration(1);
            meta.setDisplayContext(ItemDisplayMeta.DisplayContext.THIRDPERSON_RIGHT_HAND);
            meta.setItemStack(
                    ItemStack.builder(Material.PLAYER_HEAD)
                            .itemModel(this.part.getCustomModelData())
                            .set(DataComponents.PROFILE, new HeadProfile(this.playerSkin))
                            .customModelData(List.of(), List.of(), List.of("physics"), List.of())
                            .build()
            );
            meta.setScale(new Vec(PLAYER_SIZE));
            meta.setTranslation(new Vec(0, this.part.getYTranslation(), 0));
            meta.setLeftRotation(toFloats(getBody().getRotation()));

//            meta.setItemStack(ItemStack.of(Material.DIAMOND_BLOCK));
//            meta.setScale(getSize().mul(2));
        });

        return entity;
    }

}
