package dev.emortal.objects;

import com.github.stephengold.joltjni.*;
import com.github.stephengold.joltjni.enumerate.EMotionType;
import com.github.stephengold.joltjni.readonly.RVec3Arg;
import dev.emortal.MinecraftPhysics;
import dev.emortal.NoTickingEntity;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.Nullable;

import static dev.emortal.utils.CoordinateUtils.toRVec3;
import static dev.emortal.utils.CoordinateUtils.toVec;

public class ChainPhysics extends MinecraftPhysicsObject {

    public ChainPhysics(MinecraftPhysics mcPhysics, @Nullable Body parent, Vec size, RVec3Arg position) {
        super(
                mcPhysics,
                new BodyCreationSettings()
                        .setMotionType(EMotionType.Dynamic)
                        .setObjectLayer(MinecraftPhysics.objLayerMoving)
                        .setShape(new BoxShape((float)size.x(), (float)size.y(), (float)size.z()))
                        .setAngularDamping(0.1f)
                        .setLinearDamping(0.3f)
                        .setPosition(position)
        );

        if (parent == null) return;

        PointConstraintSettings jointSettings = new PointConstraintSettings();
        jointSettings.setPoint1(toRVec3(toVec(parent.getPosition()).add(0, -0.52f, 0)));
        jointSettings.setPoint2(toRVec3(toVec(position).add(0, 0.52f, 0)));

        // TODO: add 6dof rotation limits
        int numSubGroups = 1;
        GroupFilterTable filter = new GroupFilterTable(numSubGroups);
        parent.setCollisionGroup(new CollisionGroup(filter, 0, 0));
        getBody().setCollisionGroup(new CollisionGroup(filter, 0, 0));

        TwoBodyConstraint constraint = jointSettings.create(parent, getBody());
        mcPhysics.addConstraint(constraint);
    }

    @Override
    public @Nullable Entity createEntity() {
        // Uses an ITEM_DISPLAY instead of a BLOCK_DISPLAY as it is centered around the middle instead of the corner
        Entity entity = new NoTickingEntity(EntityType.ITEM_DISPLAY);

        BoxShape shape = (BoxShape) getBody().getShape();
        Vec3 halfExtents = shape.getHalfExtent();
        entity.setBoundingBox(0.2, halfExtents.getY(), 0.2);

        entity.editEntityMeta(ItemDisplayMeta.class, meta -> {
            meta.setWidth(2);
            meta.setHeight(2);
            meta.setTransformationInterpolationDuration(1);
            meta.setPosRotInterpolationDuration(1);
            meta.setItemStack(ItemStack.of(Material.CHAIN));
            meta.setScale(new Vec(1.15));
        });

        return entity;
    }

}