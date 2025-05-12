package com.github.rmheuer.voxel.ui;

import java.util.function.Consumer;

public final class CycleButton<T> extends Button {
    private final String labelPrefix;
    private final T[] options;
    private int currentOption;
    private final Consumer<T> pickCallback;

    public CycleButton(String labelPrefix, T[] options, int currentOption, Consumer<T> pickCallback) {
        super(labelPrefix + options[currentOption], null);
        this.options = options;
        this.currentOption = currentOption;
        this.labelPrefix = labelPrefix;
        this.pickCallback = pickCallback;
    }

    @Override
    protected void clicked() {
        currentOption++;
        if (currentOption == options.length)
            currentOption = 0;

        pickCallback.accept(options[currentOption]);

        setLabel(labelPrefix + options[currentOption]);
    }
}
