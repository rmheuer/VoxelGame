package com.github.rmheuer.voxel.client.render;

import com.github.rmheuer.azalea.math.CubeFace;

/**
 * Represents a set of {@link CubeFace}s.
 */
public final class FaceSet {
    private static final byte ALL_BITS = (byte) 0b111111;
    private static final byte NONE_BITS = 0;

    /**
     * @return a new FaceSet containing no faces
     */
    public static FaceSet none() {
        return new FaceSet(NONE_BITS);
    }
    
    private byte bits;

    private FaceSet(byte bits) {
        this.bits = bits;
    }

    /**
     * Creates a new FaceSet containing the same faces as another.
     *
     * @param other other set to copy from
     */
    public FaceSet(FaceSet other) {
        bits = other.bits;
    }

    /**
     * Resets this FaceSet to contain no faces.
     */
    public void clear() {
        bits = NONE_BITS;
    }

    /**
     * Adds a face to the set.
     *
     * @param face CubeFace to add
     */
    public void addFace(CubeFace face) {
        bits |= 1 << face.ordinal();
    }

    /**
     * Gets whether a face is contained within the set.
     *
     * @param face CubeFace to check
     * @return whether the face is in the set
     */
    public boolean containsFace(CubeFace face) {
        return (bits & (1 << face.ordinal())) != 0;
    }

    /**
     * Gets whether this set contains all the faces.
     *
     * @return whether all faces are contained
     */
    public boolean isAll() {
        return bits == ALL_BITS;
    }

    /**
     * Gets whether this set contains no faces.
     *
     * @return whether no faces are contained
     */
    public boolean isNone() {
        return bits == NONE_BITS;
    }
}
