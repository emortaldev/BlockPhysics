package dev.emortal.commands;

import net.kyori.adventure.text.Component;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;

public class PlayerSizeCommand extends Command {
    public static double PLAYER_SIZE = 1.0;

    public PlayerSizeCommand() {
        super("playersize");

        var doubleArg = ArgumentType.Double("playerSize").between(0.2, 10.0);
        addSyntax((sender, ctx) -> {
            PLAYER_SIZE = ctx.get(doubleArg);

            sender.sendMessage(Component.text("Set player size to: " + PLAYER_SIZE));
        }, doubleArg);
    }
}
