package com.github.rmheuer.voxel.ui;

import org.joml.Vector2i;

public interface UI {
    void draw(UIDrawList draw, UISprites sprites, Vector2i mousePos);

    void mouseClicked(Vector2i mousePos);
}
