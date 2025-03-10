package dev.emortal;

import com.jme3.bullet.NativePhysicsObject;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.PlaneCollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Plane;
import com.jme3.math.Vector3f;
import dev.emortal.objects.MinecraftPhysicsObject;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class MinecraftPhysics {

    private static final Logger LOGGER = LoggerFactory.getLogger(MinecraftPhysics.class);

    private @NotNull PhysicsSpace physicsSpace;
    private @NotNull PhysicsRigidBody floor;

    public static final Tag<MinecraftPhysicsObject> PHYSICS_BLOCK_TAG = Tag.Transient("physicsblock");
    public static final Tag<PhysicsRigidBody> PLAYER_RIGID_BODY_TAG = Tag.Transient("playerrigidbody");


    private final @NotNull List<MinecraftPhysicsObject> objects = new CopyOnWriteArrayList<>();
    private final @NotNull Map<NativePhysicsObject, MinecraftPhysicsObject> objectMap = new ConcurrentHashMap<>();
    private final Instance instance;

    public MinecraftPhysics(Instance instance) {
        this.instance = instance;

        instance.scheduleNextTick((a) -> {
            physicsSpace = new PhysicsSpace(PhysicsSpace.BroadphaseType.DBVT);

            // Default: -9.81f
            // Minecraft: -31.36f
            physicsSpace.setGravity(new Vector3f(0, -17f, 0));

            CollisionShape planeShape = new PlaneCollisionShape(new Plane(Vector3f.UNIT_Y, 0f));
            floor = new PhysicsRigidBody(planeShape, PhysicsRigidBody.massForStatic);

            physicsSpace.add(floor);
        });
    }

    public void update(float delta) {
        if (physicsSpace == null) return;

        physicsSpace.update(delta);

        for (MinecraftPhysicsObject object : objects) {
//            object.getRigidBody().isInWorld()
            // TODO: use this to automatically remove?
            object.update();
        }
    }

    public @NotNull List<MinecraftPhysicsObject> getObjects() {
        return objects;
    }

    public void addObject(MinecraftPhysicsObject object) {
        objects.add(object);
        objectMap.put(object.getCollisionObject(), object);
    }
    public void removeObject(MinecraftPhysicsObject object) {
        objects.remove(object);
        objectMap.remove(object.getCollisionObject());
    }

    public @Nullable MinecraftPhysicsObject getObjectByPhysicsObject(NativePhysicsObject physicsObject) {
        return objectMap.get(physicsObject);
    }

    public Instance getInstance() {
        return instance;
    }

    public PhysicsSpace getPhysicsSpace() {
        return physicsSpace;
    }
}
