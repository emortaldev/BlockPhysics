package dev.emortal.tools;

import com.jme3.math.Vector3f;
import dev.emortal.MinecraftPhysicsHandler;
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
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.metadata.PlayerHeadMeta;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;

import static dev.emortal.commands.PlayerSizeCommand.PLAYER_SIZE;
import static dev.emortal.objects.BlockRigidBody.toVector3;

public class PlayerSpawnerTool extends Tool {

    private final @NotNull Player player;
    private final @NotNull MinecraftPhysicsHandler physicsHandler;
    public PlayerSpawnerTool(@NotNull Player player, @NotNull MinecraftPhysicsHandler physicsHandler) {
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
        Vector3f torsoSize = toVector3(new Vec(4.0f/16.0f, 6.0f/16.0f, 2.0f/16.0f).mul(PLAYER_SIZE));
//            Vector3 headSize = new Vector3(4.0f/16.0f, 4.0f/16.0f, 4.0f/16.0f);
        Vector3f headSize = toVector3(new Vec(1f/16.0f, 4.0f/16.0f, 1f/16.0f).mul(PLAYER_SIZE));
//            Vector3 limbSize = new Vector3(2.0f/16.0f, 6.0f/16.0f, 2.0f/16.0f);
        Vector3f limbSize = toVector3(new Vec(0.5f/16.0f, 6.0f/16.0f, 0.5f/16.0f).mul(PLAYER_SIZE));

        MinecraftPhysicsObject torso = new RagdollPhysics(physicsHandler, player,null, PlayerDisplayPart.TORSO, instance, toVector3(startPos), torsoSize, 1);
        MinecraftPhysicsObject head = new RagdollPhysics(physicsHandler, player, torso.getRigidBody(), PlayerDisplayPart.HEAD, instance, toVector3(startPos.add(new Vec(0, 0.62, 0).mul(PLAYER_SIZE))), headSize, 1);
        MinecraftPhysicsObject rightArm = new RagdollPhysics(physicsHandler, player, torso.getRigidBody(), PlayerDisplayPart.RIGHT_ARM, instance, toVector3(startPos.add(new Vec(0.37, 0, 0).mul(PLAYER_SIZE))), limbSize, 1);
        MinecraftPhysicsObject leftArm = new RagdollPhysics(physicsHandler, player, torso.getRigidBody(), PlayerDisplayPart.LEFT_ARM, instance, toVector3(startPos.add(new Vec(-0.37, 0, 0).mul(PLAYER_SIZE))), limbSize, 1);
        MinecraftPhysicsObject rightLeg = new RagdollPhysics(physicsHandler, player, torso.getRigidBody(), PlayerDisplayPart.RIGHT_LEG, instance, toVector3(startPos.add(new Vec(0.13, -0.72, 0).mul(PLAYER_SIZE))), limbSize, 1);
        MinecraftPhysicsObject leftLeg = new RagdollPhysics(physicsHandler, player, torso.getRigidBody(), PlayerDisplayPart.LEFT_LEG, instance, toVector3(startPos.add(new Vec(-0.13, -0.72, 0).mul(PLAYER_SIZE))), limbSize, 1);
//        Main.paused = true;
    }

    @Override
    public ItemStack getItem() {
        return ItemStack.builder(Material.PLAYER_HEAD)
                .meta(PlayerHeadMeta.class, meta -> {
                    meta.skullOwner(player.getUuid());
                    meta.playerSkin(player.getSkin());
                    meta.displayName(Component.text("Player Spawner", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
                })
                .set(Tool.TOOL_NAME, "playerspawner")
                .build();
    }
}
