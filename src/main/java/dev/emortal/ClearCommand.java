package dev.emortal;

import net.minestom.server.command.builder.Command;

public class ClearCommand extends Command {
    public ClearCommand(MinecraftPhysicsHandler physicsHandler) {
        super("clear");

        setDefaultExecutor((sender, ctx) -> {
            physicsHandler.objects.iterator().forEachRemaining(MinecraftPhysicsObject::destroy);

            physicsHandler.getPhysicsSpace().getJointList().iterator().forEachRemaining(j -> physicsHandler.getPhysicsSpace().removeJoint(j));

            System.gc();

            sender.sendMessage("Cleared!");
        });
    }
}
