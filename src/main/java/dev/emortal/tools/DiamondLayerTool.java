package dev.emortal.tools;

import com.github.stephengold.joltjni.RVec3;
import dev.emortal.MinecraftPhysics;
import dev.emortal.objects.BlockRigidBody;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;

public class DiamondLayerTool extends Tool {

    private final @NotNull Player player;
    private final @NotNull MinecraftPhysics physicsHandler;
    public DiamondLayerTool(@NotNull Player player, @NotNull MinecraftPhysics physicsHandler) {
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

//        BlockRigidBody rigidBody = new BlockRigidBody(physicsHandler, new RVec3(0, 20, 0), new Vec(0.5), true, Block.DIAMOND_BLOCK);
//        rigidBody.setInstance();

        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                BlockRigidBody rigidBody = new BlockRigidBody(physicsHandler, new RVec3(x, 20, z), new Vec(0.5), true, Block.DIAMOND_BLOCK);
                rigidBody.setInstance();
            }
        }
    }

    @Override
    public ItemStack getItem() {
        return ItemStack.builder(Material.DIAMOND_BLOCK)
                .customName(Component.text("Diamond Spawner", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false))
                .set(Tool.TOOL_NAME_TAG, "diamond")
                .build();
    }
}
