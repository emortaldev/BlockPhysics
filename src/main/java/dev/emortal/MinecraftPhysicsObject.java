package dev.emortal;

import com.jme3.bullet.objects.PhysicsRigidBody;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import org.jetbrains.annotations.Nullable;

public interface MinecraftPhysicsObject {

    PhysicsRigidBody getRigidBody();

    void updateEntity();

    @Nullable ItemDisplayMeta getMeta();

    @Nullable Entity getEntity();

    void destroy();

}
