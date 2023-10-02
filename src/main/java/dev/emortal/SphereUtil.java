package dev.emortal;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Predicate;

public final class SphereUtil {

    public static @NotNull List<WorldBlock> getNearbyBlocks(@NotNull Point pos, Set<Point> blocksInSphere, @NotNull Instance instance,
                                                            @NotNull Predicate<WorldBlock> predicate) {
        List<WorldBlock> filteredBlocks = new ArrayList<>();
        for (Point block : blocksInSphere) {
            Point blockPos = block.add(pos);
            Block currentBlock;
            try {
                currentBlock = instance.getBlock(blockPos, Block.Getter.Condition.TYPE);
            } catch (Exception ignored) {
                continue;
            }
            if (!predicate.test(new WorldBlock(blockPos, currentBlock))) continue;

            filteredBlocks.add(new WorldBlock(blockPos, currentBlock));
        }

        Collections.shuffle(filteredBlocks);
        return filteredBlocks;
    }

    public static @NotNull Set<Point> getBlocksInSphere(double radius) {
        Set<Point> points = new HashSet<>();

        for (double x = -radius; x <= radius; x++) {
            for (double y = -radius; y <= radius; y++) {
                for (double z = -radius; z <= radius; z++) {
                    if ((x * x) + (y * y) + (z * z) > radius * radius) continue;
                    points.add(new Vec(x, y, z));
                }
            }
        }

        return points;
    }

    private SphereUtil() {
    }
}