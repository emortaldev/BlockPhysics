package dev.emortal.tools;

import dev.emortal.MinecraftPhysics;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;

public class ChainTool extends Tool {

    private final @NotNull Player player;
    private final @NotNull MinecraftPhysics physicsHandler;
    public ChainTool(@NotNull Player player, @NotNull MinecraftPhysics physicsHandler) {
        super(player, "chain");
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
        player.playSound(Sound.sound(SoundEvent.BLOCK_CHAIN_PLACE, Sound.Source.MASTER, 1f, 1f), Sound.Emitter.self());

        double holdDistance = 4;
        Vec spawnPos = player.getPosition().add(0, player.getEyeHeight(), 0).add(player.getPosition().direction().mul(holdDistance)).asVec();

//        MinecraftPhysicsObject lastLink = null;
//        for (int links = 0; links < 10; links++) {
//            PhysicsRigidBody parent = null;
//            if (lastLink != null) parent = (PhysicsRigidBody) lastLink.getCollisionObject();
//            lastLink = new ChainPhysics(physicsHandler, parent, new Vec(0.1f, 0.5f, 0.1f), 1, toVec3(spawnPos.add(0, (10 - links) * 1.04, 0)));
//            lastLink.setInstance();
//        }
    }

    @Override
    public ItemStack getItem() {
        return ItemStack.builder(Material.DIAMOND_BLOCK)
                .customName(Component.text("Chain Spawner", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false))
                .set(Tool.TOOL_NAME_TAG, "chain")
                .build();
    }
}
