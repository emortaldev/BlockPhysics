package dev.emortal.tools;

import com.github.stephengold.joltjni.Body;
import com.github.stephengold.joltjni.RVec3;
import com.github.stephengold.joltjni.SixDofConstraintSettings;
import com.github.stephengold.joltjni.TwoBodyConstraint;
import com.github.stephengold.joltjni.enumerate.EAxis;
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

    private @Nullable Body firstObject = null;

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

        List<Body> results = physicsHandler.raycastEntity(player.getPosition().add(0, player.getEyeHeight(), 0), player.getPosition().direction(), 1000);
        if (results.isEmpty()) return;

        Body obj = results.getFirst();
        if (obj == null) return;

        // TODO: unsure
//        for (PhysicsJoint physicsJoint : rigidBody.listJoints()) {
//            rigidBody.removeJoint(physicsJoint);
//        }
        player.sendMessage("Removed weld");
    }

    @Override
    public void onRightClick() {
        List<Body> results = physicsHandler.raycastEntity(player.getPosition().add(0, player.getEyeHeight(), 0), player.getPosition().direction(), 1000);
        if (results.isEmpty()) return;

        Body obj = results.getFirst();
        if (obj == null) return;

        if (firstObject != null) {
            RVec3 firstObjectPos = firstObject.getPosition();
            RVec3 secondObjectPos = obj.getPosition();

            SixDofConstraintSettings settings = new SixDofConstraintSettings();
            settings.makeFixedAxis(EAxis.TranslationX);
            settings.makeFixedAxis(EAxis.TranslationY);
            settings.makeFixedAxis(EAxis.TranslationZ);
            if (keepDistance) {
                RVec3 first = new RVec3(firstObjectPos);
                first.addInPlace(-secondObjectPos.x(), -secondObjectPos.y(), -secondObjectPos.z());
                RVec3 second = new RVec3(secondObjectPos);
                first.addInPlace(-firstObjectPos.x(), -firstObjectPos.y(), -firstObjectPos.z());

                settings.setPosition1(first);
                settings.setPosition2(second);

            } else {
                settings.setPosition1(firstObjectPos);
                settings.setPosition2(secondObjectPos);
            }
            TwoBodyConstraint constraint = settings.create(firstObject, obj);
            physicsHandler.addConstraint(constraint);

            physicsHandler.getBodyInterface().activateBody(obj.getId());
            physicsHandler.getBodyInterface().activateBody(firstObject.getId());

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
                .customName(Component.text("Welder", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false))
                .set(Tool.TOOL_NAME_TAG, "welder")
                .build();
    }
}
