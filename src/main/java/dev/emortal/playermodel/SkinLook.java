package dev.emortal.playermodel;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kr.toxicity.library.dynamicuv.UVModel;
import kr.toxicity.library.dynamicuv.UVModelData;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.util.RGBLike;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.item.component.CustomModelData;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * A skin, and optionally a cape, read into the data of every part of a player character.
 */
public final class SkinLook {

    final Map<String, CustomModelData> pieces;

    private SkinLook(Map<String, CustomModelData> pieces) {
        this.pieces = pieces;
    }

    /**
     * The model data of every piece, keyed by item model (e.g. {@code emortalmc:player/head}).
     */
    public Map<String, CustomModelData> pieces() {
        return Collections.unmodifiableMap(pieces);
    }

    public static SkinLook of(BufferedImage skin) {
        return of(skin, null, false);
    }

    public static SkinLook of(BufferedImage skin, boolean slim) {
        return of(skin, null, slim);
    }

    public static SkinLook of(BufferedImage skin, @Nullable BufferedImage cape) {
        return of(skin, cape, false);
    }

    public static SkinLook of(BufferedImage skin, @Nullable BufferedImage cape, boolean slim) {
        BufferedImage modern = toModernSkin(skin);
        Map<String, CustomModelData> pieces = new LinkedHashMap<>();
        for (UVModel model : PlayerModel.skin(slim)) {
            pieces.put(model.itemModelNamespace(), toCustomModelData(model.write(modern)));
        }
        if (cape != null) {
            UVModel model = PlayerModel.cape();
            pieces.put(model.itemModelNamespace(), toCustomModelData(model.write(toCape(cape))));
        }
        return new SkinLook(pieces);
    }

    public static CompletableFuture<SkinLook> fromSkin(PlayerSkin skin) {
        return CompletableFuture.supplyAsync(() -> {
            String json = new String(Base64.getDecoder().decode(skin.textures()), StandardCharsets.UTF_8);
            JsonObject textures = JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("textures");
            JsonObject body = textures.getAsJsonObject("SKIN");

            JsonObject metadata = body.getAsJsonObject("metadata");
            JsonElement model = metadata == null ? null : metadata.get("model");
            boolean slim = model != null && "slim".equals(model.getAsString());

            BufferedImage skinImage = read(body.get("url").getAsString());

            JsonObject capeObject = textures.getAsJsonObject("CAPE");
            JsonElement capeUrl = capeObject == null ? null : capeObject.get("url");
            BufferedImage cape = capeUrl == null ? null : read(capeUrl.getAsString());

            return of(skinImage, cape, slim);
        });
    }

    public static CompletableFuture<@Nullable SkinLook> ofUsername(String name) {
        return CompletableFuture.supplyAsync(() -> PlayerSkin.fromUsername(name))
            .thenCompose(found -> found != null
                ? fromSkin(found)
                : CompletableFuture.<SkinLook>completedFuture(null));
    }

    private static BufferedImage read(String url) {
        try {
            BufferedImage image = ImageIO.read(URI.create(url).toURL());
            if (image == null) throw new IllegalArgumentException("not an image: " + url);
            return image;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static CustomModelData toCustomModelData(UVModelData data) {
        return new CustomModelData(
            List.copyOf(data.floats()),
            List.copyOf(data.flags()),
            List.of(),
            data.colors().intStream().mapToObj(c -> (RGBLike) TextColor.color(c)).toList()
        );
    }

    private static BufferedImage toModernSkin(BufferedImage skin) {
        if (skin.getHeight() == 64) return skin;
        if (skin.getWidth() != 64 || skin.getHeight() != 32) {
            throw new IllegalArgumentException(
                "a skin is 64×64 or 64×32, not " + skin.getWidth() + "×" + skin.getHeight());
        }
        BufferedImage modern = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 64; x++) modern.setRGB(x, y, skin.getRGB(x, y));
        }
        copyMirrored(skin, modern, 0, 16, 16, 48);
        copyMirrored(skin, modern, 40, 16, 32, 48);
        return modern;
    }

    private static void copyMirrored(BufferedImage from, BufferedImage to, int sx, int sy, int dx, int dy) {
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) to.setRGB(dx + x, dy + y, from.getRGB(sx + 15 - x, sy + y));
        }
    }

    private static BufferedImage toCape(BufferedImage cape) {
        if (cape.getWidth() == 64 && cape.getHeight() == 32) return cape;
        BufferedImage scaled = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.drawImage(cape, 0, 0, 64, 32, null);
        g.dispose();
        return scaled;
    }
}