package dev.emortal.tools;

import com.jme3.math.Vector3f;
import dev.emortal.MinecraftPhysicsHandler;
import dev.emortal.objects.BlockRigidBody;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;

public class DiamondLayerTool extends Tool {

    private final @NotNull Player player;
    private final @NotNull MinecraftPhysicsHandler physicsHandler;
    public DiamondLayerTool(@NotNull Player player, @NotNull MinecraftPhysicsHandler physicsHandler) {
        super(player, "diamond");
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

        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                new BlockRigidBody(physicsHandler, instance, new Vector3f(x, 20, z), new Vector3f(0.5f, 0.5f, 0.5f), 1, true, Block.DIAMOND_BLOCK);
            }
        }

    }

    @Override
    public ItemStack getItem() {
        return ItemStack.builder(Material.DIAMOND_BLOCK)
                .meta(meta -> {
                    meta.displayName(Component.text("Diamond Spawner", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
                })
                .set(Tool.TOOL_NAME, "diamond")
                .build();
    }
}
