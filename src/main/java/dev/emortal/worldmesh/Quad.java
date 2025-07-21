package dev.emortal.worldmesh;

import com.github.stephengold.joltjni.Triangle;
import com.github.stephengold.joltjni.Vec3;

public record Quad(Vec3 point1, Vec3 point2, Vec3 point3, Vec3 point4) {
    public Triangle[] triangles() {
        Triangle[] triangles = new Triangle[2];
        triangles[0] = new Triangle(point3, point2, point1);
        triangles[1] = new Triangle(point1, point4, point3);
        return triangles;
    }

    public Vec3 min() {
        return new Vec3(min(point1.getX(), point2.getX(), point3.getX(), point4.getX()), min(point1.getY(), point2.getY(), point3.getY(), point4.getY()), min(point1.getZ(), point2.getZ(), point3.getZ(), point4.getZ()));
    }

    private float min(float a, float b, float c, float d) {
        return Math.min(a, Math.min(b, Math.min(c, d)));
    }

    public Vec3 max() {
        return new Vec3(max(point1.getX(), point2.getX(), point3.getX(), point4.getX()), max(point1.getY(), point2.getY(), point3.getY(), point4.getY()), max(point1.getZ(), point2.getZ(), point3.getZ(), point4.getZ()));
    }

    private float max(float a, float b, float c, float d) {
        return Math.max(a, Math.max(b, Math.max(c, d)));
    }

}