package com.github.rmheuer.voxel.client.ui;

import com.github.rmheuer.voxel.client.VoxelGame;

import org.joml.Vector2i;

/**
 * UI to choose the size to create a new level.
 */
public final class CreateLevelUI implements UI {
    private static final String TITLE = "Create Level";

    private final boolean inGame;
    private final Button smallButton;
    private final Button normalButton;
    private final Button largeButton;
    private final Button cancelButton;

    /**
     * @param game main game instance
     * @param prevUI ui to go back to if cancel is pressed
     */
    public CreateLevelUI(VoxelGame game, UI prevUI, boolean inGame) {
        this.inGame = inGame;

        smallButton = new Button("Small (128x128)", () -> createLevel(game, 128));
        normalButton = new Button("Normal (256x256)", () -> createLevel(game, 256));
        largeButton = new Button("Large (512x512)", () -> createLevel(game, 512));
        cancelButton = new Button("Cancel", () -> game.setUI(prevUI));

        // TODO: Re-enable once singleplayer is implemented
        smallButton.setEnabled(false);
        normalButton.setEnabled(false);
        largeButton.setEnabled(false);
    }

    private void createLevel(VoxelGame game, int size) {
//        game.resetLevel(size / MapSection.SIZE);
        game.setUI(null);
    }

    @Override
    public void draw(UIDrawList draw, Vector2i mousePos) {
        int centerX = draw.getWidth() / 2;
        int centerY = draw.getHeight() / 2;
        
        int cornerX = centerX - Button.WIDTH / 2;
        int cornerY = centerY - (4 * Button.HEIGHT + 36) / 2;

        smallButton.setPosition(cornerX, cornerY);
        normalButton.setPosition(cornerX, cornerY + Button.HEIGHT + 4);
        largeButton.setPosition(cornerX, cornerY + Button.HEIGHT * 2 + 8);
        cancelButton.setPosition(cornerX, cornerY + Button.HEIGHT * 3 + 36);

        if (inGame)
            draw.drawGradientBackground(0, 0, draw.getWidth(), draw.getHeight());
        else
            draw.drawDirtBackground(0, 0, draw.getWidth(), draw.getHeight());

        draw.drawTextCentered(centerX, cornerY - 24, TITLE);
        smallButton.draw(draw, mousePos);
        normalButton.draw(draw, mousePos);
        largeButton.draw(draw, mousePos);
        cancelButton.draw(draw, mousePos);
    }

    @Override
    public void mouseClicked(Vector2i mousePos) {
        smallButton.mouseClicked(mousePos);
        normalButton.mouseClicked(mousePos);
        largeButton.mouseClicked(mousePos);
        cancelButton.mouseClicked(mousePos);
    }

    @Override
    public boolean shouldPauseGame() {
        return true;
    }
}
