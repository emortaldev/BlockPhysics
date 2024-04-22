package dev.emortal.commands;

import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;

public class PlayerSizeCommand extends Command {
    public static double PLAYER_SIZE = 1.0;

    public PlayerSizeCommand() {
        super("playersize");

        var intArg = ArgumentType.Double("playerSize").between(0.2, 10.0);
        addSyntax((sender, ctx) -> {
            PLAYER_SIZE = ctx.get(intArg);
        }, intArg);
    }
}
