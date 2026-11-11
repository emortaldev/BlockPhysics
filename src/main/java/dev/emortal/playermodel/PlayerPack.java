package dev.emortal.playermodel;

import kr.toxicity.library.dynamicuv.UVByteBuilder;
import kr.toxicity.library.dynamicuv.UVModel;
import kr.toxicity.library.dynamicuv.UVTextureName;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PlayerPack {

    private PlayerPack() {}

    public static void writePlayerPack(File out) {
        Map<String, UVModel> distinct = new LinkedHashMap<>();
        for (UVModel model : PlayerModel.skin(false)) distinct.putIfAbsent(model.modelName(), model);
        for (UVModel model : PlayerModel.skin(true)) distinct.putIfAbsent(model.modelName(), model);

        List<UVModel> models = new ArrayList<>(distinct.values());
        models.add(PlayerModel.cape());

        for (UVModel model : models) {
            for (UVByteBuilder builder : model.asJson()) write(out, builder);
        }
        write(out, UVTextureName.DEFAULT.normalPixel(PlayerModel.NAMESPACE));
        write(out, UVTextureName.DEFAULT.translucentPixel(PlayerModel.NAMESPACE));
    }

    private static void write(File out, UVByteBuilder builder) {
        File file = new File(out, builder.path());
        try {
            Files.createDirectories(file.getParentFile().toPath());
            Files.write(file.toPath(), builder.build());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}