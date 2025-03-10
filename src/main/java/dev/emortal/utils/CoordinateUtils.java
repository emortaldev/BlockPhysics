package dev.emortal.utils;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.NotNull;

public class CoordinateUtils {

    private CoordinateUtils() {}

    public static @NotNull Vec toVec(Vector3f vector3) {
        return new Vec(vector3.x, vector3.y, vector3.z);
    }
    public static @NotNull Pos toPos(Vector3f vector3) {
        return new Pos(vector3.x, vector3.y, vector3.z);
    }
    public static @NotNull Vector3f toVector3(Point vec) {
        return new Vector3f((float)vec.x(), (float)vec.y(), (float)vec.z());
    }
    public static float[] toFloats(Quaternion rotation) {
        return new float[] { rotation.getX(), rotation.getY(), rotation.getZ(), rotation.getW() };
    }
}
