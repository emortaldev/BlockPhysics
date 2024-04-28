package dev.emortal;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.PlaneCollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Plane;
import com.jme3.math.Vector3f;
import dev.emortal.objects.MinecraftPhysicsObject;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Entity;
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class MinecraftPhysicsHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MinecraftPhysicsHandler.class);

    private @NotNull PhysicsSpace physicsSpace;
    private @NotNull PhysicsRigidBody floor;

    public final @NotNull List<MinecraftPhysicsObject> objects = new CopyOnWriteArrayList<>();
    public final @NotNull Map<Entity, MinecraftPhysicsObject> entityObjectMap = new HashMap<>();
    private final Instance instance;

    public MinecraftPhysicsHandler(Instance instance) {
        this.instance = instance;

        instance.scheduleNextTick((a) -> {
            physicsSpace = new PhysicsSpace(PhysicsSpace.BroadphaseType.DBVT);

            // Default: -9.81f
            // Minecraft: -31.36f
            physicsSpace.setGravity(new Vector3f(0, -17f, 0));

            CollisionShape planeShape = new PlaneCollisionShape(new Plane(Vector3f.UNIT_Y, 0f));
            floor = new PhysicsRigidBody(planeShape, PhysicsRigidBody.massForStatic);

            physicsSpace.addCollisionObject(floor);
        });

        MinecraftServer.getGlobalEventHandler().addListener(RemoveEntityFromInstanceEvent.class, e -> {
            entityObjectMap.remove(e.getEntity());
        });
    }

    public void update(float delta) {
        if (physicsSpace == null) return;

        physicsSpace.update(delta);

        for (MinecraftPhysicsObject object : objects) {
            object.updateEntity();
        }
    }

    public MinecraftPhysicsObject getFromEntity(Entity entity) {
        return entityObjectMap.get(entity);
    }

    public PhysicsSpace getPhysicsSpace() {
        return physicsSpace;
    }
}
