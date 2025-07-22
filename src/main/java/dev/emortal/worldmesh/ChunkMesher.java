package dev.emortal.worldmesh;

import com.github.stephengold.joltjni.BodyCreationSettings;
import com.github.stephengold.joltjni.MeshShapeSettings;
import com.github.stephengold.joltjni.Triangle;
import com.github.stephengold.joltjni.enumerate.EActivation;
import com.github.stephengold.joltjni.enumerate.EMotionType;
import dev.emortal.MinecraftPhysics;
import net.minestom.server.MinecraftServer;
import net.minestom.server.collision.Shape;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Section;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ChunkMesher {

    private static final BlockFace[] BLOCK_FACES = BlockFace.values();

    public static void createChunk(MinecraftPhysics physics, Chunk chunk) {
        int minY = MinecraftServer.getDimensionTypeRegistry().get(chunk.getInstance().getDimensionType()).minY();
        int maxY = MinecraftServer.getDimensionTypeRegistry().get(chunk.getInstance().getDimensionType()).maxY();

        generateChunkCollisionObject(physics, chunk, minY, maxY);
    }

    private static void generateChunkCollisionObject(MinecraftPhysics physics, Chunk chunk, int minY, int maxY) {
        List<Face> faces = getChunkFaces(chunk, minY, maxY);

        if (faces.isEmpty()) return;

        List<Triangle> triangles = new ArrayList<>();
        for (Face face : faces) {
            face.addTris(triangles);
        }

        MeshShapeSettings shapeSettings = new MeshShapeSettings(triangles);

        BodyCreationSettings bodySettings = new BodyCreationSettings()
                .setMotionType(EMotionType.Static)
                .setObjectLayer(MinecraftPhysics.objLayerNonMoving)
                .setShape(shapeSettings.create().get());

        physics.getBodyInterface().createAndAddBody(bodySettings, EActivation.DontActivate);
    }

    private static List<Face> getChunkFaces(Chunk chunk, int minY, int maxY) {
        int bottomY = maxY;
        int topY = minY;

        // Get min and max of current chunk sections to avoid computing on air
        List<Section> sections = chunk.getSections();
        for (int i = 0; i < sections.size(); i++) {
            Section section = sections.get(i);
            if (isEmpty(section)) continue;
            int chunkBottom = minY + i * Chunk.CHUNK_SECTION_SIZE;
            int chunkTop = chunkBottom + Chunk.CHUNK_SECTION_SIZE;

            if (bottomY > chunkBottom) {
                bottomY = chunkBottom;
            }
            if (topY < chunkTop) {
                topY = chunkTop;
            }
        }


        List<Face> finalFaces = new ArrayList<>();

        for (int y = bottomY; y < topY; y++) {
            for (int x = 0; x < Chunk.CHUNK_SIZE_X; x++) {
                for (int z = 0; z < Chunk.CHUNK_SIZE_Z; z++) {
                    List<Face> faces = getFaces(chunk, x, y, z);
                    if (faces == null) continue;
                    finalFaces.addAll(faces);
                }
            }
        }

        return finalFaces;
    }

    private static @Nullable List<Face> getFaces(Chunk chunk, int x, int y, int z){
        Block block = chunk.getBlock(x, y, z, Block.Getter.Condition.TYPE);

        if (block.isAir() || block.isLiquid()) return null;
        List<Face> faces = new ArrayList<>();

        Shape shape = block.registry().collisionShape();
        Point relStart = shape.relativeStart();
        Point relEnd = shape.relativeEnd();

        var blockX = chunk.getChunkX() * Chunk.CHUNK_SIZE_X + x;
        var blockZ = chunk.getChunkZ() * Chunk.CHUNK_SIZE_Z + z;

        for (BlockFace blockFace : BLOCK_FACES) {
            Face face = new Face(
                    blockFace,
                    blockFace == BlockFace.EAST ? relEnd.x() : relStart.x(),
                    blockFace == BlockFace.TOP ? relEnd.y() : relStart.y(),
                    blockFace == BlockFace.SOUTH ? relEnd.z() : relStart.z(),
                    blockFace == BlockFace.WEST ? relStart.x() : relEnd.x(),
                    blockFace == BlockFace.BOTTOM ? relStart.y() : relEnd.y(),
                    blockFace == BlockFace.NORTH ? relStart.z() : relEnd.z(),
                    blockX,
                    y,
                    blockZ
            );

            if (!face.isEdge()) { // If face isn't an edge, we don't need to check neighbours
                faces.add(face);
                continue;
            }

            var dir = blockFace.toDirection();
            var neighbourBlock = chunk.getBlock(x + dir.normalX(), y + dir.normalY(), z + dir.normalZ(), Block.Getter.Condition.TYPE);

            if (!isFull(neighbourBlock)) {
                faces.add(face);
            }
        }

        return faces;
    }

    private static boolean isFull(Block block) {
        if (block.isAir() || block.isLiquid()) return false;

        Shape shape = block.registry().collisionShape();
        Point relStart = shape.relativeStart();
        Point relEnd = shape.relativeEnd();

        return relStart.x() == 0.0 && relStart.y() == 0.0 && relStart.z() == 0.0 &&
                relEnd.x() == 1.0 && relEnd.y() == 1.0 && relEnd.z() == 1.0;
    }

    private static boolean isEmpty(Section section) {
        return section.blockPalette().count() == 0;
    }

}
