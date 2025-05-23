package com.github.rmheuer.voxel.client.ui;

import com.github.rmheuer.azalea.input.keyboard.Key;
import com.github.rmheuer.azalea.render.Colors;
import com.github.rmheuer.voxel.client.VoxelGame;
import com.github.rmheuer.voxel.network.PacketDataBuf;
import org.joml.Vector2i;

public final class ChatInputUI implements UI {
    private static final int BG_COLOR = Colors.RGBA.fromInts(5, 5, 0, 96);

    private final VoxelGame game;
    private final boolean limitLength;

    private String input;
    private int inputCursor;

    public ChatInputUI(VoxelGame game, boolean limitLength) {
        this.game = game;
        this.limitLength = limitLength;

        input = "";
    }

    @Override
    public void draw(UIDrawList draw, Vector2i mousePos) {
        int width = draw.getWidth();
        int height = draw.getHeight();

        draw.drawRect(1, height - 11, width - 2, 10, BG_COLOR);
        draw.drawText(2, height - 2, input);

        int cursorX = draw.textWidth(input.substring(0, inputCursor)) + 3;
        if (inputCursor < input.length()) {
            draw.drawRect(cursorX, height - 10, 1, 8, Colors.RGBA.WHITE);
        } else {
            draw.drawRect(cursorX, height - 2, 5, 1, Colors.RGBA.WHITE);
        }
    }

    @Override
    public void mouseClicked(Vector2i mousePos) {
    }

    @Override
    public boolean keyPressed(Key key) {
        switch (key) {
            case ESCAPE:
                game.setUI(null);
                break;
            case ENTER:
                if (!input.isEmpty()) {
                    game.sendChatMessage(input);
                    game.setUI(null);
                }
                break;
            case LEFT:
                if (inputCursor > 0)
                    inputCursor--;
                break;
            case RIGHT:
                if (inputCursor < input.length())
                    inputCursor++;
                break;
            case BACKSPACE:
                if (inputCursor > 0) {
                    input = input.substring(0, inputCursor - 1) + input.substring(inputCursor);
                    inputCursor--;
                }
                break;
        }
        return true;
    }

    @Override
    public void charTyped(char c) {
        if (c < ' ' || c > '~' || c == '&')
            return;

        if (!limitLength || input.length() < PacketDataBuf.MAX_STRING_LEN) {
            input = input.substring(0, inputCursor) + c + input.substring(inputCursor);
            inputCursor++;
        }
    }

    @Override
    public boolean shouldPauseGame() {
        return false;
    }
}
