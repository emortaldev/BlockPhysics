package dev.emortal.tools;

import com.github.stephengold.joltjni.Body;
import com.github.stephengold.joltjni.Quat;
import com.github.stephengold.joltjni.RVec3;
import dev.emortal.MinecraftPhysics;
import dev.emortal.PlayerDisplayPart;
import dev.emortal.objects.MinecraftPhysicsObject;
import dev.emortal.objects.RagdollPhysics;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.HeadProfile;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

import java.util.concurrent.ThreadLocalRandom;

import static dev.emortal.commands.PlayerSizeCommand.PLAYER_SIZE;
import static dev.emortal.utils.CoordinateUtils.toRVec3;
import static dev.emortal.utils.CoordinateUtils.toVec3;

public class PlayerSpawnerTool extends Tool {

    private final @NotNull Player player;
    private final @NotNull MinecraftPhysics physicsHandler;
    public PlayerSpawnerTool(@NotNull Player player, @NotNull MinecraftPhysics physicsHandler) {
        super(player, "playerspawner");
        this.player = player;
        this.physicsHandler = physicsHandler;
    }


    @Override
    void onSwitchHands() {

    }

    @Override
    public void onLeftClick() {
        player.playSound(Sound.sound(SoundEvent.ENTITY_VILLAGER_AMBIENT, Sound.Source.MASTER, 1f, 1f), Sound.Emitter.self());

        Pos startPos = player.getPosition().add(0, player.getEyeHeight(), 0).add(player.getPosition().direction().mul(5));
        PlayerBody playerBody = spawnAtPos(startPos);

        RVec3 torsoPos = playerBody.torso().getBody().getPosition();
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        RVec3 impulsePos = new RVec3(rand.nextFloat(-1f, 1f), rand.nextFloat(-1.5f, 1.5f), rand.nextFloat(-1f, 1f));
        impulsePos.addInPlace(torsoPos.xx(), torsoPos.yy(), torsoPos.zz());
        playerBody.torso().getBody().addImpulse(toVec3(startPos.direction().mul(2000)), impulsePos);
    }

    @Override
    public void onRightClick() {
        player.playSound(Sound.sound(SoundEvent.ENTITY_VILLAGER_AMBIENT, Sound.Source.MASTER, 1f, 1f), Sound.Emitter.self());

        Pos startPos = player.getPosition().add(0, player.getEyeHeight(), 0).add(player.getPosition().direction().mul(5));
        spawnAtPos(startPos);
    }

    record PlayerBody(
            MinecraftPhysicsObject torso,
            MinecraftPhysicsObject head,
            MinecraftPhysicsObject rightArm,
            MinecraftPhysicsObject leftArm,
            MinecraftPhysicsObject rightLeg,
            MinecraftPhysicsObject leftLeg
    ) {}

    private PlayerBody spawnAtPos(Pos startPos) {
        // all halves \/
        Vec torsoSize = new Vec(4.0f/16.0f, 6.0f/16.0f, 2.0f/16.0f).mul(PLAYER_SIZE);
//            Vector3 headSize = new Vector3(4.0f/16.0f, 4.0f/16.0f, 4.0f/16.0f);
        Vec headSize = new Vec(3f/16.0f, 3.0f/16.0f, 3f/16.0f).mul(PLAYER_SIZE);
//            Vector3 limbSize = new Vector3(2.0f/16.0f, 6.0f/16.0f, 2.0f/16.0f);
        Vec limbSize = new Vec(1f/16.0f, 6.0f/16.0f, 1f/16.0f).mul(PLAYER_SIZE);

        float yaw = -player.getPosition().yaw() + 180;
        double yawRad = Math.toRadians(yaw);
        Quaternionf yawQuat = new Quaternionf(new AxisAngle4f((float) yawRad, 0, 1, 0));
        Quat yawQuat2 = new Quat(yawQuat.x(), yawQuat.y(), yawQuat.z(), yawQuat.w());

        MinecraftPhysicsObject torso = new RagdollPhysics(physicsHandler, player,null, PlayerDisplayPart.TORSO, toRVec3(startPos), yawQuat2, torsoSize);
        Body torsoBody = torso.getBody();
        MinecraftPhysicsObject head = new RagdollPhysics(physicsHandler, player, torsoBody, PlayerDisplayPart.HEAD, toRVec3(startPos.add(new Vec(0,  torsoSize.y() * 2, 0).rotateAroundY(yawRad).mul(PLAYER_SIZE))), yawQuat2, headSize);
        MinecraftPhysicsObject rightArm = new RagdollPhysics(physicsHandler, player, torsoBody, PlayerDisplayPart.RIGHT_ARM, toRVec3(startPos.add(new Vec((torsoSize.x() / 1.35) * 2, 0, 0).rotateAroundY(yawRad).mul(PLAYER_SIZE))), yawQuat2, limbSize);
        MinecraftPhysicsObject leftArm = new RagdollPhysics(physicsHandler, player, torsoBody, PlayerDisplayPart.LEFT_ARM, toRVec3(startPos.add(new Vec((torsoSize.x() / 1.35) * -2, 0, 0).rotateAroundY(yawRad).mul(PLAYER_SIZE))), yawQuat2, limbSize);
        MinecraftPhysicsObject rightLeg = new RagdollPhysics(physicsHandler, player, torsoBody, PlayerDisplayPart.RIGHT_LEG, toRVec3(startPos.add(new Vec(0.13, -0.72, 0).rotateAroundY(yawRad).mul(PLAYER_SIZE))), yawQuat2, limbSize);
        MinecraftPhysicsObject leftLeg = new RagdollPhysics(physicsHandler, player, torsoBody, PlayerDisplayPart.LEFT_LEG, toRVec3(startPos.add(new Vec(-0.13, -0.72, 0).rotateAroundY(yawRad).mul(PLAYER_SIZE))), yawQuat2, limbSize);

        torso.setInstance();
        head.setInstance();
        rightArm.setInstance();
        leftArm.setInstance();
        rightLeg.setInstance();
        leftLeg.setInstance();

        return new PlayerBody(torso, head, rightArm, leftArm, rightLeg, leftLeg);
    }

    @Override
    public ItemStack getItem() {
        return ItemStack.builder(Material.PLAYER_HEAD)
                .set(DataComponents.PROFILE, new HeadProfile(player.getSkin()))
                .customName(Component.text("Player Spawner", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false))
                .set(Tool.TOOL_NAME_TAG, "playerspawner")
                .build();
    }
}
