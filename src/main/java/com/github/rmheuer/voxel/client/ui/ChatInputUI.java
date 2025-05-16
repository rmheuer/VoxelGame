package com.github.rmheuer.voxel.client.ui;

import com.github.rmheuer.azalea.input.keyboard.Key;
import com.github.rmheuer.azalea.render.Colors;
import com.github.rmheuer.voxel.client.VoxelGame;
import com.github.rmheuer.voxel.network.PacketDataBuf;
import org.joml.Vector2i;

public final class ChatInputUI implements UI {
    private static final int BG_COLOR = Colors.RGBA.fromInts(5, 5, 0, 96);

    private final VoxelGame game;

    private String input;
    private int inputCursor;

    public ChatInputUI(VoxelGame game) {
        this.game = game;
        input = "";
    }

    @Override
    public void draw(UIDrawList draw, UISprites sprites, Vector2i mousePos) {
        int width = draw.getWidth();
        int height = draw.getHeight();

        draw.drawRect(1, height - 11, width - 2, 10, BG_COLOR);
        draw.drawText(2, height - 2, input);
    }

    @Override
    public void mouseClicked(Vector2i mousePos) {
    }

    @Override
    public boolean keyPressed(Key key) {
        switch (key) {
            case ESCAPE:
                game.setUI(null);
                return true;
            case ENTER:
                if (!input.isEmpty()) {
                    game.sendChatMessage(input);
                    game.setUI(null);
                }
                return true;
            case LEFT:
                if (inputCursor > 0)
                    inputCursor--;
                return true;
            case RIGHT:
                if (inputCursor < input.length())
                    inputCursor++;
                return true;
            case BACKSPACE:
                if (inputCursor > 0) {
                    input = input.substring(0, inputCursor - 1) + input.substring(inputCursor);
                    inputCursor--;
                }
                return true;
        }
        return false;
    }

    @Override
    public void charTyped(char c) {
        if (c < ' ' || c > '~' || c == '&')
            return;

        if (input.length() < PacketDataBuf.MAX_STRING_LEN) {
            input = input.substring(0, inputCursor) + c + input.substring(inputCursor);
            inputCursor++;
        }
    }

    @Override
    public boolean shouldPauseGame() {
        return false;
    }
}
