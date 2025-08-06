package dev.emortal.commands;

import dev.emortal.Main;
import dev.emortal.MinecraftPhysics;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClearCommand extends Command {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClearCommand.class);

    public ClearCommand(MinecraftPhysics physics) {
        super("clear");

        setDefaultExecutor((sender, ctx) -> {
            if (!(sender instanceof Player player)) return;

            for (Entity entity : player.getInstance().getEntities()) {
                if (entity instanceof Player) continue;
                entity.remove();
            }

            physics.clear();

            Instance instance = physics.getInstance();

            // Readd chunk meshes
            Main.CHUNK_MESH_SETTINGS.clear();
            Main.CHUNK_MESH_MAP.clear();
            int chunkLoadRadius = 3;
            for (int x = -chunkLoadRadius; x < chunkLoadRadius; x++) {
                for (int z = -chunkLoadRadius; z < chunkLoadRadius; z++) {
                    instance.loadChunk(x, z).thenAccept(c -> {
                        Main.refreshChunk(physics, c);
                    });
                }
            }

            physics.addFloorPlane();

            System.gc();

            sender.sendMessage("Cleared!");
        });
    }
}
