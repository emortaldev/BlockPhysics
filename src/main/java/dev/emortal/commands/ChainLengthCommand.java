package dev.emortal.commands;

import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;

public class ChainLengthCommand extends Command {
    public static int CHAIN_LENGTH = 10;

    public ChainLengthCommand() {
        super("chainlength");

        var intArg = ArgumentType.Integer("chainLength").between(1, 100);
        addSyntax((sender, ctx) -> {
            CHAIN_LENGTH = ctx.get(intArg);
        }, intArg);
    }
}
