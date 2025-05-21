package com.github.rmheuer.voxel.client.ui;

import com.github.rmheuer.azalea.input.keyboard.Key;
import com.github.rmheuer.azalea.render.Colors;
import com.github.rmheuer.voxel.client.ServerAddress;
import com.github.rmheuer.voxel.client.VoxelGame;
import org.joml.Vector2i;

public final class MainMenuUI implements UI {
    private static final int LABEL_COLOR = Colors.RGBA.fromInts(192, 192, 192);

    private final VoxelGame game;

    private final Button singlePlayerButton;
    private final TextInputBox usernameInput;
    private final Button serverListButton;
    private final TextInputBox serverAddressInput;
    private final Button joinServerButton;

    /*
     * [Singleplayer]
     *
     * Enter username:
     * [username text box]
     *
     * [Server List]
     *
     * Enter direct connect address:
     * [address box]
     * [Join Server]
     */

    public MainMenuUI(VoxelGame game, String initialUsername) {
        this.game = game;

//        singlePlayerButton = new Button("Singleplayer", () -> game.setUI(new CreateLevelUI(game, this, false)));
        singlePlayerButton = new Button("Singleplayer", game::beginSinglePlayer);
        usernameInput = new TextInputBox(initialUsername);
        serverListButton = new Button("Server List", () -> game.setUI(new ServerListUI(game, this)));
        serverAddressInput = new TextInputBox();
        joinServerButton = new Button("Join Server", () -> doJoinServer(serverAddressInput.getInput()));

        joinServerButton.setEnabled(false);
        usernameInput.setOnChange((username) -> joinServerButton.setEnabled(areServerParametersValid()));
        serverAddressInput.setOnChange((address) -> joinServerButton.setEnabled(areServerParametersValid()));
        serverAddressInput.setOnConfirm(this::doJoinServer);
    }

    private boolean areServerParametersValid() {
        if (usernameInput.getInput().isEmpty())
            return false;

        return ServerAddress.isValid(serverAddressInput.getInput());
    }

    private void doJoinServer(String address) {
        if (usernameInput.getInput().isEmpty())
            return;

        game.beginMultiPlayer(address, usernameInput.getInput());
    }

    @Override
    public void draw(UIDrawList draw, Vector2i mousePos) {
        int centerX = draw.getWidth() / 2;
        int centerY = draw.getHeight() / 2;
        int cornerX = centerX - Button.WIDTH / 2;
        int cornerY = centerY - (7 * Button.HEIGHT + 5 * 4 + 16) / 2;

        // Maybe sometime I'll add a better layout system than making up numbers
        singlePlayerButton.setPosition(cornerX, cornerY);
        usernameInput.setPosition(cornerX, cornerY + Button.HEIGHT * 2 + 12);
        serverListButton.setPosition(cornerX, cornerY + Button.HEIGHT * 4 + 4 + 4);
        serverAddressInput.setPosition(cornerX, cornerY + Button.HEIGHT * 5 + 16 + 16 + 4);
        joinServerButton.setPosition(cornerX, cornerY + Button.HEIGHT * 6 + 20 + 16 + 4);

        draw.drawDirtBackground(0, 0, draw.getWidth(), draw.getHeight());
        singlePlayerButton.draw(draw, mousePos);
        draw.drawTextColored(cornerX + 1, cornerY + Button.HEIGHT * 2 + 8, "Enter username:", LABEL_COLOR);
        usernameInput.draw(draw);
        serverListButton.draw(draw, mousePos);
        draw.drawTextColored(cornerX + 1, cornerY + Button.HEIGHT * 5 + 16 + 16, "Enter direct connect address:", LABEL_COLOR);
        serverAddressInput.draw(draw);
        joinServerButton.draw(draw, mousePos);
    }

    @Override
    public void mouseClicked(Vector2i mousePos) {
        singlePlayerButton.mouseClicked(mousePos);
        usernameInput.mouseClicked(mousePos);
        serverAddressInput.mouseClicked(mousePos);
        serverListButton.mouseClicked(mousePos);
        joinServerButton.mouseClicked(mousePos);
    }

    @Override
    public boolean keyPressed(Key key) {
        return serverAddressInput.keyPressed(key)
                || usernameInput.keyPressed(key);
    }

    @Override
    public void charTyped(char c) {
        serverAddressInput.charTyped(c);
        usernameInput.charTyped(c);
    }

    @Override
    public boolean shouldPauseGame() {
        return false;
    }
}
