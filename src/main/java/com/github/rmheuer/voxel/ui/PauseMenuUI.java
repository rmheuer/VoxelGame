package com.github.rmheuer.voxel.ui;

import com.github.rmheuer.azalea.render.Colors;
import com.github.rmheuer.voxel.VoxelGame;

import org.joml.Vector2i;

public final class PauseMenuUI implements UI {
    private static final String TITLE = "Game Paused";

    private static final int BG_COLOR_1 = Colors.RGBA.fromInts(5, 5, 0, 96);
    private static final int BG_COLOR_2 = Colors.RGBA.fromInts(48, 48, 96, 160);

    private final Button backToGameButton;
    private final Button resetLevelButton;
    private final Button quitGameButton;

    public PauseMenuUI(VoxelGame game) {
        backToGameButton = new Button("Back to Game", () -> game.setUI(null));
        resetLevelButton = new Button("Create New Level...", () -> game.setUI(new CreateLevelUI(game, this)));
        quitGameButton = new Button("Quit Game", game::stop);
    }

    @Override
    public void draw(UIDrawList draw, UISprites sprites, Vector2i mousePos) {
        int centerX = draw.getWidth() / 2;
        int centerY = draw.getHeight() / 2;
        
        int cornerX = centerX - Button.WIDTH / 2;
        int cornerY = centerY - (3 * Button.HEIGHT + 32) / 2;
        
        backToGameButton.setPosition(cornerX, cornerY);
        resetLevelButton.setPosition(cornerX, cornerY + Button.HEIGHT + 4);
        quitGameButton.setPosition(cornerX, cornerY + Button.HEIGHT * 2 + 32);

        draw.drawRectVGradient(0, 0, draw.getWidth(), draw.getHeight(), BG_COLOR_1, BG_COLOR_2);
        draw.drawTextCentered(centerX, cornerY - 24, TITLE);
        backToGameButton.draw(draw, sprites, mousePos);
        resetLevelButton.draw(draw, sprites, mousePos);
        quitGameButton.draw(draw, sprites, mousePos);
    }

    @Override
    public void mouseClicked(Vector2i mousePos) {
        backToGameButton.mouseClicked(mousePos);
        resetLevelButton.mouseClicked(mousePos);
        quitGameButton.mouseClicked(mousePos);
    }

    @Override
    public boolean shouldPauseGame() {
        return true;
    }
}
