package com.github.rmheuer.voxel.anim;

import com.github.rmheuer.azalea.math.MathUtil;
import com.github.rmheuer.azalea.render.Colors;
import com.github.rmheuer.azalea.render.texture.Bitmap;
import com.github.rmheuer.azalea.render.texture.ColorFormat;

// From https://github.com/ClassiCube/ClassiCube/wiki/Minecraft-Classic-lava-animation-algorithm
public final class WaterAnimationGenerator {
    private final float[] flameHeat = new float[256];
    private final float[] potHeat = new float[256];
    private final float[] soupHeat = new float[256];

    private int index(int x, int y) {
        if (x < 0) x += 16;
        if (x >= 16) x -= 16;

        if (y < 0) y += 16;
        if (y >= 16) y -= 16;

        return x + y * 16;
    }

    public void tick() {
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                float localSoupHeat = soupHeat[index(x - 1, y)]
                        + soupHeat[index(x, y)]
                        + soupHeat[index(x + 1, y)];

                int i = index(x, y);
                soupHeat[i] = localSoupHeat / 3.3f + potHeat[i] * 0.8f;
                potHeat[i] += flameHeat[i] * 0.05f;
                if (potHeat[i] < 0)
                    potHeat[i] = 0;
                flameHeat[i] -= 0.1f;

                if (Math.random() < 0.05)
                    flameHeat[i] = 0.5f;
            }
        }
    }

    public Bitmap getImage() {
        Bitmap img = new Bitmap(16, 16, ColorFormat.RGBA);

        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                float colorHeat = MathUtil.clamp(soupHeat[index(x, y)], 0, 1);
                float colorHeatSq = colorHeat * colorHeat;

                int red = (int) (32 + colorHeatSq * 32);
                int green = (int) (50 + colorHeatSq * 64);
                int blue = 255;
                int alpha = (int) (146 + colorHeatSq * 50);

                img.setPixel(x, y, Colors.RGBA.fromInts(red, green, blue, alpha));
            }
        }

        return img;
    }
}
