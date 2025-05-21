package com.github.rmheuer.voxel.client.ui;

import com.github.rmheuer.azalea.render.Colors;
import com.github.rmheuer.voxel.client.server_lists.betacraft.BetacraftServerList;
import com.github.rmheuer.voxel.client.server_lists.betacraft.ServerEntry;
import org.joml.Vector2i;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class ServerListUI implements UI {
    private static final int EDGE_COLOR = Colors.RGBA.fromInts(192, 192, 192);

    private final Button joinButton;
    private final Button cancelButton;

    private final CompletableFuture<List<ServerEntry>> entriesFuture;

    public ServerListUI() {
        joinButton = new Button("Join Server", () -> {});
        cancelButton = new Button("Cancel", () -> {});

        entriesFuture = new CompletableFuture<>();
        new Thread(() -> {
            try {
                entriesFuture.complete(BetacraftServerList.get());
            } catch (IOException e) {
                entriesFuture.completeExceptionally(e);
            }
        }).start();
    }

    @Override
    public void draw(UIDrawList draw, Vector2i mousePos) {
        draw.drawDirtBackground(0, 0, draw.getWidth(), draw.getHeight());

        draw.drawTextCentered(draw.getWidth() / 2, 11, "Betacraft Server List");
        draw.drawRect(0, 15, draw.getWidth(), 1, EDGE_COLOR);
        draw.drawRect(0, draw.getHeight() - 28, draw.getWidth(), 1, EDGE_COLOR);

        draw.setClipRect(0, 16, draw.getWidth(), draw.getHeight() - 28 - 16);
        draw.drawRect(0, 16, draw.getWidth(), draw.getHeight() - 28 - 16, Colors.RGBA.fromInts(0, 0, 0, 128));

        if (entriesFuture.isCompletedExceptionally()) {
            draw.drawText(10, 20, "Failed to retrieve server list :(");
            return;
        }

        List<ServerEntry> entries = entriesFuture.getNow(null);
        if (entries == null) {
            draw.drawText(10, 20, "Retrieving server list...");
            return;
        }

        int y = 20;
        for (ServerEntry entry : entries) {
//            draw.drawRectOutline(4, y, 312, 26, Colors.RGBA.WHITE);
            draw.drawText(6, y + 12, entry.name);
            draw.drawText(6, y + 22, entry.playerCount + " / " + entry.maxPlayers + " players online");

            y += 26 + 4;
        }

        int gradColor = Colors.RGBA.fromInts(0, 0, 0, 192);
        draw.drawRectVGradient(0, 16, draw.getWidth(), 4, gradColor, Colors.RGBA.TRANSPARENT);
        draw.drawRectVGradient(0, draw.getHeight() - 28 - 4, draw.getWidth(), 4, Colors.RGBA.TRANSPARENT, gradColor);
    }

    @Override
    public void mouseClicked(Vector2i mousePos) {

    }

    @Override
    public boolean shouldPauseGame() {
        return false;
    }
}
