package com.github.rmheuer.voxel.client.ui;

import com.github.rmheuer.azalea.render.Colors;

import java.util.HashMap;
import java.util.Map;

public final class ChatColors {
    private static final Map<Character, Integer> defaults = new HashMap<>();
    static {
        defaults.put('0', Colors.RGBA.fromInts(0, 0, 0));
        defaults.put('1', Colors.RGBA.fromInts(0, 0, 191));
        defaults.put('2', Colors.RGBA.fromInts(0, 191, 0));
        defaults.put('3', Colors.RGBA.fromInts(0, 191, 191));
        defaults.put('4', Colors.RGBA.fromInts(191, 0, 0));
        defaults.put('5', Colors.RGBA.fromInts(191, 0, 191));
        defaults.put('6', Colors.RGBA.fromInts(191, 191, 0));
        defaults.put('7', Colors.RGBA.fromInts(191, 191, 191));
        defaults.put('8', Colors.RGBA.fromInts(64, 64, 64));
        defaults.put('9', Colors.RGBA.fromInts(64, 64, 255));
        defaults.put('a', Colors.RGBA.fromInts(64, 255, 64));
        defaults.put('b', Colors.RGBA.fromInts(64, 255, 255));
        defaults.put('c', Colors.RGBA.fromInts(255, 64, 64));
        defaults.put('d', Colors.RGBA.fromInts(255, 64, 255));
        defaults.put('e', Colors.RGBA.fromInts(255, 255, 64));
        defaults.put('f', Colors.RGBA.fromInts(255, 255, 255));
    }

    private final Map<Character, Integer> customFormatCodes;

    public ChatColors() {
        customFormatCodes = new HashMap<>();
    }

    public void defineCustomColor(char c, int color) {
        customFormatCodes.put(c, color);
    }

    public void removeCustomColor(char c) {
        customFormatCodes.remove(c);
    }

    public Integer getColorForFormatChar(char c) {
        return customFormatCodes.computeIfAbsent(c, defaults::get);
    }
}
