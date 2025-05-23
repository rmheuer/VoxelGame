package com.github.rmheuer.voxel.client.ui;

import com.github.rmheuer.azalea.input.keyboard.Key;
import com.github.rmheuer.voxel.client.RenderDistance;

import com.github.rmheuer.voxel.client.VoxelGame;
import org.joml.Vector2i;

/**
 * UI for when the game is paused.
 */
public final class GameMenuUI implements UI {
    private static final String TITLE = "Game Menu";

    private final VoxelGame game;

    private final Button backToGameButton;
    private final CycleButton<RenderDistance> renderDistanceButton;
    private final Button resetLevelButton;
    private final Button openToLANButton;
    private final Button quitGameButton;

    /**
     * @param game main game instance
     */
    public GameMenuUI(VoxelGame game) {
        this.game = game;

        backToGameButton = new Button("Back to Game", () -> game.setUI(null));
        renderDistanceButton = new CycleButton<>("Render Distance: ", RenderDistance.values(), game.getRenderDistance().ordinal(), game::setRenderDistance);
        resetLevelButton = new Button("Create New Level...", () -> game.setUI(new CreateLevelUI(game, this, true)));
        openToLANButton = new Button("Open to LAN", () -> {
            game.openSinglePlayerServerToLAN();
            game.setUI(null);
        });
        quitGameButton = new Button("Quit Game", game::stop);

        resetLevelButton.setEnabled(false); // FIXME: Currently does not work
        openToLANButton.setEnabled(game.isPlayingSingleplayer());
    }

    @Override
    public void draw(UIDrawList draw, Vector2i mousePos) {
        int centerX = draw.getWidth() / 2;
        int centerY = draw.getHeight() / 2;
        
        int cornerX = centerX - Button.WIDTH / 2;
        int cornerY = centerY - (5 * Button.HEIGHT + 40) / 2;
        
        backToGameButton.setPosition(cornerX, cornerY);
        renderDistanceButton.setPosition(cornerX, cornerY + Button.HEIGHT + 4);
        resetLevelButton.setPosition(cornerX, cornerY + Button.HEIGHT * 2 + 8);
        openToLANButton.setPosition(cornerX, cornerY + Button.HEIGHT * 3 + 12);
        quitGameButton.setPosition(cornerX, cornerY + Button.HEIGHT * 4 + 40);

        draw.drawGradientBackground(0, 0, draw.getWidth(), draw.getHeight());
        draw.drawTextCentered(centerX, cornerY - 24, TITLE);
        backToGameButton.draw(draw, mousePos);
        renderDistanceButton.draw(draw, mousePos);
        resetLevelButton.draw(draw, mousePos);
        openToLANButton.draw(draw, mousePos);
        quitGameButton.draw(draw, mousePos);
    }

    @Override
    public void mouseClicked(Vector2i mousePos) {
        backToGameButton.mouseClicked(mousePos);
        renderDistanceButton.mouseClicked(mousePos);
        resetLevelButton.mouseClicked(mousePos);
        openToLANButton.mouseClicked(mousePos);
        quitGameButton.mouseClicked(mousePos);
    }

    @Override
    public boolean keyPressed(Key key) {
        if (key == Key.ESCAPE) {
            game.setUI(null);
            return true;
        }
        return false;
    }

    @Override
    public boolean shouldPauseGame() {
        return true;
    }
}
