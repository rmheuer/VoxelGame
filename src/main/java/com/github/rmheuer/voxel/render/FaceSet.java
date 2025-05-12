package com.github.rmheuer.voxel.render;

import com.github.rmheuer.azalea.math.CubeFace;

public final class FaceSet {
    private static final byte ALL_BITS = (byte) 0b111111;
    private static final byte NONE_BITS = 0;

    public static FaceSet none() {
        return new FaceSet(NONE_BITS);
    }
    
    private byte bits;

    private FaceSet(byte bits) {
        this.bits = bits;
    }

    public FaceSet(FaceSet other) {
        bits = other.bits;
    }

    public void clear() {
        bits = NONE_BITS;
    }

    public void addFace(CubeFace face) {
        bits |= 1 << face.ordinal();
    }

    public boolean containsFace(CubeFace face) {
        return (bits & (1 << face.ordinal())) != 0;
    }

    public boolean isAll() {
        return bits == ALL_BITS;
    }

    public boolean isNone() {
        return bits == NONE_BITS;
    }
}
