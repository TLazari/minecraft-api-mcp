package com.example.endpoints;

import io.javalin.Javalin;
import net.minecraft.server.MinecraftServer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class CommandEndpoint extends APIEndpoint {
    public CommandEndpoint(Javalin app, MinecraftServer server, org.slf4j.Logger logger) {
        super(app, server, logger);
        init();
    }

    private void init() {
        app.post("/api/command", ctx -> {
            CommandRequest req = ctx.bodyAsClass(CommandRequest.class);
            String command = req.command();

            if (command.startsWith("/")) {
                command = command.substring(1);
            }

            final String finalCommand = command;
            CompletableFuture<Integer> future = new CompletableFuture<>();

            server.execute(() -> {
                try {
                    server.getCommandManager().executeWithPrefix(
                            server.getCommandSource(),
                            finalCommand);
                    future.complete(1);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });

            try {
                int result = future.get();
                ctx.json(Map.of(
                        "success", true,
                        "command", finalCommand,
                        "resultCode", result));
            } catch (Exception e) {
                LOGGER.error("Error executing command: " + finalCommand, e);
                ctx.status(500).json(Map.of(
                        "success", false,
                        "error", e.getMessage()));
            }
        });
    }
}

record CommandRequest(String command) {
}
