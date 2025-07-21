package dev.emortal.utils;

import com.github.stephengold.joltjni.Jolt;
import com.github.stephengold.joltjni.RVec3;
import com.github.stephengold.joltjni.Vec3;
import com.github.stephengold.joltjni.readonly.QuatArg;
import com.github.stephengold.joltjni.readonly.RVec3Arg;
import com.github.stephengold.joltjni.readonly.Vec3Arg;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.NotNull;

public class CoordinateUtils {

    private CoordinateUtils() {}

    public static @NotNull Vec toVec(Vec3Arg vec3) {
        return new Vec(vec3.getX(), vec3.getY(), vec3.getZ());
    }

    public static @NotNull Pos toPos(Vec3Arg vec3) {
        return new Pos(vec3.getX(), vec3.getY(), vec3.getZ());
    }

    public static @NotNull Vec toVec(RVec3Arg vec3) {
        if (Jolt.isDoublePrecision()) {
            return new Vec((Double) vec3.getX(), (Double) vec3.getY(), (Double) vec3.getZ());
        } else {
            return new Vec((Float) vec3.getX(), (Float) vec3.getY(), (Float) vec3.getZ());
        }
    }

    public static @NotNull Pos toPos(RVec3Arg vec3) {
        if (Jolt.isDoublePrecision()) {
            return new Pos((Double) vec3.getX(), (Double) vec3.getY(), (Double) vec3.getZ());
        } else {
            return new Pos((Float) vec3.getX(), (Float) vec3.getY(), (Float) vec3.getZ());
        }
    }

    public static @NotNull Vec3 toVec3(Point vec) {
        return new Vec3((float)vec.x(), (float)vec.y(), (float)vec.z());
    }

    public static @NotNull RVec3 toRVec3(Point vec) {
        return new RVec3((float)vec.x(), (float)vec.y(), (float)vec.z());
    }

    public static float[] toFloats(QuatArg rotation) {
        return new float[] { rotation.getX(), rotation.getY(), rotation.getZ(), rotation.getW() };
    }
}
