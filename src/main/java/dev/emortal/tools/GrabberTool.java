package dev.emortal.tools;

import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.PhysicsRayTestResult;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.EmptyShape;
import com.jme3.bullet.joints.ConeJoint;
import com.jme3.bullet.joints.PhysicsJoint;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;
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
import static dev.emortal.utils.CoordinateUtils.toVector3;

public class GrabberTool extends Tool {

    private final double grabberForce = 7;

    private double holdingDistance = 0.0;
    private @Nullable PhysicsRigidBody heldObject = null;
    private @Nullable Task holdingTask = null;

    private final Map<PhysicsRigidBody, PhysicsRigidBody> jointMap = new HashMap<>();

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

        MinecraftPhysicsObject mcObj = physicsHandler.getObjectByPhysicsObject(heldObject);
        if (mcObj != null && mcObj.getEntity() != null) {
            mcObj.getEntity().setGlowing(false);
        }

        player.playSound(Sound.sound(SoundEvent.BLOCK_AMETHYST_BLOCK_PLACE, Sound.Source.MASTER, 0.5f, 1.8f), Sound.Emitter.self());
        player.playSound(Sound.sound(SoundEvent.ENTITY_BEE_STING, Sound.Source.MASTER, 0.5f, 2f), Sound.Emitter.self());

        Vector3f physicsLoc = new Vector3f();
        heldObject.getPhysicsLocation(physicsLoc);

        player.sendPacket(new ParticlePacket(Particle.REVERSE_PORTAL, toVec(physicsLoc), Pos.ZERO, 2.5f, 20));

        CollisionShape jointBoxShape = new EmptyShape(false);
        PhysicsRigidBody jointRigidBody = new PhysicsRigidBody(jointBoxShape, PhysicsRigidBody.massForStatic);
        jointRigidBody.setPhysicsLocation(physicsLoc);
        physicsHandler.getPhysicsSpace().add(jointRigidBody);
        ConeJoint joint = new ConeJoint(heldObject, jointRigidBody, Vector3f.ZERO, Vector3f.ZERO);
        physicsHandler.getPhysicsSpace().addJoint(joint);

        jointMap.put(heldObject, jointRigidBody);

        holdingTask.cancel();
        holdingTask = null;
        heldObject = null;
    }

    @Override
    public void onRightClick() {
        if (holdingTask != null) {
            MinecraftPhysicsObject mcObj = physicsHandler.getObjectByPhysicsObject(heldObject);
            if (mcObj != null && mcObj.getEntity() != null) {
                mcObj.getEntity().setGlowing(false);
            }

            holdingTask.cancel();
            holdingTask = null;
            heldObject = null;

            player.playSound(Sound.sound(SoundEvent.BLOCK_AMETHYST_BLOCK_PLACE, Sound.Source.MASTER, 0.5f, 1.8f), Sound.Emitter.self());
            return;
        }

        List<PhysicsRayTestResult> results = physicsHandler.raycastEntity(player.getPosition().add(0, player.getEyeHeight(), 0), player.getPosition().direction(), 1000);
        if (results.isEmpty()) return;

        PhysicsCollisionObject obj = results.getFirst().getCollisionObject();
        if (!(obj instanceof PhysicsRigidBody rigidBody)) return;

        if (jointMap.containsKey(obj)) { // Remove holding joints if any
            PhysicsRigidBody joint = jointMap.get(obj);
            for (PhysicsJoint physicsJoint : joint.listJoints()) {
                physicsHandler.getPhysicsSpace().remove(physicsJoint);
            }
            physicsHandler.getPhysicsSpace().remove(joint);

            jointMap.remove(obj);
        }

        player.playSound(Sound.sound(SoundEvent.BLOCK_AMETHYST_BLOCK_PLACE, Sound.Source.MASTER, 0.5f, 2f), Sound.Emitter.self());

        rigidBody.activate();
        heldObject = rigidBody;

        Vector3f objPos = new Vector3f();
        obj.getPhysicsLocation(objPos);
        holdingDistance = player.getPosition().distance(toVec(objPos));

        MinecraftPhysicsObject mcObj = physicsHandler.getObjectByPhysicsObject(obj);
        if (mcObj != null && mcObj.getEntity() != null) {
            mcObj.getEntity().setGlowing(true);
        }

        holdingTask = player.scheduler().buildTask(() -> {
            Vector3f physicsVec = new Vector3f();
            obj.getPhysicsLocation(physicsVec);

            Vec wantedPos = player.getPosition().add(0, player.getEyeHeight(), 0).add(player.getPosition().direction().mul(holdingDistance)).asVec();
            Vec diff = wantedPos.sub(toVec(physicsVec)).asVec();

            rigidBody.setLinearVelocity(toVector3(diff.mul(grabberForce)));
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
