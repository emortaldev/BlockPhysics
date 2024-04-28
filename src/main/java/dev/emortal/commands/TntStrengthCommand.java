package dev.emortal.commands;

import net.kyori.adventure.text.Component;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;

public class TntStrengthCommand extends Command {
    public static double TNT_STRENGTH = 1.0;

    public TntStrengthCommand() {
        super("tntstrength");

        var doubleArg = ArgumentType.Double("tntStrength").between(0.2, 50.0);
        addSyntax((sender, ctx) -> {
            TNT_STRENGTH = ctx.get(doubleArg);

            sender.sendMessage(Component.text("Set tnt strength to: " + TNT_STRENGTH));
        }, doubleArg);
    }
}
