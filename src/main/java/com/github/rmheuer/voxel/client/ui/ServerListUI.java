package com.github.rmheuer.voxel.client.ui;

import com.github.rmheuer.azalea.render.Colors;
import com.github.rmheuer.voxel.client.VoxelGame;
import com.github.rmheuer.voxel.client.server_lists.betacraft.BetacraftServerList;
import com.github.rmheuer.voxel.client.server_lists.betacraft.ServerEntry;
import org.joml.Vector2i;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class ServerListUI implements UI {
    private static final int EDGE_COLOR = Colors.RGBA.fromInts(192, 192, 192);
    private static final int UNSUPPORTED_COLOR = Colors.RGBA.fromInts(128, 128, 128);
    private static final int BUTTON_WIDTH = 100;

    private final VoxelGame game;

    private final Button joinButton;
    private final Button refreshButton;
    private final Button cancelButton;

    private CompletableFuture<List<ServerEntry>> entriesFuture;
    private int selectedEntryIndex;
    private int paneBottomY;

    public ServerListUI(VoxelGame game, UI prevUI) {
        this.game = game;

        joinButton = new Button("Join Server", this::join);
        refreshButton = new Button("Refresh", this::refresh);
        cancelButton = new Button("Cancel", () -> game.setUI(prevUI));

        joinButton.setSize(BUTTON_WIDTH, 20);
        refreshButton.setSize(BUTTON_WIDTH, 20);
        cancelButton.setSize(BUTTON_WIDTH, 20);

        refresh();
    }

    private void join() {
        if (selectedEntryIndex < 0)
            return;

        List<ServerEntry> entries = entriesFuture.getNow(null);
        if (entries == null)
            return;

        game.beginMultiPlayer(entries.get(selectedEntryIndex).address);
    }

    private void refresh() {
        // Be completely sure we will never have multiple refreshes in parallel
        if (entriesFuture != null && !entriesFuture.isDone())
            return;

        entriesFuture = new CompletableFuture<>();

        // Real server list getter
        new Thread(() -> {
            try {
                List<ServerEntry> allServers = BetacraftServerList.get();

                // Move offline-mode servers to the top of the list, since
                // those are the ones that are currently supported
                List<ServerEntry> reordered = new ArrayList<>(allServers.size());
                int offlineCount = 0;
                for (ServerEntry server : allServers) {
                    if (server.onlineMode) {
                        reordered.add(server);
                    } else {
                        reordered.add(offlineCount, server);
                        offlineCount++;
                    }
                }

                entriesFuture.complete(reordered);
            } catch (IOException e) {
                entriesFuture.completeExceptionally(e);
            }
        }).start();

        // Test server list getter
//        new Thread(() -> {
//            try {
//                Thread.sleep(500);
//            } catch (InterruptedException ignored) {}
//
//            entriesFuture.complete(Arrays.asList(
//                    new ServerEntry("Server Entry 1", 1, 1, "localhost"),
//                    new ServerEntry("Server Entry 2", 111, 128, "localhost"),
//                    new ServerEntry("Server Entry 3", -3, 0, "localhost")
//            ));
//        }).start();

        setSelected(-1);
        refreshButton.setEnabled(false);
    }

    @Override
    public void draw(UIDrawList draw, Vector2i mousePos) {
        if (entriesFuture.isDone())
            refreshButton.setEnabled(true);

        draw.drawDirtBackground(0, 0, draw.getWidth(), draw.getHeight());

        draw.drawTextCentered(draw.getWidth() / 2, 11, "Betacraft Server List");
        draw.drawRect(0, 15, draw.getWidth(), 1, EDGE_COLOR);
        draw.drawRect(0, draw.getHeight() - 28, draw.getWidth(), 1, EDGE_COLOR);
        paneBottomY = draw.getHeight() - 28;

        joinButton.setPosition(draw.getWidth() / 2 - BUTTON_WIDTH / 2 - BUTTON_WIDTH - 4, draw.getHeight() - 23);
        refreshButton.setPosition(draw.getWidth() / 2 - BUTTON_WIDTH / 2, draw.getHeight() - 23);
        cancelButton.setPosition(draw.getWidth() / 2 + BUTTON_WIDTH / 2 + 4, draw.getHeight() - 23);

        joinButton.draw(draw, mousePos);
        refreshButton.draw(draw, mousePos);
        cancelButton.draw(draw, mousePos);

        draw.setClipRect(0, 16, draw.getWidth(), draw.getHeight() - 28 - 16);
        draw.drawRect(0, 16, draw.getWidth(), draw.getHeight() - 28 - 16, Colors.RGBA.fromInts(0, 0, 0, 128));

        if (entriesFuture.isCompletedExceptionally()) {
            draw.drawTextCentered(draw.getWidth() / 2, draw.getHeight() / 2 + 3, "Failed to retrieve server list :(");
            return;
        }

        List<ServerEntry> entries = entriesFuture.getNow(null);
        if (entries == null) {
            draw.drawTextCentered(draw.getWidth() / 2, draw.getHeight() / 2 + 3, "Retrieving server list...");
            return;
        }

        int y = 20;
        for (int i = 0; i < entries.size(); i++) {
            ServerEntry entry = entries.get(i);
            int color = entry.onlineMode ? UNSUPPORTED_COLOR : Colors.RGBA.WHITE;
            if (i == selectedEntryIndex)
                draw.drawRectOutline(4, y, 312, 26, color);

            draw.drawTextColored(6, y + 12, entry.name, color);
            draw.drawTextColored(6, y + 22, entry.playerCount + " / " + entry.maxPlayers + " players online", color);

            y += 26 + 4;
        }

        int gradColor = Colors.RGBA.fromInts(0, 0, 0, 192);
        draw.drawRectVGradient(0, 16, draw.getWidth(), 4, gradColor, Colors.RGBA.TRANSPARENT);
        draw.drawRectVGradient(0, draw.getHeight() - 28 - 4, draw.getWidth(), 4, Colors.RGBA.TRANSPARENT, gradColor);
    }

    private void setSelected(int index) {
        selectedEntryIndex = index;
        joinButton.setEnabled(index >= 0);
    }

    @Override
    public void mouseClicked(Vector2i mousePos) {
        joinButton.mouseClicked(mousePos);
        refreshButton.mouseClicked(mousePos);
        cancelButton.mouseClicked(mousePos);

        if (mousePos.y >= 16 && mousePos.y < paneBottomY) {
            List<ServerEntry> entries = entriesFuture.getNow(null);
            if (entries != null) {
                int clickedEntryIndex = (mousePos.y - 18) / 30;
                if (clickedEntryIndex >= 0 && clickedEntryIndex < entries.size())
                    setSelected(clickedEntryIndex);
                else
                    setSelected(-1);
            }
        }
    }

    @Override
    public boolean shouldPauseGame() {
        return false;
    }
}
