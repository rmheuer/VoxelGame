package com.github.rmheuer.voxel.client.ui;

import com.github.rmheuer.azalea.render.Colors;
import org.joml.Vector2i;

public final class DownloadingTerrainUI implements UI {
    private static final int BAR_BG = Colors.RGBA.fromInts(128, 128, 128);
    private static final int BAR_COLOR = Colors.RGBA.fromInts(128, 255, 128);

    private int percentReceived;

    public DownloadingTerrainUI() {
        percentReceived = 0;
    }

    @Override
    public void draw(UIDrawList draw, UISprites sprites, Vector2i mousePos) {
        int width = draw.getWidth();
        int height = draw.getHeight();

        draw.drawDirtBackground(sprites, 0, 0, width, height);
        draw.drawTextCentered(width / 2, height / 2 - 12, "Downloading terrain...");

        draw.drawRect(width/2 - 50, height/2 + 12, 100, 2, BAR_BG);
        draw.drawRect(width/2 - 50, height/2 + 12, percentReceived, 2, BAR_COLOR);
    }

    @Override
    public void mouseClicked(Vector2i mousePos) {

    }

    @Override
    public boolean shouldPauseGame() {
        return false;
    }

    public void setPercentReceived(int percentReceived) {
        this.percentReceived = percentReceived;
    }
}
