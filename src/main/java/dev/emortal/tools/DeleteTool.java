package dev.emortal.tools;

import dev.emortal.MinecraftPhysics;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

public class DeleteTool extends Tool {

    private final @NotNull Player player;
    private final @NotNull MinecraftPhysics physicsHandler;
    public DeleteTool(@NotNull Player player, @NotNull MinecraftPhysics physicsHandler) {
        super(player, "remover");
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
//        List<PhysicsRayTestResult> results = physicsHandler.raycastEntity(player.getPosition().add(0, player.getEyeHeight(), 0), player.getPosition().direction(), 1000);
//        if (results.isEmpty()) return;
//
//        PhysicsCollisionObject obj = results.getFirst().getCollisionObject();
//
//        if (obj == null) return;
//
//        MinecraftPhysicsObject mcObj = physicsHandler.getObjectByBody(obj);
//        if (mcObj != null) {
//            player.playSound(Sound.sound(SoundEvent.BLOCK_STONE_BREAK, Sound.Source.MASTER, 0.5f, 0.6f), Sound.Emitter.self());
//            mcObj.destroy();
//        }
    }

    @Override
    public ItemStack getItem() {
        return ItemStack.builder(Material.TNT_MINECART)
                .customName(Component.text("Remover", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false))
                .set(Tool.TOOL_NAME_TAG, "remover")
                .build();
    }
}
