package com.github.rmheuer.voxel.ui;

import com.github.rmheuer.azalea.render.Colors;
import com.github.rmheuer.voxel.UIState;
import com.github.rmheuer.voxel.VoxelGame;

import org.joml.Vector2i;

public final class PauseMenuUI {
    private static final class Button {
        public static final int WIDTH = 200;
        public static final int HEIGHT = 20;

        private final String label;
        private final Runnable onClick;
        private int x, y;

        public Button(String label, Runnable onClick) {
            this.label = label;
            this.onClick = onClick;
        }

        public void setPosition(int x, int y) {
            this.x = x;
            this.y = y;
        }

        private boolean mouseOver(Vector2i mousePos) {
            return mousePos.x >= x && mousePos.x < x + WIDTH
                && mousePos.y >= y && mousePos.y < y + HEIGHT;
        }

        public void draw(UIDrawList draw, UISprites sprites, Vector2i mousePos) {
            boolean hover = mouseOver(mousePos);
            UISprite sprite = hover ? sprites.getButtonHighlight() : sprites.getButton();

            draw.drawSprite(x, y, sprite);
            draw.drawTextCentered(x + WIDTH / 2, y + HEIGHT / 2 + 3, label);
        }

        public void mouseClicked(Vector2i mousePos) {
            if (mouseOver(mousePos)) {
                onClick.run();
            }
        }
    }

    private static final int BG_COLOR_1 = Colors.RGBA.fromInts(5, 5, 0, 96);
    private static final int BG_COLOR_2 = Colors.RGBA.fromInts(48, 48, 96, 160);

    private final Button backToGameButton;
    private final Button quitGameButton;

    public PauseMenuUI(VoxelGame game) {
        backToGameButton = new Button("Back to Game", () -> game.setUIState(UIState.NONE));
        quitGameButton = new Button("Quit Game", () -> game.stop());
    }
    
    public void draw(UIDrawList draw, UISprites sprites, Vector2i mousePos) {
        int centerX = draw.getWidth() / 2;
        int centerY = draw.getHeight() / 2;
        
        int cornerX = centerX - Button.WIDTH / 2;
        int cornerY = centerY - (2 * Button.HEIGHT + 30) / 2;
        
        backToGameButton.setPosition(cornerX, cornerY);
        quitGameButton.setPosition(cornerX, cornerY + Button.HEIGHT + 30);

        draw.drawRectVGradient(0, 0, draw.getWidth(), draw.getHeight(), BG_COLOR_1, BG_COLOR_2);
        backToGameButton.draw(draw, sprites, mousePos);
        quitGameButton.draw(draw, sprites, mousePos);
    }

    public void mouseClicked(Vector2i mousePos) {
        backToGameButton.mouseClicked(mousePos);
        quitGameButton.mouseClicked(mousePos);
    }
}
