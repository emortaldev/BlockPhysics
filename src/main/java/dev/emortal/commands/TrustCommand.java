package dev.emortal.commands;

import dev.emortal.MinecraftPhysicsHandler;
import dev.emortal.tools.*;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class TrustCommand extends Command {

    public static final List<String> TRUSTED_PLAYERS = new ArrayList<>();

    public TrustCommand(MinecraftPhysicsHandler physicsHandler) {
        super("trust");

        TRUSTED_PLAYERS.add("emortaldev");

        var playerArg = ArgumentType.String("trust");
        addConditionalSyntax((sender, ctx) -> ((Player) sender).getUsername().equals("emortaldev"), (sender, ctx) -> {
            Player player = MinecraftServer.getConnectionManager().findOnlinePlayer(ctx.get(playerArg));

            if (player == null) return;

            TRUSTED_PLAYERS.add(player.getUsername());
            player.getInventory().setItemStack(9, new DiamondLayerTool(player, physicsHandler).getItem());
            player.getInventory().setItemStack(7, new DeleteTool(player, physicsHandler).getItem());
            player.getInventory().setItemStack(6, new PlayerSpawnerTool(player, physicsHandler).getItem());
            player.getInventory().setItemStack(5, new GrabberTool(player, physicsHandler).getItem());
            player.getInventory().setItemStack(4, new WeldTool(player, physicsHandler).getItem());
        }, playerArg);
    }
}
