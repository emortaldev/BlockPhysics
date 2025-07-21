package dev.emortal.tools;

import com.github.stephengold.joltjni.Body;
import com.github.stephengold.joltjni.RVec3;
import com.github.stephengold.joltjni.SixDofConstraintSettings;
import com.github.stephengold.joltjni.TwoBodyConstraint;
import com.github.stephengold.joltjni.enumerate.EAxis;
import com.github.stephengold.joltjni.readonly.RVec3Arg;
import dev.emortal.MinecraftPhysics;
import dev.emortal.objects.MinecraftPhysicsObject;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.emortal.utils.CoordinateUtils.toVec;
import static dev.emortal.utils.CoordinateUtils.toVec3;

public class GrabberTool extends Tool {

    private final double grabberForce = 7;

    private double holdingDistance = 0.0;
    private @Nullable Body heldObject = null;
    private @Nullable Task holdingTask = null;

    private final Map<Integer, Integer> jointMap = new HashMap<>();

    private final @NotNull Player player;
    private final @NotNull MinecraftPhysics physicsHandler;
    public GrabberTool(@NotNull Player player, @NotNull MinecraftPhysics physicsHandler) {
        super(player, "grabber");
        this.player = player;
        this.physicsHandler = physicsHandler;
    }


    @Override
    boolean onSlotChange(int newSlot, int diff) {
        if (holdingTask == null) return false;

        holdingDistance += diff * 1.5;

        player.playSound(Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_HAT, Sound.Source.MASTER, 0.5f, 2f), Sound.Emitter.self());

        return true;
    }

    @Override
    void onSwitchHands() {

    }

    @Override
    public void onLeftClick() {
        if (holdingTask == null || heldObject == null) return;

        MinecraftPhysicsObject mcObj = physicsHandler.getObjectByBody(heldObject);
        if (mcObj != null && mcObj.getEntity() != null) {
            mcObj.getEntity().setGlowing(false);
        }

        player.playSound(Sound.sound(SoundEvent.BLOCK_AMETHYST_BLOCK_PLACE, Sound.Source.MASTER, 0.5f, 1.8f), Sound.Emitter.self());
        player.playSound(Sound.sound(SoundEvent.ENTITY_BEE_STING, Sound.Source.MASTER, 0.5f, 2f), Sound.Emitter.self());

        RVec3Arg physicsLoc = heldObject.getPosition();

        player.sendPacket(new ParticlePacket(Particle.REVERSE_PORTAL, toVec(physicsLoc), Pos.ZERO, 2.5f, 20));

        Body jointBody = Body.sFixedToWorld();

        SixDofConstraintSettings jointSettings = new SixDofConstraintSettings();
        jointSettings.makeFixedAxis(EAxis.TranslationX);
        jointSettings.makeFixedAxis(EAxis.TranslationY);
        jointSettings.makeFixedAxis(EAxis.TranslationZ);
        jointSettings.setPosition1(physicsLoc);
        jointSettings.setPosition2(physicsLoc);

        TwoBodyConstraint constraint = jointSettings.create(jointBody, heldObject);
        physicsHandler.addConstraint(constraint);

        jointMap.put(heldObject.getId(), jointBody.getId());

        holdingTask.cancel();
        holdingTask = null;
        heldObject = null;
    }

    @Override
    public void onRightClick() {
        if (holdingTask != null) {
            MinecraftPhysicsObject mcObj = physicsHandler.getObjectByBody(heldObject);
            if (mcObj != null && mcObj.getEntity() != null) {
                mcObj.getEntity().setGlowing(false);
            }

            holdingTask.cancel();
            holdingTask = null;
            heldObject = null;

            player.playSound(Sound.sound(SoundEvent.BLOCK_AMETHYST_BLOCK_PLACE, Sound.Source.MASTER, 0.5f, 1.8f), Sound.Emitter.self());
            return;
        }

        List<Body> results = physicsHandler.raycastEntity(player.getPosition().add(0, player.getEyeHeight(), 0), player.getPosition().direction(), 1000);
        if (results.isEmpty()) return;

        Body obj = results.getFirst();

        if (obj == null) return;

        if (jointMap.containsKey(obj)) { // Remove holding joints if any
            int jointId = jointMap.get(obj);
            physicsHandler.getBodyInterface().removeBody(jointId);

            jointMap.remove(obj);
        }

        player.playSound(Sound.sound(SoundEvent.BLOCK_AMETHYST_BLOCK_PLACE, Sound.Source.MASTER, 0.5f, 2f), Sound.Emitter.self());

        obj.setAllowSleeping(false); // TODO: re-enable later
        physicsHandler.getBodyInterface().activateBody(obj.getId());

        heldObject = obj;

        holdingDistance = player.getPosition().distance(toVec(obj.getPosition()));

        MinecraftPhysicsObject mcObj = physicsHandler.getObjectByBody(obj);
        if (mcObj != null && mcObj.getEntity() != null) {
            mcObj.getEntity().setGlowing(true);
        }

        holdingTask = player.scheduler().buildTask(() -> {
            RVec3 physicsVec = obj.getPosition();

            Vec wantedPos = player.getPosition().add(0, player.getEyeHeight(), 0).add(player.getPosition().direction().mul(holdingDistance)).asVec();
            Vec diff = wantedPos.sub(toVec(physicsVec)).asVec();

            obj.setLinearVelocity(toVec3(diff.mul(grabberForce)));
        }).repeat(TaskSchedule.tick(1)).schedule();
    }

    @Override
    public ItemStack getItem() {
        return ItemStack.builder(Material.BLAZE_ROD)
                .customName(Component.text("Grabber", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false))
                .set(Tool.TOOL_NAME_TAG, "grabber")
                .build();
    }
}
