package dev.emortal.objects;

import dev.emortal.playermodel.SkinLook;
import net.minestom.server.item.component.CustomModelData;
import org.jetbrains.annotations.Nullable;

/**
 * A piece of a ragdoll, mapped onto the core player model pieces.
 * Centres are in player model pixels (16 per block at normal size), relative to the feet,
 * with +x being the player's right.
 */
public enum RagdollPart {
    HEAD("head", false, 0, 28),
    TORSO("body", false, 0, 18),
    RIGHT_ARM("right_arm", true, 6, 18),
    LEFT_ARM("left_arm", true, -6, 18),
    RIGHT_LEG("right_leg", false, 2, 6),
    LEFT_LEG("left_leg", false, -2, 6);

    private static final String PREFIX = "emortalmc:player/";

    private final String modelName;
    private final boolean hasSlimVariant;
    private final float centreX;
    private final float centreY;

    RagdollPart(String modelName, boolean hasSlimVariant, float centreX, float centreY) {
        this.modelName = modelName;
        this.hasSlimVariant = hasSlimVariant;
        this.centreX = centreX;
        this.centreY = centreY;
    }

    /**
     * @return the item model of this part for the look (slim or wide), or null if the look has no such piece
     */
    public @Nullable String itemModel(SkinLook look) {
        String wide = PREFIX + modelName;
        if (look.pieces().containsKey(wide)) return wide;
        if (hasSlimVariant) {
            String slim = wide + "_slim";
            if (look.pieces().containsKey(slim)) return slim;
        }
        return null;
    }

    public @Nullable CustomModelData modelData(SkinLook look) {
        String itemModel = itemModel(look);
        return itemModel == null ? null : look.pieces().get(itemModel);
    }

    public boolean isSlim(SkinLook look) {
        String itemModel = itemModel(look);
        return itemModel != null && itemModel.endsWith("_slim");
    }

    /**
     * @return the x of this part's centre in pixels; slim arms are half a pixel closer to the body
     */
    public float centreX(boolean slim) {
        if (slim && hasSlimVariant) return centreX - Math.signum(centreX) * 0.5f;
        return centreX;
    }

    public float centreY() {
        return centreY;
    }
}
