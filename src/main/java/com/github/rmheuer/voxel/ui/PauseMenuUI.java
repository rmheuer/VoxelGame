package com.github.rmheuer.voxel.ui;

import com.github.rmheuer.azalea.render.Colors;

import org.joml.Vector2i;

public final class PauseMenuUI {
    private static final int BG_COLOR_1 = Colors.RGBA.fromInts(5, 5, 0, 96);
    private static final int BG_COLOR_2 = Colors.RGBA.fromInts(48, 48, 96, 160);
    
    public void draw(UIDrawList draw, UISprites sprites, Vector2i mousePos) {
        int cornerX = 10;
        int cornerY = 10;

        boolean hover = mousePos.x >= cornerX && mousePos.x < cornerX + 200 && mousePos.y >= cornerY && mousePos.y < cornerY + 20;
        UISprite sprite = hover ? sprites.getButtonHighlight() : sprites.getButton();

        draw.drawRectVGradient(0, 0, draw.getWidth(), draw.getHeight(), BG_COLOR_1, BG_COLOR_2);
        draw.drawSprite(cornerX, cornerY, sprite);
    }
}
