package dev.emortal.tools;

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

import static dev.emortal.Main.raycastEntity;

public class DeleteTool extends Tool {

    private final @NotNull Player player;
    private final @NotNull MinecraftPhysicsHandler physicsHandler;
    public DeleteTool(@NotNull Player player, @NotNull MinecraftPhysicsHandler physicsHandler) {
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
        Entity entity = raycastEntity(player.getInstance(), player.getPosition().add(0, player.getEyeHeight(), 0), player.getPosition().direction(), 100000, (ent) -> {
            if (ent == player) return false;
            return true;
        });

        if (entity == null) return;

        MinecraftPhysicsObject obj = physicsHandler.getFromEntity(entity);

        if (obj == null) return;

        player.playSound(Sound.sound(SoundEvent.BLOCK_STONE_BREAK, Sound.Source.MASTER, 0.5f, 0.6f), Sound.Emitter.self());

        obj.destroy();
    }

    @Override
    public ItemStack getItem() {
        return ItemStack.builder(Material.TNT_MINECART)
                .meta(meta -> {
                    meta.displayName(Component.text("Remover", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
                })
                .set(Tool.TOOL_NAME, "remover")
                .build();
    }
}
