package dev.emortal.objects;

import com.github.stephengold.joltjni.*;
import com.github.stephengold.joltjni.enumerate.EActivation;
import dev.emortal.MinecraftPhysics;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static dev.emortal.utils.CoordinateUtils.*;

public abstract class MinecraftPhysicsObject {

    private final List<Integer> relatedBodies = new CopyOnWriteArrayList<>();
    private final List<Constraint> constraints = new CopyOnWriteArrayList<>();

    private final @NotNull MinecraftPhysics mcPhysics;
    private final @NotNull BodyCreationSettings bodySettings;
    private final @NotNull Body body;
    private @Nullable Entity entity;
    public MinecraftPhysicsObject(@NotNull MinecraftPhysics mcPhysics, @NotNull BodyCreationSettings bodySettings) {
        this.mcPhysics = mcPhysics;
        this.bodySettings = bodySettings;

        this.body = mcPhysics.getBodyInterface().createBody(this.bodySettings);
        mcPhysics.getBodyInterface().addBody(body, EActivation.Activate);
        mcPhysics.addObject(this);
    }

    public @Nullable Entity setInstance() {
        this.entity = createEntity();
        if (this.entity != null) {
            this.entity.setInstance(mcPhysics.getInstance(), toVec(body.getPosition()));
        }
        return this.entity;
    }

    public void addRelated(Body related) {
        this.relatedBodies.add(related.getId());
    }

    public void addRelatedConstraint(Constraint related) {
        this.constraints.add(related);
    }

    public void removeRelatedConstraint(Constraint related) {
        this.constraints.remove(related);
    }

    public void destroy() {
        for (Constraint constraint : constraints) {
            mcPhysics.removeConstraint(constraint);
        }

        for (int relatedObject : relatedBodies) {
            mcPhysics.getPhysicsSystem().getBodyInterface().removeBody(relatedObject);
            mcPhysics.getPhysicsSystem().getBodyInterface().destroyBody(relatedObject);
        }

        mcPhysics.getPhysicsSystem().getBodyInterface().removeBody(body.getId());
        mcPhysics.getPhysicsSystem().getBodyInterface().destroyBody(body.getId());
        mcPhysics.removeObject(this);
        if (entity != null) {
            entity.remove();
        }
    }

    public @NotNull Body getBody() {
        return body;
    }

    public abstract @Nullable Entity createEntity();

    public @Nullable Entity getEntity() {
        return entity;
    }

    public void update() {
        if (entity == null) return;
        if (!entity.isActive()) return;

        entity.editEntityMeta(AbstractDisplayMeta.class, meta -> {
            meta.setTransformationInterpolationStartDelta(0);

            RVec3 rVec3 = new RVec3();
            Quat quat = new Quat();
            getBody().getPositionAndRotation(rVec3, quat);
            entity.teleport(toPos(rVec3));

            // size not updated as it doesn't change
            meta.setLeftRotation(toFloats(quat));
        });
    }

}
