package com.github.rmheuer.voxel.render;

import com.github.rmheuer.azalea.render.mesh.VertexData;
import com.github.rmheuer.azalea.utils.SafeCloseable;

import java.util.ArrayList;
import java.util.List;

public final class SectionGeometry implements SafeCloseable {
    private final VertexData opaqueData;
    private final List<BlockFace> translucentFaces;

    public SectionGeometry() {
        opaqueData = new VertexData(BlockFace.VERTEX_LAYOUT);
        translucentFaces = new ArrayList<>();
    }

    public void addFace(boolean opaque, BlockFace face) {
        if (opaque)
            face.addToMesh(opaqueData);
        else
            translucentFaces.add(face);
    }

    public void addDoubleSidedFace(boolean opaque, BlockFace face) {
        addFace(opaque, face);
        addFace(opaque, face.makeBackFace());
    }

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
