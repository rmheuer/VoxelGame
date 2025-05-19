package com.github.rmheuer.voxel.client.ui;

import com.github.rmheuer.azalea.render.Colors;

// TODO: Actually do the text input
public final class TextInputBox {
    private int x, y;

    public TextInputBox() {}

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void draw(UIDrawList draw) {
        draw.drawRect(x, y, 200, 20, Colors.RGBA.BLACK);
        draw.drawRectOutline(x, y, 200, 20, Colors.RGBA.fromInts(128, 128, 128));
    }
}
