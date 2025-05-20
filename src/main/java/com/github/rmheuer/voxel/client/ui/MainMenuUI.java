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
    private final TextInputBox serverAddressInput;
    private final TextInputBox usernameInput;
    private final Button joinServerButton;

    public MainMenuUI(VoxelGame game, String initialUsername) {
        this.game = game;

//        singlePlayerButton = new Button("Singleplayer", () -> game.setUI(new CreateLevelUI(game, this, false)));
        singlePlayerButton = new Button("Singleplayer", game::beginSinglePlayer);
        serverAddressInput = new TextInputBox();
        usernameInput = new TextInputBox(initialUsername);
        joinServerButton = new Button("Join Server", () -> doJoinServer(serverAddressInput.getInput()));

        joinServerButton.setEnabled(false);
        usernameInput.setOnChange((username) -> joinServerButton.setEnabled(areServerParametersValid()));
        serverAddressInput.setOnChange((address) -> joinServerButton.setEnabled(areServerParametersValid()));
        serverAddressInput.setOnConfirm(this::doJoinServer);
    }

    private boolean areServerParametersValid() {
        if (usernameInput.getInput().isEmpty())
            return false;

        try {
            return parseAddress(serverAddressInput.getInput()) != null;
        } catch (Exception e) {
            return false;
        }
    }

    private ServerAddress parseAddress(String address) {
        if (address.isEmpty())
            return null;

        int lastColon = address.lastIndexOf(':');
        if (lastColon < 0)
            return new ServerAddress(address, 25565);

        String host;
        if (address.charAt(0) == '[') {
            int close = address.indexOf(']');
            if (close < 0)
                return null;

            if (lastColon != close + 1)
                return null;

            host = address.substring(1, close);
        } else {
            int firstColon = address.indexOf(':');
            if (firstColon != lastColon) {
                // Probably an IPv6 address
                return new ServerAddress(address, 25565);
            }

            host = address.substring(0, lastColon);
        }

        int port = Integer.parseInt(address.substring(lastColon + 1));
        if (port < 0 || port > 65535)
            return null;

        return new ServerAddress(host, port);
    }

    private void doJoinServer(String address) {
        if (usernameInput.getInput().isEmpty())
            return;

        ServerAddress addr;
        try {
            addr = parseAddress(address);
            if (addr == null)
                return;
        } catch (Exception e) {
            return;
        }

        game.beginMultiPlayer(addr, usernameInput.getInput());
    }

    @Override
    public void draw(UIDrawList draw, Vector2i mousePos) {
        int centerX = draw.getWidth() / 2;
        int centerY = draw.getHeight() / 2;
        int cornerX = centerX - Button.WIDTH / 2;
        int cornerY = centerY - (6 * Button.HEIGHT + 4 * 4 + 16) / 2;

        singlePlayerButton.setPosition(cornerX, cornerY);
        serverAddressInput.setPosition(cornerX, cornerY + Button.HEIGHT * 3 + 12);
        usernameInput.setPosition(cornerX, cornerY + Button.HEIGHT * 4 + 16 + 16);
        joinServerButton.setPosition(cornerX, cornerY + Button.HEIGHT * 5 + 20 + 16);

        draw.drawDirtBackground(0, 0, draw.getWidth(), draw.getHeight());
        singlePlayerButton.draw(draw, mousePos);
        draw.drawTextColored(cornerX + 1, cornerY + Button.HEIGHT * 3 + 8, "Enter server address:", LABEL_COLOR);
        serverAddressInput.draw(draw);
        draw.drawTextColored(cornerX + 1, cornerY + Button.HEIGHT * 4 + 12 + 16, "Enter username:", LABEL_COLOR);
        usernameInput.draw(draw);
        joinServerButton.draw(draw, mousePos);
    }

    @Override
    public void mouseClicked(Vector2i mousePos) {
        singlePlayerButton.mouseClicked(mousePos);
        serverAddressInput.mouseClicked(mousePos);
        usernameInput.mouseClicked(mousePos);
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
