package com.github.rmheuer.voxel.ui;

import com.github.rmheuer.azalea.render.Colors;
import com.github.rmheuer.voxel.VoxelGame;

import org.joml.Vector2i;

public final class CreateLevelUI implements UI {
    private static final String TITLE = "Create Level";

    private static final int BG_COLOR_1 = Colors.RGBA.fromInts(5, 5, 0, 96);
    private static final int BG_COLOR_2 = Colors.RGBA.fromInts(48, 48, 96, 160);

    private final Button smallButton;
    private final Button normalButton;
    private final Button largeButton;
    private final Button cancelButton;

    public CreateLevelUI(VoxelGame game, PauseMenuUI pauseMenu) {
        smallButton = new Button("Small (128x128)", () -> createLevel(game, 128));
        normalButton = new Button("Normal (256x256)", () -> createLevel(game, 256));
        largeButton = new Button("Large (512x512)", () -> createLevel(game, 512));
        cancelButton = new Button("Cancel", () -> game.setUI(pauseMenu));
    }

    private void createLevel(VoxelGame game, int size) {
        game.resetLevel();
        game.setUI(null);
    }

    @Override
    public void draw(UIDrawList draw, UISprites sprites, Vector2i mousePos) {
        int centerX = draw.getWidth() / 2;
        int centerY = draw.getHeight() / 2;
        
        int cornerX = centerX - Button.WIDTH / 2;
        int cornerY = centerY - (4 * Button.HEIGHT + 36) / 2;

        smallButton.setPosition(cornerX, cornerY);
        normalButton.setPosition(cornerX, cornerY + Button.HEIGHT + 4);
        largeButton.setPosition(cornerX, cornerY + Button.HEIGHT * 2 + 8);
        cancelButton.setPosition(cornerX, cornerY + Button.HEIGHT * 3 + 36);

        draw.drawRectVGradient(0, 0, draw.getWidth(), draw.getHeight(), BG_COLOR_1, BG_COLOR_2);
        draw.drawTextCentered(centerX, cornerY - 24, TITLE);
        smallButton.draw(draw, sprites, mousePos);
        normalButton.draw(draw, sprites, mousePos);
        largeButton.draw(draw, sprites, mousePos);
        cancelButton.draw(draw, sprites, mousePos);
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
