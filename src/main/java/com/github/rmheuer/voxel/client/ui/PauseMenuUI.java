package com.github.rmheuer.voxel.client.ui;

import com.github.rmheuer.azalea.render.Colors;
import com.github.rmheuer.voxel.client.RenderDistance;
import com.github.rmheuer.voxel.client.VoxelGame;

import org.joml.Vector2i;

/**
 * UI for when the game is paused.
 */
public final class PauseMenuUI implements UI {
    private static final String TITLE = "Game Paused";

    private static final int BG_COLOR_1 = Colors.RGBA.fromInts(5, 5, 0, 96);
    private static final int BG_COLOR_2 = Colors.RGBA.fromInts(48, 48, 96, 160);

    private final Button backToGameButton;
    private final CycleButton<RenderDistance> renderDistanceButton;
    private final Button resetLevelButton;
    private final Button quitGameButton;

    /**
     * @param game main game instance
     */
    public PauseMenuUI(VoxelGame game) {
        backToGameButton = new Button("Back to Game", () -> game.setUI(null));
        renderDistanceButton = new CycleButton<>("Render Distance: ", RenderDistance.values(), game.getRenderDistance().ordinal(), game::setRenderDistance);
        resetLevelButton = new Button("Create New Level...", () -> game.setUI(new CreateLevelUI(game, this)));
        quitGameButton = new Button("Quit Game", game::stop);
    }

    @Override
    public void draw(UIDrawList draw, UISprites sprites, Vector2i mousePos) {
        int centerX = draw.getWidth() / 2;
        int centerY = draw.getHeight() / 2;
        
        int cornerX = centerX - Button.WIDTH / 2;
        int cornerY = centerY - (4 * Button.HEIGHT + 36) / 2;
        
        backToGameButton.setPosition(cornerX, cornerY);
        renderDistanceButton.setPosition(cornerX, cornerY + Button.HEIGHT + 4);
        resetLevelButton.setPosition(cornerX, cornerY + Button.HEIGHT * 2 + 8);
        quitGameButton.setPosition(cornerX, cornerY + Button.HEIGHT * 3 + 36);

        draw.drawRectVGradient(0, 0, draw.getWidth(), draw.getHeight(), BG_COLOR_1, BG_COLOR_2);
        draw.drawTextCentered(centerX, cornerY - 24, TITLE);
        backToGameButton.draw(draw, sprites, mousePos);
        renderDistanceButton.draw(draw, sprites, mousePos);
        resetLevelButton.draw(draw, sprites, mousePos);
        quitGameButton.draw(draw, sprites, mousePos);
    }

    @Override
    public void mouseClicked(Vector2i mousePos) {
        backToGameButton.mouseClicked(mousePos);
        renderDistanceButton.mouseClicked(mousePos);
        resetLevelButton.mouseClicked(mousePos);
        quitGameButton.mouseClicked(mousePos);
    }

    @Override
    public boolean shouldPauseGame() {
        return true;
    }
}
