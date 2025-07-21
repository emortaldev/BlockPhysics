package dev.emortal.objects;

import com.github.stephengold.joltjni.BodyCreationSettings;
import com.github.stephengold.joltjni.BoxShape;
import com.github.stephengold.joltjni.Vec3;
import com.github.stephengold.joltjni.enumerate.EMotionType;
import com.github.stephengold.joltjni.readonly.RVec3Arg;
import dev.emortal.MinecraftPhysics;
import dev.emortal.NoTickingEntity;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import static dev.emortal.utils.CoordinateUtils.toVec;

public class BlockRigidBody extends MinecraftPhysicsObject {

    private final Block block;
    private final boolean visible;

    public BlockRigidBody(@NotNull MinecraftPhysics mcPhysics, RVec3Arg position, Vec size, boolean visible, Block block) {
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

        this.block = block;
        this.visible = visible;
    }

    @Override
    public Entity createEntity() {
        if (!visible) return null;

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
            meta.setItemStack(ItemStack.of(block.registry().material()));
            meta.setScale(toVec(halfExtents).mul(2));
        });

        return entity;
    }

}
