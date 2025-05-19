package com.github.rmheuer.voxel.client.ui;

import com.github.rmheuer.azalea.input.keyboard.Key;
import com.github.rmheuer.azalea.render.Colors;
import com.github.rmheuer.voxel.network.PacketDataBuf;
import org.joml.Vector2i;

import java.util.function.Consumer;

public final class TextInputBox {
    private static final int BG_COLOR = Colors.RGBA.BLACK;
    private static final int BORDER_COLOR = Colors.RGBA.fromInts(192, 192, 192);
    private static final int ACTIVE_BORDER_COLOR = Colors.RGBA.WHITE;

    private Consumer<String> onChange, onConfirm;
    private int x, y;

    private String input;
    private int cursor;

    public TextInputBox() {
        input = "";
        onChange = null;
        onConfirm = null;
        cursor = -1;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
        if (onChange != null)
            onChange.accept(input);
    }

    public void setOnChange(Consumer<String> onChange) {
        this.onChange = onChange;
    }

    public void setOnConfirm(Consumer<String> onConfirm) {
        this.onConfirm = onConfirm;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    private boolean mouseOver(Vector2i mousePos) {
        return mousePos.x >= x && mousePos.x < x + Button.WIDTH
                && mousePos.y >= y && mousePos.y < y + Button.HEIGHT;
    }

    public void draw(UIDrawList draw) {
        draw.drawRect(x, y, Button.WIDTH, Button.HEIGHT, BG_COLOR);
        draw.drawRectOutline(x, y, Button.WIDTH, Button.HEIGHT, cursor >= 0 ? ACTIVE_BORDER_COLOR : BORDER_COLOR);

        int textX = x + 4;
        int textY = y + Button.HEIGHT / 2 + 3;
        draw.drawText(textX, textY, input);

        if (cursor >= 0) {
            int cursorX = draw.textWidth(input.substring(0, cursor)) + textX + 1;
            if (cursor < input.length()) {
                draw.drawRect(cursorX, textY - 8, 1, 8, Colors.RGBA.WHITE);
            } else {
                draw.drawRect(cursorX, textY, 5, 1, Colors.RGBA.WHITE);
            }
        }
    }

    public boolean keyPressed(Key key) {
        if (cursor < 0)
            return false;

        switch (key) {
            case ENTER:
                cursor = -1;
                if (!input.isEmpty()  && onConfirm != null)
                    onConfirm.accept(input);
                break;
            case LEFT:
                if (cursor > 0)
                    cursor--;
                break;
            case RIGHT:
                if (cursor < input.length())
                    cursor++;
                break;
            case BACKSPACE:
                if (cursor > 0) {
                    setInput(input.substring(0, cursor - 1) + input.substring(cursor));
                    cursor--;
                }
                break;
        }

        // Consume all key presses if active
        return true;
    }

    public void charTyped(char c) {
        if (cursor < 0)
            return;

        if (c < ' ' || c > '~' || c == '&')
            return;

        if (input.length() < PacketDataBuf.MAX_STRING_LEN) {
            setInput(input.substring(0, cursor) + c + input.substring(cursor));
            cursor++;
        }
    }

    public void mouseClicked(Vector2i mousePos) {
        boolean over = mouseOver(mousePos);

        if (cursor < 0) {
            if (over)
                cursor = input.length();
        } else {
            if (!over)
                cursor = -1;
        }
    }
}
