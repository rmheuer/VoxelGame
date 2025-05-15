package com.github.rmheuer.voxel.client.ui;

import org.joml.Vector2i;

public final class ConnectingToServerUI implements UI {
    public enum State {
        CONNECTING("Connecting to server..."),
        LOGGING_IN("Logging in...");

        public final String message;

        State(String message) {
            this.message = message;
        }
    }

    private State state;

    public ConnectingToServerUI() {
        state = State.CONNECTING;
    }

    @Override
    public void draw(UIDrawList draw, UISprites sprites, Vector2i mousePos) {
        int width = draw.getWidth();
        int height = draw.getHeight();

        draw.drawDirtBackground(sprites, 0, 0, width, height);
        draw.drawTextCentered(width / 2, height / 2 - 12, state.message);

//        draw.drawRect(draw.getWidth() / 2 - 50, draw.getHeight() / 2 + 12, 100, 2, Colors.RGBA.fromInts(128, 255, 128));
    }

    @Override
    public void mouseClicked(Vector2i mousePos) {

    }

    @Override
    public boolean shouldPauseGame() {
        return false;
    }

    public void setState(State state) {
        this.state = state;
    }
}
