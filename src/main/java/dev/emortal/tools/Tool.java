package dev.emortal.tools;

import net.minestom.server.entity.Player;
import net.minestom.server.event.player.*;
import net.minestom.server.item.ItemStack;
import net.minestom.server.tag.Tag;

public abstract class Tool {

    public static final Tag<String> TOOL_NAME_TAG = Tag.String("toolName");

    public Tool(Player player, String toolName) {

        player.eventNode().addListener(PlayerChangeHeldSlotEvent.class, e -> {
            ItemStack item = e.getPlayer().getInventory().getItemStack(e.getOldSlot());
            if (!item.hasTag(TOOL_NAME_TAG)) return;
            String toolNameTag = item.getTag(TOOL_NAME_TAG);
            if (!toolNameTag.equals(toolName)) return;

            boolean shouldCancel = onSlotChange(e.getNewSlot(), e.getOldSlot() - e.getNewSlot());

            e.setCancelled(shouldCancel);
        });
        player.eventNode().addListener(PlayerBlockPlaceEvent.class, e -> {
            ItemStack item = e.getPlayer().getItemInHand(e.getHand());
            if (!item.hasTag(TOOL_NAME_TAG)) return;
            String toolNameTag = item.getTag(TOOL_NAME_TAG);
            if (!toolNameTag.equals(toolName)) return;

            e.setCancelled(true);

            onRightClick();
        });
        player.eventNode().addListener(PlayerUseItemEvent.class, e -> {
            if (!e.getItemStack().hasTag(TOOL_NAME_TAG)) return;
            String toolNameTag = e.getItemStack().getTag(TOOL_NAME_TAG);
            if (!toolNameTag.equals(toolName)) return;

            onRightClick();
        });
        player.eventNode().addListener(PlayerHandAnimationEvent.class, e -> {
            ItemStack item = e.getPlayer().getItemInHand(e.getHand());

            if (!item.hasTag(TOOL_NAME_TAG)) return;
            String toolNameTag = item.getTag(TOOL_NAME_TAG);
            if (!toolNameTag.equals(toolName)) return;

            onLeftClick();
        });
        player.eventNode().addListener(PlayerSwapItemEvent.class, e -> {
            String toolNameTagMain = e.getMainHandItem().getTag(TOOL_NAME_TAG);
            if (toolNameTagMain == null) toolNameTagMain = "";
            String toolNameTagOff = e.getOffHandItem().getTag(TOOL_NAME_TAG);
            if (toolNameTagOff == null) toolNameTagOff = "";
            if (!toolNameTagMain.equals(toolName) && !toolNameTagOff.equals(toolName)) return;

            onSwitchHands();

            e.setCancelled(true);
        });

    }

    abstract void onSwitchHands();
    abstract void onLeftClick();
    abstract void onRightClick();
    boolean onSlotChange(int newSlot, int diff) {
        return false;
    }

    abstract ItemStack getItem();

}
