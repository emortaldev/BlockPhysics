package dev.emortal.objects;

import com.github.stephengold.joltjni.*;
import com.github.stephengold.joltjni.enumerate.EAxis;
import com.github.stephengold.joltjni.enumerate.EMotionQuality;
import com.github.stephengold.joltjni.enumerate.EMotionType;
import com.github.stephengold.joltjni.readonly.QuatArg;
import com.github.stephengold.joltjni.readonly.RVec3Arg;
import dev.emortal.MinecraftPhysics;
import dev.emortal.NoTickingEntity;
import dev.emortal.playermodel.SkinLook;
import net.minestom.server.MinecraftServer;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.CustomModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.concurrent.CompletableFuture;

import static dev.emortal.commands.PlayerSizeCommand.PLAYER_SIZE;
import static dev.emortal.utils.CoordinateUtils.toPos;
import static dev.emortal.utils.CoordinateUtils.toRVec3;

public class RagdollPhysics extends MinecraftPhysicsObject {

    /** The player model's pieces are built for this scale to be normal player size */
    private static final double FIGURE_SCALE = 2.0;

    private final @NotNull CompletableFuture<@Nullable SkinLook> look;
    private final @NotNull RagdollPart part;
    private volatile boolean slim = false;

    public RagdollPhysics(MinecraftPhysics mcPhysics, @NotNull CompletableFuture<@Nullable SkinLook> look, @Nullable Body torso, @NotNull RagdollPart part, RVec3Arg position, QuatArg rotation, Vec size) {
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
                        .setMotionQuality(EMotionQuality.LinearCast) // Use CCD (Continuous Collision Detection), helps not fall through the mesh
        );
        this.part = part;
        this.look = look;

        Vec torsoSize = new Vec(8.0/16.0, 12.0/16.0, 4.0/16.0);
        Vec headSize = new Vec(8.0/16.0, 8.0/16.0, 8.0/16.0);
        Vec limbSize = new Vec(4.0/16.0, 12.0/16.0, 4.0/16.0);

        if (torso != null) {
            assert (part != RagdollPart.TORSO);

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

            SixDofConstraintSettings jointSettings = new SixDofConstraintSettings();
            jointSettings.makeFixedAxis(EAxis.TranslationX);
            jointSettings.makeFixedAxis(EAxis.TranslationY);
            jointSettings.makeFixedAxis(EAxis.TranslationZ);
            jointSettings.setPosition1(firstThing2);
            jointSettings.setPosition2(secondThing2);

            TwoBodyConstraint constraint = jointSettings.create(torso, getBody());
            mcPhysics.addConstraint(constraint);
        }
    }

    @Override
    public Entity createEntity() {
        Entity entity = new NoTickingEntity(EntityType.ITEM_DISPLAY);

        BoxShape shape = (BoxShape) getBody().getShape();
        Vec3 halfExtents = shape.getHalfExtent();
        entity.setBoundingBox(halfExtents.getX(), halfExtents.getY(), halfExtents.getZ());

        entity.editEntityMeta(ItemDisplayMeta.class, meta -> {
            meta.setWidth(2);
            meta.setHeight(2);
            meta.setTransformationInterpolationDuration(1);
            meta.setPosRotInterpolationDuration(1);
            meta.setScale(new Vec(FIGURE_SCALE * PLAYER_SIZE));
        });
        applyTransform(entity);

        // The skin is fetched asynchronously, so the item is set once it arrives
        look.whenComplete((result, error) -> {
            if (error != null) error.printStackTrace();
            if (result == null) return;
            CustomModelData data = part.modelData(result);
            String itemModel = part.itemModel(result);
            if (data == null || itemModel == null) return;

            // NoTickingEntity doesn't tick, so its own scheduler never runs - use the global one
            MinecraftServer.getSchedulerManager().scheduleNextTick(() -> {
                if (entity.isRemoved()) return;
                this.slim = part.isSlim(result);
                entity.editEntityMeta(ItemDisplayMeta.class, meta -> meta.setItemStack(
                        ItemStack.builder(Material.PAPER)
                                .set(DataComponents.ITEM_MODEL, itemModel)
                                .set(DataComponents.CUSTOM_MODEL_DATA, data)
                                .build()
                ));
                applyTransform(entity);
            });
        });

        return entity;
    }

    @Override
    public void update() {
        Entity entity = getEntity();
        if (entity == null) return;
        if (!entity.isActive()) return;

        applyTransform(entity);
        entity.teleport(toPos(getBody().getPosition()));
    }

    /**
     * The player model pieces are positioned relative to the feet, so each display is rotated with its body
     * and translated so the piece's centre sits on the body's centre.
     */
    private void applyTransform(Entity entity) {
        Quat rotation = getBody().getRotation();
        Quaternionf bodyRotation = new Quaternionf(rotation.getX(), rotation.getY(), rotation.getZ(), rotation.getW());

        // Item displays render models turned 180 degrees around Y, undo that so the model's axes match the body's
        Quaternionf displayRotation = new Quaternionf(bodyRotation).rotateY((float) Math.PI);

        float pixel = (float) (PLAYER_SIZE / 16.0);
        Vector3f centre = new Vector3f(part.centreX(slim) * pixel, part.centreY() * pixel, 0f);
        bodyRotation.transform(centre).negate();

        entity.editEntityMeta(ItemDisplayMeta.class, meta -> {
            meta.setTransformationInterpolationStartDelta(0);
            meta.setLeftRotation(new float[] { displayRotation.x, displayRotation.y, displayRotation.z, displayRotation.w });
            meta.setTranslation(new Vec(centre.x, centre.y, centre.z));
        });
    }

}
