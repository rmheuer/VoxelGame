package com.github.rmheuer.voxel.client.ui;

import java.util.function.Consumer;

/**
 * Button that cycles through a set of choices.
 */
public final class CycleButton<T> extends Button {
    private final String labelPrefix;
    private final T[] options;
    private int currentOption;
    private final Consumer<T> pickCallback;

    /**
     * @param labelPrefix prefix for the option in the button label
     * @param options array of possible options to choose
     * @param currentOption index of the current option in the options array
     * @param pickCallback function to call when the selected option is changed
     */
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
