package dev.emortal.commands;

import com.jme3.bullet.objects.PhysicsRigidBody;
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

        int awakeRigid = 0;
        for (PhysicsRigidBody rigidBody : physicsHandler.getPhysicsSpace().getRigidBodyList()) {
            if (rigidBody.isActive()) awakeRigid++;
        }

        sender.sendMessage(Component.text()
                .append(Component.newline())

                // RAM usage information
                .append(Component.text("RAM Usage: ", NamedTextColor.GRAY))
                .append(Component.text(String.format("%sMB / %sMB", ramUsage, totalMem), NamedTextColor.GRAY))
                .append(Component.newline())

                // Physics Information
                .append(Component.text("Rigid Bodies: ", NamedTextColor.GRAY))
                .append(Component.text(physicsHandler.getPhysicsSpace().countRigidBodies(), NamedTextColor.GOLD))
                .append(Component.text(" (Awake: ", NamedTextColor.GRAY))
                .append(Component.text(awakeRigid, NamedTextColor.WHITE))
                .append(Component.text(")", NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.text("Joints: ", NamedTextColor.GRAY))
                .append(Component.text(physicsHandler.getPhysicsSpace().countJoints(), NamedTextColor.GOLD)));
    }
}