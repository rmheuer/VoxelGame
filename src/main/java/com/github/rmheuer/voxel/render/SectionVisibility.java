package com.github.rmheuer.voxel.render;

import com.github.rmheuer.azalea.math.CubeFace;

/**
 * Represents the visibility between faces of a section.
 */
public final class SectionVisibility {
    private static final int BITS_ALL = 0x3FFFFFFF;
    private static final int BITS_NONE = 0x00000000;

    /**
     * @return visibility where all face pairs are visible
     */
    public static SectionVisibility all() {
        return new SectionVisibility(BITS_ALL);
    }

    /**
     * @return visibility where no face pairs are visible
     */
    public static SectionVisibility none() {
        return new SectionVisibility(BITS_NONE);
    }
    
    private int bits;

    private SectionVisibility(int bits) {
        this.bits = bits;
    }

    // Both indices must be 0-5, and indexA != indexB
    private int bit(int indexA, int indexB) {
        // Make indexB 0-4 by removing indexA from the sequence
        if (indexB > indexA)
            indexB--;

        int bitIdx = indexA * 5 + indexB;
        return 1 << bitIdx;
    }

    /**
     * Marks a face pair as being possible to see between them.
     *
     * @param from face looking into
     * @param to face looking out of
     */
    public void setVisible(CubeFace from, CubeFace to) {
        int i1 = from.ordinal();
        int i2 = to.ordinal();
        bits |= bit(i1, i2);
        bits |= bit(i2, i1);
    }

    /**
     * Gets whether it is possible to see between a pair of faces.
     *
     * @param from face looking into
     * @param to face looking out of
     */
    public boolean isVisible(CubeFace from, CubeFace to) {
        int b = bit(from.ordinal(), to.ordinal());
        return (bits & b) != 0;
    }

    /**
     * @return whether it is possible to see between all pairs of faces
     */
    public boolean isAll() {
        return bits == BITS_ALL;
    }

    /**
     * @return whether it is not possible to see between any pair of faces
     */
    public boolean isNone() {
        return bits == BITS_NONE;
    }
}
