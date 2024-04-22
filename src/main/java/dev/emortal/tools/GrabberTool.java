package dev.emortal.tools;

import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;
import dev.emortal.MinecraftPhysicsHandler;
import dev.emortal.objects.JointObject;
import dev.emortal.objects.MinecraftPhysicsObject;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import static dev.emortal.Main.raycastEntity;
import static dev.emortal.objects.BlockRigidBody.toVec;
import static dev.emortal.objects.BlockRigidBody.toVector3;

public class GrabberTool extends Tool {

    private final double grabberForce = 7;

    private double holdingDistance = 0.0;
    private @Nullable MinecraftPhysicsObject heldObject = null;
    private @Nullable Task holdingTask = null;

    private final Map<MinecraftPhysicsObject, MinecraftPhysicsObject> jointMap = new HashMap<>();

    private final @NotNull Player player;
    private final @NotNull MinecraftPhysicsHandler physicsHandler;
    public GrabberTool(@NotNull Player player, @NotNull MinecraftPhysicsHandler physicsHandler) {
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
        if (holdingTask == null) return;

        player.playSound(Sound.sound(SoundEvent.BLOCK_AMETHYST_BLOCK_PLACE, Sound.Source.MASTER, 0.5f, 1.8f), Sound.Emitter.self());

        player.playSound(Sound.sound(SoundEvent.ENTITY_BEE_STING, Sound.Source.MASTER, 0.5f, 2f), Sound.Emitter.self());

        Vector3f physicsLoc = new Vector3f();
        heldObject.getRigidBody().getPhysicsLocation(physicsLoc);

        MinecraftPhysicsObject jointObject = new JointObject(physicsHandler, physicsLoc, heldObject.getRigidBody());

        jointMap.put(heldObject, jointObject);

        holdingTask.cancel();
        holdingTask = null;
        heldObject = null;
    }

    @Override
    public void onRightClick() {
        if (holdingTask != null) {
            holdingTask.cancel();
            holdingTask = null;
            heldObject = null;

            player.playSound(Sound.sound(SoundEvent.BLOCK_AMETHYST_BLOCK_PLACE, Sound.Source.MASTER, 0.5f, 1.8f), Sound.Emitter.self());
            return;
        }

        Entity entity = raycastEntity(player.getInstance(), player.getPosition().add(0, player.getEyeHeight(), 0), player.getPosition().direction(), 100000, (ent) -> {
            if (ent == player) return false;
            return true;
        });

        if (entity == null) return;

        MinecraftPhysicsObject obj = physicsHandler.getFromEntity(entity);

        if (obj == null) return;

        if (jointMap.containsKey(obj)) { // Remove holding joints if any
            jointMap.get(obj).destroy();
            jointMap.remove(obj);
        }

        player.playSound(Sound.sound(SoundEvent.BLOCK_AMETHYST_BLOCK_PLACE, Sound.Source.MASTER, 0.5f, 2f), Sound.Emitter.self());

        obj.getRigidBody().activate();
        heldObject = obj;

        holdingDistance = player.getPosition().distance(entity.getPosition());

        holdingTask = player.scheduler().buildTask(() -> {
            PhysicsRigidBody rigidBody = obj.getRigidBody();

            Vector3f physicsVec = new Vector3f();
            rigidBody.getPhysicsLocation(physicsVec);

            Vec wantedPos = Vec.fromPoint(player.getPosition().add(0, player.getEyeHeight(), 0).add(player.getPosition().direction().mul(holdingDistance)));
            Vec diff = Vec.fromPoint(wantedPos.sub(toVec(physicsVec)));
            rigidBody.setLinearVelocity(toVector3(diff.mul(grabberForce)));
        }).repeat(TaskSchedule.tick(1)).schedule();
    }

    @Override
    public ItemStack getItem() {
        return ItemStack.builder(Material.BLAZE_ROD)
                .meta(meta -> {
                    meta.displayName(Component.text("Grabber", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
                })
                .set(Tool.TOOL_NAME, "grabber")
                .build();
    }
}
