package dev.emortal.commands;

import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;

import static dev.emortal.commands.TrustCommand.TRUSTED_PLAYERS;

public class UnTrustCommand extends Command {

    public UnTrustCommand() {
        super("untrust");


        var playerArg = ArgumentType.String("untrust");
        addConditionalSyntax((sender, ctx) -> ((Player) sender).getUsername().equals("emortaldev"), (sender, ctx) -> {
            Player player = MinecraftServer.getConnectionManager().findOnlinePlayer(ctx.get(playerArg));

            if (player == null) return;

            TRUSTED_PLAYERS.remove(player.getUsername());
            player.getInventory().clear();
            player.kick("lol");
        }, playerArg);
    }
}
