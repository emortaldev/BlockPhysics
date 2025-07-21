package dev.emortal.worldmesh;

import com.github.stephengold.joltjni.Vec3;
import net.minestom.server.instance.block.BlockFace;

public record Face(BlockFace blockFace, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int blockX, int blockY, int blockZ) {

    public Face(BlockFace blockFace, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int blockX, int blockY, int blockZ) {
        this(blockFace, (float) minX, (float) minY, (float) minZ, (float) maxX, (float) maxY, (float) maxZ, blockX, blockY, blockZ);
    }

    public boolean isEdge() {
        return switch (blockFace) {
            case BOTTOM -> minY == 0.0;
            case TOP -> maxY == 1.0;
            case NORTH -> minZ == 0.0;
            case SOUTH -> maxZ == 1.0;
            case WEST -> minX == 0.0;
            case EAST -> maxX == 1.0;
        };
    }

    public Quad toQuad() {
        return switch (blockFace) {
            case TOP -> new Quad(
                    new Vec3(minX + blockX, maxY + blockY, minZ + blockZ),
                    new Vec3(maxX + blockX, maxY + blockY, minZ + blockZ),
                    new Vec3(maxX + blockX, maxY + blockY, maxZ + blockZ),
                    new Vec3(minX + blockX, maxY + blockY, maxZ + blockZ)
            );
            case BOTTOM -> new Quad(
                    new Vec3(maxX + blockX, maxY + blockY, maxZ + blockZ),
                    new Vec3(maxX + blockX, maxY + blockY, minZ + blockZ),
                    new Vec3(minX + blockX, maxY + blockY, minZ + blockZ),
                    new Vec3(minX + blockX, maxY + blockY, maxZ + blockZ)
            );
            case WEST -> new Quad(
                    new Vec3(maxX + blockX, minY + blockY, minZ + blockZ),
                    new Vec3(maxX + blockX, maxY + blockY, minZ + blockZ),
                    new Vec3(maxX + blockX, maxY + blockY, maxZ + blockZ),
                    new Vec3(maxX + blockX, minY + blockY, maxZ + blockZ)
            );
            case EAST -> new Quad(
                    new Vec3(maxX + blockX, maxY + blockY, maxZ + blockZ),
                    new Vec3(maxX + blockX, maxY + blockY, minZ + blockZ),
                    new Vec3(maxX + blockX, minY + blockY, minZ + blockZ),
                    new Vec3(maxX + blockX, minY + blockY, maxZ + blockZ)
            );
            case SOUTH -> new Quad(
                    new Vec3(maxX + blockX, maxY + blockY, minZ + blockZ),
                    new Vec3(maxX + blockX, minY + blockY, minZ + blockZ),
                    new Vec3(minX + blockX, minY + blockY, minZ + blockZ),
                    new Vec3(minX + blockX, maxY + blockY, minZ + blockZ)
            );
            case NORTH -> new Quad(
                    new Vec3(minX + blockX, minY + blockY, minZ + blockZ),
                    new Vec3(maxX + blockX, minY + blockY, minZ + blockZ),
                    new Vec3(maxX + blockX, maxY + blockY, minZ + blockZ),
                    new Vec3(minX + blockX, maxY + blockY, minZ + blockZ)
            );
        };
    }
}