package com.github.rmheuer.voxel.client.ui;

import com.github.rmheuer.azalea.render.Colors;

import java.util.HashMap;
import java.util.Map;

public final class ChatColors {
    private static final Map<Character, Integer> colors = new HashMap<>();
    static {
        colors.put('0', Colors.RGBA.fromInts(0, 0, 0));
        colors.put('1', Colors.RGBA.fromInts(0, 0, 191));
        colors.put('2', Colors.RGBA.fromInts(0, 191, 0));
        colors.put('3', Colors.RGBA.fromInts(0, 191, 191));
        colors.put('4', Colors.RGBA.fromInts(191, 0, 0));
        colors.put('5', Colors.RGBA.fromInts(191, 0, 191));
        colors.put('6', Colors.RGBA.fromInts(191, 191, 0));
        colors.put('7', Colors.RGBA.fromInts(191, 191, 191));
        colors.put('8', Colors.RGBA.fromInts(64, 64, 64));
        colors.put('9', Colors.RGBA.fromInts(64, 64, 255));
        colors.put('a', Colors.RGBA.fromInts(64, 255, 64));
        colors.put('b', Colors.RGBA.fromInts(64, 255, 255));
        colors.put('c', Colors.RGBA.fromInts(255, 64, 64));
        colors.put('d', Colors.RGBA.fromInts(255, 64, 255));
        colors.put('e', Colors.RGBA.fromInts(255, 255, 64));
        colors.put('f', Colors.RGBA.fromInts(255, 255, 255));
    }

    public static Integer getColorForFormatChar(char c) {
        return colors.get(c);
    }
}
