package dev.emortal.tools;

import com.jme3.bullet.joints.ConeJoint;
import com.jme3.bullet.joints.PhysicsJoint;
import com.jme3.math.Vector3f;
import dev.emortal.MinecraftPhysicsHandler;
import dev.emortal.objects.MinecraftPhysicsObject;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static dev.emortal.Main.raycastEntity;

public class WeldTool extends Tool {

    private @Nullable MinecraftPhysicsObject firstObject = null;

    private boolean keepDistance = false;

    private final @NotNull Player player;
    private final @NotNull MinecraftPhysicsHandler physicsHandler;
    public WeldTool(@NotNull Player player, @NotNull MinecraftPhysicsHandler physicsHandler) {
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

        Entity entity = raycastEntity(player.getInstance(), player.getPosition().add(0, player.getEyeHeight(), 0), player.getPosition().direction(), 100000, (ent) -> {
            if (ent == player) return false;
            return true;
        });

        if (entity == null) return;

        MinecraftPhysicsObject obj = physicsHandler.getFromEntity(entity);

        if (obj == null) return;

        for (PhysicsJoint physicsJoint : obj.getRigidBody().listJoints()) {
            obj.getRigidBody().removeJoint(physicsJoint);
        }
        player.sendMessage("Removed weld");
    }

    @Override
    public void onRightClick() {
        Entity entity = raycastEntity(player.getInstance(), player.getPosition().add(0, player.getEyeHeight(), 0), player.getPosition().direction(), 100000, (ent) -> {
            if (ent == player) return false;
            return true;
        });

        if (entity == null) return;

        MinecraftPhysicsObject obj = physicsHandler.getFromEntity(entity);

        if (obj == null) return;

        if (firstObject != null) {
            Vector3f firstObjectPos = new Vector3f();
            Vector3f secondObjectPos = new Vector3f();

            firstObject.getRigidBody().getPhysicsLocation(firstObjectPos);
            obj.getRigidBody().getPhysicsLocation(secondObjectPos);

            PhysicsJoint joint;
            if (keepDistance) {
                joint = new ConeJoint(firstObject.getRigidBody(), obj.getRigidBody(), secondObjectPos.subtract(firstObjectPos), firstObjectPos.subtract(secondObjectPos));
            } else {
                joint = new ConeJoint(firstObject.getRigidBody(), obj.getRigidBody(), Vector3f.ZERO, Vector3f.ZERO);
            }
            physicsHandler.getPhysicsSpace().addJoint(joint);

            firstObject.getRigidBody().activate();
            obj.getRigidBody().activate();

            firstObject = null;

            player.playSound(Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_PLING, Sound.Source.MASTER, 0.5f, 2f), Sound.Emitter.self());
            return;
        }
        player.playSound(Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_PLING, Sound.Source.MASTER, 0.5f, 1.5f), Sound.Emitter.self());
        firstObject = obj;
    }

    @Override
    public ItemStack getItem() {
        return ItemStack.builder(Material.NETHERITE_PICKAXE)
                .meta(meta -> {
                    meta.displayName(Component.text("Welder", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
                })
                .set(Tool.TOOL_NAME, "welder")
                .build();
    }
}
