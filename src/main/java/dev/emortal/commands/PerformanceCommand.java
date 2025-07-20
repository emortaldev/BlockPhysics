package dev.emortal.commands;

import com.github.stephengold.joltjni.enumerate.EBodyType;
import dev.emortal.MinecraftPhysics;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import org.jetbrains.annotations.NotNull;

public final class PerformanceCommand extends Command {

    private final MinecraftPhysics physicsHandler;
    public PerformanceCommand(@NotNull EventNode<Event> eventNode, MinecraftPhysics physicsHandler) {
        super("performance", "stats");
        this.physicsHandler = physicsHandler;
        this.addSyntax(this::onExecute);
    }

    private void onExecute(@NotNull CommandSender sender, @NotNull CommandContext context) {
        long totalMem = Runtime.getRuntime().totalMemory() / 1024 / 1024;
        long freeMem = Runtime.getRuntime().freeMemory() / 1024 / 1024;
        long ramUsage = totalMem - freeMem;

        int numBodies = physicsHandler.getPhysicsSystem().getNumBodies();
        int numActiveBodies = physicsHandler.getPhysicsSystem().getNumActiveBodies(EBodyType.RigidBody);
        int numConstraints = physicsHandler.getPhysicsSystem().getConstraints().size();

        sender.sendMessage(Component.text()
                .appendNewline()

                // RAM usage information
                .append(Component.text("RAM Usage: ", NamedTextColor.GRAY))
                .append(Component.text(String.format("%sMB / %sMB", ramUsage, totalMem), NamedTextColor.GRAY))
                .appendNewline()

                // Physics Information
                .append(Component.text("Physics Bodies: ", NamedTextColor.GRAY))
                .append(Component.text(numBodies, NamedTextColor.GOLD))
                .append(Component.text(" (Active: ", NamedTextColor.GRAY))
                .append(Component.text(numActiveBodies, NamedTextColor.WHITE))
                .append(Component.text(")", NamedTextColor.GRAY))
                .appendNewline()
                .append(Component.text("Constraints: ", NamedTextColor.GRAY))
                .append(Component.text(numConstraints, NamedTextColor.GOLD)));
    }
}