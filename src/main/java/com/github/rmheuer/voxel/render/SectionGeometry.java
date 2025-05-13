package com.github.rmheuer.voxel.render;

import com.github.rmheuer.azalea.render.mesh.VertexData;
import com.github.rmheuer.azalea.utils.SafeCloseable;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores the result of meshing a level section.
 */
public final class SectionGeometry implements SafeCloseable {
    private final VertexData opaqueData;
    private final List<BlockFace> translucentFaces;

    public SectionGeometry() {
        opaqueData = new VertexData(BlockFace.VERTEX_LAYOUT);
        translucentFaces = new ArrayList<>();
    }

    /**
     * Adds a face to the section mesh.
     *
     * @param opaque whether the face is opaque (is not translucent)
     * @param face face to add
     */
    public void addFace(boolean opaque, BlockFace face) {
        if (opaque)
            face.addToMesh(opaqueData);
        else
            translucentFaces.add(face);
    }

    /**
     * Adds a face to the section mesh that will be visible from both sides.
     *
     * @param opaque whether the face is opaque (is not translucent)
     * @param face face to add
     */
    public void addDoubleSidedFace(boolean opaque, BlockFace face) {
        addFace(opaque, face);
        addFace(opaque, face.makeBackFace());
    }

    /**
     * Gets the number of faces required to be available in the shared index
     * buffer.
     *
     * @return required number of faces
     */
    public int getRequiredFaceCount() {
        return Math.max(opaqueData.getVertexCount() / 4, translucentFaces.size());
    }

    public VertexData getOpaqueData() {
        return opaqueData;
    }

    public List<BlockFace> getTranslucentFaces() {
        return translucentFaces;
    }

    @Override
    public void close() {
        opaqueData.close();
    }
}
