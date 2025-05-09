package com.github.rmheuer.voxel.ui;

import org.joml.Vector2i;

public final class Button {
    public static final int WIDTH = 200;
    public static final int HEIGHT = 20;

    private final String label;
    private final Runnable onClick;
    private int x, y;

    public Button(String label, Runnable onClick) {
        this.label = label;
        this.onClick = onClick;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    private boolean mouseOver(Vector2i mousePos) {
        return mousePos.x >= x && mousePos.x < x + WIDTH
            && mousePos.y >= y && mousePos.y < y + HEIGHT;
    }

    public void draw(UIDrawList draw, UISprites sprites, Vector2i mousePos) {
        UISprite sprite = mouseOver(mousePos)
            ? sprites.getButtonHighlight()
            : sprites.getButton();

        draw.drawSprite(x, y, sprite);
        draw.drawTextCentered(x + WIDTH / 2, y + HEIGHT / 2 + 3, label);
    }

    public void mouseClicked(Vector2i mousePos) {
        if (mouseOver(mousePos)) {
            onClick.run();
        }
    }
}
