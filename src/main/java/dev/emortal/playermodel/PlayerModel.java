package dev.emortal.playermodel;

import kr.toxicity.library.dynamicuv.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PlayerModel {

    static final UVNamespace NAMESPACE = new UVNamespace("emortalmc", "player");
    private static final float PIXEL = 1f / 32;

    private record Cuboid(float x, float y, float z, int w, int h, int d, int u, int v) {
        Cuboid withUv(int u, int v) {
            return new Cuboid(x, y, z, w, h, d, u, v);
        }
    }

    private PlayerModel() {}

    static List<UVModel> skin(boolean slim) {
        int arm = slim ? 3 : 4;
        String suffix = slim ? "_slim" : "";
        return List.of(
            layered("head", new Cuboid(0f, 28f, 0f, 8, 8, 8, 0, 0), 32, 0, 0.5f),
            layered("body", new Cuboid(0f, 18f, 0f, 8, 12, 4, 16, 16), 16, 32, 0.25f),
            layered("right_arm" + suffix, new Cuboid(4f + arm / 2f, 18f, 0f, arm, 12, 4, 40, 16), 40, 32, 0.25f),
            layered("left_arm" + suffix, new Cuboid(-4f - arm / 2f, 18f, 0f, arm, 12, 4, 32, 48), 48, 48, 0.25f),
            layered("right_leg", new Cuboid(2f, 6f, 0f, 4, 12, 4, 0, 16), 0, 32, 0.25f),
            layered("left_leg", new Cuboid(-2f, 6f, 0f, 4, 12, 4, 16, 48), 0, 48, 0.25f)
        );
    }

    static UVModel cape() {
        UVModel model = new UVModel(NAMESPACE, "cape");
        model.addElement(element(new Cuboid(0f, 16f, 2.5f, 10, 16, 1, 0, 0), UVElement.ColorType.RGB, 0f, true));
        return model;
    }

    private static UVModel layered(String name, Cuboid base, int overlayU, int overlayV, float inflate) {
        UVModel model = new UVModel(NAMESPACE, name);
        model.addElement(element(base, UVElement.ColorType.RGB, 0f, false));
        model.addElement(element(base.withUv(overlayU, overlayV), UVElement.ColorType.COMPLEX_ARGB, inflate, false));
        return model;
    }

    private static UVElement element(Cuboid c, UVElement.ColorType type, float inflate, boolean outsideFront) {
        return new UVElement(
            new ElementVector((c.w() + 2 * inflate) * PIXEL, (c.h() + 2 * inflate) * PIXEL, (c.d() + 2 * inflate) * PIXEL),
            new ElementVector(c.x() * PIXEL, c.y() * PIXEL, c.z() * PIXEL),
            new UVSpace(c.w(), c.h(), c.d()),
            type,
            faces(c, outsideFront)
        );
    }

    private static Map<UVFace, UVPos> faces(Cuboid c, boolean outsideFront) {
        UVPos front = new UVPos(c.u() + c.d(), c.v() + c.d());
        UVPos back = new UVPos(c.u() + 2 * c.d() + c.w(), c.v() + c.d());
        UVPos north = outsideFront ? back : front;
        UVPos south = outsideFront ? front : back;
        Map<UVFace, UVPos> map = new LinkedHashMap<>();
        map.put(UVFace.NORTH, north);
        map.put(UVFace.SOUTH, south);
        map.put(UVFace.EAST, new UVPos(c.u(), c.v() + c.d()));
        map.put(UVFace.WEST, new UVPos(c.u() + c.d() + c.w(), c.v() + c.d()));
        map.put(UVFace.UP, new UVPos(c.u() + c.d(), c.v()));
        map.put(UVFace.DOWN, new UVPos(c.u() + c.d() + c.w(), c.v()));
        return map;
    }
}