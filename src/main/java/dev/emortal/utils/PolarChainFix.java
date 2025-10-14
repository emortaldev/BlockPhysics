package dev.emortal.utils;

import net.hollowcube.polar.PolarDataConverter;
import org.jetbrains.annotations.NotNull;

public class PolarChainFix implements PolarDataConverter {

    @Override
    public void convertBlockPalette(@NotNull String[] palette, int fromVersion, int toVersion) {
        for (int i = 0; i < palette.length; i++) {
            String s = palette[i];

            if (s.contains("chain")) {
                palette[i] = s.replace("chain", "iron_chain");
            }
        }
    }

    @Override
    public int dataVersion() {
        return 1000000000;
    }
}
