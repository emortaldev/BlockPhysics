package dev.emortal;

import com.github.stephengold.joltjni.CustomContactListener;
import com.github.stephengold.joltjni.Vec3;
import dev.emortal.objects.MinecraftPhysicsObject;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.sound.SoundEvent;

import static dev.emortal.utils.CoordinateUtils.toVec;

public class MCContactListener extends CustomContactListener {

    private final MinecraftPhysics physics;
    public MCContactListener(MinecraftPhysics physics) {
        this.physics = physics;
    }

    @Override
    public void onContactAdded(long body1Va, long body2Va, long manifoldVa, long settingsVa) {
        MinecraftPhysicsObject object = physics.getObjectByVa(body2Va);
        if (object == null) return;

        Vec3 linearVelocity = object.getBody().getLinearVelocity();

        double lengthSq = linearVelocity.lengthSq();
        if (lengthSq > 5 * 5) {
            physics.getInstance().playSound(Sound.sound(SoundEvent.BLOCK_GILDED_BLACKSTONE_HIT, Sound.Source.MASTER, 0.2f, 1f), toVec(object.getBody().getPosition()));
        }
    }

    @Override
    public void onContactPersisted(long body1Va, long body2Va, long manifoldVa, long settingsVa) {

    }

    @Override
    public void onContactRemoved(long pairVa) {

    }

    @Override
    public int onContactValidate(long body1Va, long body2Va, double baseOffsetX, double baseOffsetY, double baseOffsetZ, long collisionResultVa) {
        return 0;
    }
}
