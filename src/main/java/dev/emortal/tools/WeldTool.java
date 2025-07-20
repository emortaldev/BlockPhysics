package dev.emortal.tools;

import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.PhysicsRayTestResult;
import com.jme3.bullet.joints.ConeJoint;
import com.jme3.bullet.joints.PhysicsJoint;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;
import dev.emortal.MinecraftPhysics;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WeldTool extends Tool {

    private @Nullable PhysicsRigidBody firstObject = null;

    private boolean keepDistance = false;

    private final @NotNull Player player;
    private final @NotNull MinecraftPhysics physicsHandler;
    public WeldTool(@NotNull Player player, @NotNull MinecraftPhysics physicsHandler) {
        super(player, "welder");
        this.player = player;
        this.physicsHandler = physicsHandler;
    }

    @Override
    void onSwitchHands() {
        player.sendMessage("Changed weld mode");
        keepDistance = !keepDistance;
    }

    @Override
    public void onLeftClick() {
        if (firstObject != null) {
            firstObject = null;
            player.sendMessage("Deselected first object");
        }

        List<PhysicsRayTestResult> results = physicsHandler.raycastEntity(player.getPosition().add(0, player.getEyeHeight(), 0), player.getPosition().direction(), 1000);
        if (results.isEmpty()) return;

        PhysicsCollisionObject obj = results.getFirst().getCollisionObject();
        if (!(obj instanceof PhysicsRigidBody rigidBody)) return;

        for (PhysicsJoint physicsJoint : rigidBody.listJoints()) {
            rigidBody.removeJoint(physicsJoint);
        }
        player.sendMessage("Removed weld");
    }

    @Override
    public void onRightClick() {
        List<PhysicsRayTestResult> results = physicsHandler.raycastEntity(player.getPosition().add(0, player.getEyeHeight(), 0), player.getPosition().direction(), 1000);
        if (results.isEmpty()) return;

        PhysicsCollisionObject obj = results.getFirst().getCollisionObject();
        if (!(obj instanceof PhysicsRigidBody rigidBody)) return;

        if (firstObject != null) {
            Vector3f firstObjectPos = new Vector3f();
            Vector3f secondObjectPos = new Vector3f();

            firstObject.getPhysicsLocation(firstObjectPos);
            obj.getPhysicsLocation(secondObjectPos);

            PhysicsJoint joint;
            if (keepDistance) {
                joint = new ConeJoint(firstObject, rigidBody, secondObjectPos.subtract(firstObjectPos), firstObjectPos.subtract(secondObjectPos));
            } else {
                joint = new ConeJoint(firstObject, rigidBody, Vector3f.ZERO, Vector3f.ZERO);
            }
            physicsHandler.getPhysicsSpace().addJoint(joint);

            firstObject.activate();
            rigidBody.activate();

            firstObject = null;

            player.playSound(Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_PLING, Sound.Source.MASTER, 0.5f, 2f), Sound.Emitter.self());
            return;
        }
        player.playSound(Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_PLING, Sound.Source.MASTER, 0.5f, 1.5f), Sound.Emitter.self());
        firstObject = rigidBody;
    }

    @Override
    public ItemStack getItem() {
        return ItemStack.builder(Material.NETHERITE_PICKAXE)
                .customName(Component.text("Welder", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false))
                .set(Tool.TOOL_NAME_TAG, "welder")
                .build();
    }
}
