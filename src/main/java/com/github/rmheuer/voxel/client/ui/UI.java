package com.github.rmheuer.voxel.client.ui;

import com.github.rmheuer.azalea.input.keyboard.Key;
import org.joml.Vector2i;

/**
 * A UI screen that can be shown to the player.
 */
public interface UI {
    /**
     * Draws the UI into a draw list.
     *
     * @param draw draw list to draw into
     * @param mousePos current position of mouse
     */
    void draw(UIDrawList draw, Vector2i mousePos);

    /**
     * Handles when the mouse is clicked.
     *
     * @param mousePos position that was clicked
     */
    void mouseClicked(Vector2i mousePos);

    default boolean keyPressed(Key key) {
        return false;
    }

    default void charTyped(char c) {
    }

    /**
     * @return whether the game should be paused while this UI is open
     */
    boolean shouldPauseGame();
}
