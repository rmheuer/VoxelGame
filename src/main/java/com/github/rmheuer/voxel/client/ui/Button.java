package com.github.rmheuer.voxel.client.ui;

import org.joml.Vector2i;

/**
 * A clickable button with a text label.
 */
public class Button {
    public static final int WIDTH = 200;
    public static final int HEIGHT = 20;

    private String label;
    private boolean enabled;
    private final Runnable onClick;
    private int x, y;
    private int width, height;

    /**
     * @param label label for the button
     * @param onClick function to call when the button is clicked
     */
    public Button(String label, Runnable onClick) {
        this.label = label;
        this.onClick = onClick;
        enabled = true;
        width = WIDTH;
        height = HEIGHT;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Sets the position of the top-left corner on the screen.
     *
     * @param x X coordinate of left side
     * @param y Y coordinate of top side
     */
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    private boolean mouseOver(Vector2i mousePos) {
        return mousePos.x >= x && mousePos.x < x + width
            && mousePos.y >= y && mousePos.y < y + height;
    }

    /**
     * Draws the button into the draw list.
     *
     * @param draw draw list to draw into
     * @param mousePos current mouse position
     */
    public void draw(UIDrawList draw, Vector2i mousePos) {
        UISprite sprite;
        if (enabled) {
            if (mouseOver(mousePos))
                sprite = draw.getSprites().getButtonHighlight();
            else
                sprite = draw.getSprites().getButton();
        } else {
            sprite = draw.getSprites().getButtonGray();
        }

        draw.drawSpriteNineSlice(x, y, width, height, sprite, 3);
//        draw.drawSprite(x, y, sprite);
        draw.drawTextCentered(x + width / 2, y + height / 2 + 3, label);
    }

    protected void clicked() {
        onClick.run();
    }

    /**
     * Handles a mouse press at the specified position.
     *
     * @param mousePos position the mouse was clicked
     */
    public void mouseClicked(Vector2i mousePos) {
        if (enabled && mouseOver(mousePos)) {
            clicked();
        }
    }
}
