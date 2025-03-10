package dev.emortal.tools;

import com.jme3.bullet.objects.PhysicsRigidBody;
import dev.emortal.MinecraftPhysics;
import dev.emortal.PlayerDisplayPart;
import dev.emortal.objects.MinecraftPhysicsObject;
import dev.emortal.objects.RagdollPhysics;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.HeadProfile;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;

import static dev.emortal.commands.PlayerSizeCommand.PLAYER_SIZE;
import static dev.emortal.utils.CoordinateUtils.toVector3;

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

    }

    @Override
    public void onRightClick() {
        player.playSound(Sound.sound(SoundEvent.ENTITY_VILLAGER_AMBIENT, Sound.Source.MASTER, 1f, 1f), Sound.Emitter.self());

        Instance instance = player.getInstance();
        Pos startPos = player.getPosition().add(0, player.getEyeHeight(), 0).add(player.getPosition().direction().mul(5));
        // all halves \/
        Vec torsoSize = new Vec(4.0f/16.0f, 6.0f/16.0f, 2.0f/16.0f).mul(PLAYER_SIZE);
//            Vector3 headSize = new Vector3(4.0f/16.0f, 4.0f/16.0f, 4.0f/16.0f);
        Vec headSize = new Vec(3f/16.0f, 3.0f/16.0f, 3f/16.0f).mul(PLAYER_SIZE);
//            Vector3 limbSize = new Vector3(2.0f/16.0f, 6.0f/16.0f, 2.0f/16.0f);
        Vec limbSize = new Vec(1f/16.0f, 6.0f/16.0f, 1f/16.0f).mul(PLAYER_SIZE);

        MinecraftPhysicsObject torso = new RagdollPhysics(physicsHandler, player,null, PlayerDisplayPart.TORSO, toVector3(startPos), torsoSize, 1);
        PhysicsRigidBody torsoBody = (PhysicsRigidBody) torso.getCollisionObject();
        MinecraftPhysicsObject head = new RagdollPhysics(physicsHandler, player, torsoBody, PlayerDisplayPart.HEAD, toVector3(startPos.add(new Vec(0, 0.62, 0).mul(PLAYER_SIZE))), headSize, 1);
        MinecraftPhysicsObject rightArm = new RagdollPhysics(physicsHandler, player, torsoBody, PlayerDisplayPart.RIGHT_ARM, toVector3(startPos.add(new Vec(0.37, 0, 0).mul(PLAYER_SIZE))), limbSize, 1);
        MinecraftPhysicsObject leftArm = new RagdollPhysics(physicsHandler, player, torsoBody, PlayerDisplayPart.LEFT_ARM, toVector3(startPos.add(new Vec(-0.37, 0, 0).mul(PLAYER_SIZE))), limbSize, 1);
        MinecraftPhysicsObject rightLeg = new RagdollPhysics(physicsHandler, player, torsoBody, PlayerDisplayPart.RIGHT_LEG, toVector3(startPos.add(new Vec(0.13, -0.72, 0).mul(PLAYER_SIZE))), limbSize, 1);
        MinecraftPhysicsObject leftLeg = new RagdollPhysics(physicsHandler, player, torsoBody, PlayerDisplayPart.LEFT_LEG, toVector3(startPos.add(new Vec(-0.13, -0.72, 0).mul(PLAYER_SIZE))), limbSize, 1);
//        Main.paused = true;

        torso.setInstance();
        head.setInstance();
        rightArm.setInstance();
        leftArm.setInstance();
        rightLeg.setInstance();
        leftLeg.setInstance();
    }

    @Override
    public ItemStack getItem() {
        return ItemStack.builder(Material.PLAYER_HEAD)
                .set(ItemComponent.PROFILE, new HeadProfile(player.getSkin()))
                .customName(Component.text("Player Spawner", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false))
                .set(Tool.TOOL_NAME_TAG, "playerspawner")
                .build();
    }
}
