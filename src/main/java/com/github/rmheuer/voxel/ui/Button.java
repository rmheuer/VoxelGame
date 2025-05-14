package com.github.rmheuer.voxel.ui;

import org.joml.Vector2i;

/**
 * A clickable button with a text label.
 */
public class Button {
    public static final int WIDTH = 200;
    public static final int HEIGHT = 20;

    private String label;
    private final Runnable onClick;
    private int x, y;

    /**
     * @param label label for the button
     * @param onClick function to call when the button is clicked
     */
    public Button(String label, Runnable onClick) {
        this.label = label;
        this.onClick = onClick;
    }

    public void setLabel(String label) {
        this.label = label;
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

    private boolean mouseOver(Vector2i mousePos) {
        return mousePos.x >= x && mousePos.x < x + WIDTH
            && mousePos.y >= y && mousePos.y < y + HEIGHT;
    }

    /**
     * Draws the button into the draw list.
     *
     * @param draw draw list to draw into
     * @param sprites access to UI sprites
     * @param mousePos current mouse position
     */
    public void draw(UIDrawList draw, UISprites sprites, Vector2i mousePos) {
        UISprite sprite = mouseOver(mousePos)
            ? sprites.getButtonHighlight()
            : sprites.getButton();

        draw.drawSprite(x, y, sprite);
        draw.drawTextCentered(x + WIDTH / 2, y + HEIGHT / 2 + 3, label);
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
        if (mouseOver(mousePos)) {
            clicked();
        }
    }
}
