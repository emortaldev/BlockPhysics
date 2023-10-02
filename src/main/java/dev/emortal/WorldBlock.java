package dev.emortal;

import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

public record WorldBlock(@NotNull Point position, @NotNull Block block) {
}