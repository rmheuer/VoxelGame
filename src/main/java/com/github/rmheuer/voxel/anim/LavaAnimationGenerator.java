package com.github.rmheuer.voxel.anim;

import com.github.rmheuer.azalea.math.MathUtil;
import com.github.rmheuer.azalea.render.Colors;
import com.github.rmheuer.azalea.render.texture.Bitmap;
import com.github.rmheuer.azalea.render.texture.ColorFormat;

public final class LavaAnimationGenerator {
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
            int rowSin = (int) (1.2 * Math.sin(Math.toRadians(22.5 * y)));
            for (int x = 0; x < 16; x++) {
                int colSin = (int) (1.2 * Math.sin(Math.toRadians(22.5 * x)));

                float localSoupHeat = 0;
                for (int oy = -1; oy <= 1; oy++) {
                    for (int ox = -1; ox <= 1; ox++) {
                        localSoupHeat += soupHeat[index(x + ox + rowSin, y + oy + colSin)];
                    }
                }

                float localPotHeat = potHeat[index(x, y)]
                        + potHeat[index(x + 1, y)]
                        + potHeat[index(x, y + 1)]
                        + potHeat[index(x + 1, y + 1)];

                int i = index(x, y);
                soupHeat[i] = localSoupHeat / 10 + localPotHeat / 4 * 0.8f;
                potHeat[i] += flameHeat[i] * 0.01f;
                if (potHeat[i] < 0)
                    potHeat[i] = 0;
                flameHeat[i] -= 0.06f;

                if (Math.random() < 0.005)
                    flameHeat[i] = 1.5f;
            }
        }
    }

    public Bitmap getImage() {
        Bitmap img = new Bitmap(16, 16, ColorFormat.RGBA);

        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                float colorHeat = MathUtil.clamp(2 * soupHeat[index(x, y)], 0, 1);
                float colorHeatSq = colorHeat * colorHeat;

                int red = (int) (colorHeat * 100 + 155);
                int green = (int) (colorHeatSq * 255);
                int blue = (int) (colorHeatSq * colorHeatSq * 128);
                int alpha = 255;

                img.setPixel(x, y, Colors.RGBA.fromInts(red, green, blue, alpha));
            }
        }

        return img;
    }
}
