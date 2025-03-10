package dev.emortal.objects;

import com.jme3.bullet.NativePhysicsObject;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.joints.PhysicsJoint;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Transform;
import dev.emortal.MinecraftPhysics;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static dev.emortal.utils.CoordinateUtils.*;

public abstract class MinecraftPhysicsObject {

    private final List<NativePhysicsObject> relatedObjects = new ArrayList<>();

    private final @NotNull MinecraftPhysics mcPhysics;
    private final @NotNull PhysicsCollisionObject collisionObject;
    private @NotNull Vec size;
    private @Nullable Entity entity;
    private boolean alwaysActive = false;
    public MinecraftPhysicsObject(@NotNull MinecraftPhysics mcPhysics, @NotNull PhysicsCollisionObject collisionObject, @NotNull Vec size) {
        this.mcPhysics = mcPhysics;
        this.collisionObject = collisionObject;
        this.size = size;

        mcPhysics.getPhysicsSpace().add(collisionObject);
        mcPhysics.addObject(this);


    }

    public Entity setInstance() {
        this.entity = createEntity();
        if (this.entity != null) {
            Transform transform = new Transform();
            collisionObject.getTransform(transform);
            this.entity.setInstance(mcPhysics.getInstance(), toVec(transform.getTranslation()));
        }
        return this.entity;
    }

    public void addRelated(NativePhysicsObject related) {
        this.relatedObjects.add(related);
    }

    public void destroy() {
        if (collisionObject instanceof PhysicsRigidBody rigidBody) {
            for (PhysicsJoint physicsJoint : rigidBody.listJoints()) {
                mcPhysics.getPhysicsSpace().remove(physicsJoint);
            }
        }

        for (NativePhysicsObject relatedObject : relatedObjects) {
            mcPhysics.getPhysicsSpace().remove(relatedObject);
        }
        mcPhysics.getPhysicsSpace().remove(collisionObject);
        mcPhysics.removeObject(this);
        if (entity != null) {
            entity.remove();
        }
    }

    public @NotNull PhysicsCollisionObject getCollisionObject() {
        return collisionObject;
    }

    public void setAlwaysActive(boolean alwaysActive) {
        this.alwaysActive = alwaysActive;
    }

    public abstract @Nullable Entity createEntity();

    public @Nullable Entity getEntity() {
        return entity;
    }

    public @NotNull Vec getSize() {
        return size;
    }

    public void update() {
        if (entity == null) return;
        if (!entity.isActive()) return;
        if (alwaysActive) collisionObject.activate(true);

        entity.editEntityMeta(AbstractDisplayMeta.class, meta -> {
            Transform transform = new Transform();
            collisionObject.getTransform(transform);

            meta.setTransformationInterpolationDuration(1);
            meta.setPosRotInterpolationDuration(1);
            meta.setTransformationInterpolationStartDelta(0);

            entity.teleport(toPos(transform.getTranslation()));

            // size not updated as it doesn't change
            meta.setLeftRotation(toFloats(transform.getRotation()));
        });
    }

}
