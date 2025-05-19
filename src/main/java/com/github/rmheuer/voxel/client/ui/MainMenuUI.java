package com.github.rmheuer.voxel.client.ui;

import com.github.rmheuer.azalea.render.Colors;
import org.joml.Vector2i;

public final class MainMenuUI implements UI {
    /*

    [ Singleplayer ]

    Server Address
    [ Address text input box ]
    [ Join Server ]

     */

    private final Button singlePlayerButton;
    private final TextInputBox serverAddressInput;
    private final Button joinServerButton;

    public MainMenuUI() {
        singlePlayerButton = new Button("Singleplayer", () -> {});
        serverAddressInput = new TextInputBox();
        joinServerButton = new Button("Join Server", () -> {});

        singlePlayerButton.setEnabled(false);
    }

    @Override
    public void draw(UIDrawList draw, UISprites sprites, Vector2i mousePos) {
        int centerX = draw.getWidth() / 2;
        int centerY = draw.getHeight() / 2;
        int cornerX = centerX - Button.WIDTH / 2;
        int cornerY = centerY - (5 * Button.HEIGHT + 4 * 4) / 2;

        singlePlayerButton.setPosition(cornerX, cornerY);
        serverAddressInput.setPosition(cornerX, cornerY + Button.HEIGHT * 3 + 12);
        joinServerButton.setPosition(cornerX, cornerY + Button.HEIGHT * 4 + 16);

        draw.drawDirtBackground(sprites, 0, 0, draw.getWidth(), draw.getHeight());
        singlePlayerButton.draw(draw, sprites, mousePos);
        draw.drawText(cornerX + 1, cornerY + Button.HEIGHT * 3 + 8, "Enter server address:");
        serverAddressInput.draw(draw);
        joinServerButton.draw(draw, sprites, mousePos);
    }

    @Override
    public void mouseClicked(Vector2i mousePos) {
        singlePlayerButton.mouseClicked(mousePos);
        joinServerButton.mouseClicked(mousePos);
    }

    @Override
    public boolean shouldPauseGame() {
        return false;
    }
}
