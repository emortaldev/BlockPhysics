package dev.emortal;

import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import org.jetbrains.annotations.NotNull;

public class NoTickingEntity extends Entity {
    public NoTickingEntity(@NotNull EntityType entityType) {
        super(entityType);

        this.hasPhysics = false;
        setNoGravity(true);
    }

    @Override
    public void tick(long time) {

    }
}
