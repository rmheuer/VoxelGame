package com.github.rmheuer.voxel.render;

import com.github.rmheuer.azalea.io.ResourceUtil;
import com.github.rmheuer.azalea.render.Renderer;
import com.github.rmheuer.azalea.render.mesh.*;
import com.github.rmheuer.azalea.render.pipeline.*;
import com.github.rmheuer.azalea.render.shader.ShaderProgram;
import com.github.rmheuer.azalea.render.shader.ShaderUniform;
import com.github.rmheuer.azalea.render.texture.Texture2D;
import com.github.rmheuer.azalea.render.utils.SharedIndexBuffer;
import com.github.rmheuer.azalea.utils.SafeCloseable;
import com.github.rmheuer.voxel.block.Block;
import com.github.rmheuer.voxel.level.*;
import org.joml.Matrix4f;
import org.joml.Vector3fc;

import java.io.IOException;
import java.util.*;

public final class LevelRenderer implements SafeCloseable {
    private final Texture2D atlasTexture;

    private final ShaderProgram shader;
    private final PipelineInfo pipeline;
    private final SharedIndexBuffer sharedIndexBuffer;

    public LevelRenderer(Renderer renderer, Texture2D atlasTexture) throws IOException {
        this.atlasTexture = atlasTexture;

        shader = renderer.createShaderProgram(
                ResourceUtil.readAsStream("shaders/block_vertex.glsl"),
                ResourceUtil.readAsStream("shaders/fragment.glsl")
        );
        pipeline = new PipelineInfo(shader)
                .setDepthTest(true)
                .setWinding(FaceWinding.CCW_FRONT)
                .setFillMode(FillMode.FILLED)
                .setCullMode(CullMode.BACK);

        sharedIndexBuffer = new SharedIndexBuffer(
                renderer,
                PrimitiveType.TRIANGLES,
                4,
                0, 1, 2, 0, 2, 3
        );
    }

    private static final class RenderSection {
        public final int blockX, blockY, blockZ;
        public final SectionRenderLayer layer;

        public RenderSection(int blockX, int blockY, int blockZ, SectionRenderLayer layer) {
            this.blockX = blockX;
            this.blockY = blockY;
            this.blockZ = blockZ;
            this.layer = layer;
        }
    }

    public static final class PreparedRender {
        private final PipelineInfo pipeline;
        private final IndexBuffer indexBuffer;
        private final Texture2D atlasTexture;
        private final List<RenderSection> opaqueToRender;
        private final List<RenderSection> translucentToRender;

        PreparedRender(PipelineInfo pipeline, IndexBuffer indexBuffer, Texture2D atlasTexture, List<RenderSection> opaqueToRender, List<RenderSection> translucentToRender) {
            this.pipeline = pipeline;
            this.indexBuffer = indexBuffer;
            this.atlasTexture = atlasTexture;
            this.opaqueToRender = opaqueToRender;
            this.translucentToRender = translucentToRender;
        }

        public void renderOpaqueLayer(Renderer renderer, Matrix4f view, Matrix4f proj, FogInfo fogInfo) {
            renderLayer(renderer, view, proj, fogInfo, opaqueToRender);
        }

        public void renderTranslucentLayer(Renderer renderer, Matrix4f view, Matrix4f proj, FogInfo fogInfo) {
            renderLayer(renderer, view, proj, fogInfo, translucentToRender);
        }

        private void renderLayer(Renderer renderer, Matrix4f view, Matrix4f proj, FogInfo fogInfo, List<RenderSection> sectionsToRender) {
            try (ActivePipeline pipe = renderer.bindPipeline(pipeline)) {
                pipe.bindTexture(0, atlasTexture);
                pipe.getUniform("u_View").setMat4(view);
                pipe.getUniform("u_Proj").setMat4(proj);
                pipe.getUniform("u_FogStart").setFloat(fogInfo.minDistance);
                pipe.getUniform("u_FogEnd").setFloat(fogInfo.maxDistance);
                pipe.getUniform("u_FogColor").setVec4(fogInfo.color);
                pipe.getUniform("u_TintColor").setVec4(fogInfo.tintColor);

                ShaderUniform offsetUniform = pipe.getUniform("u_SectionOffset");
                for (RenderSection section : sectionsToRender) {
                    offsetUniform.setVec3(section.blockX, section.blockY, section.blockZ);
                    pipe.draw(section.layer.getVertexBuffer(), indexBuffer, 0, section.layer.getElementCount());
                }
            }
        }
    }

    public PreparedRender prepareRender(Renderer renderer, BlockMap blockMap, LightMap lightMap, LevelRenderData renderData, Vector3fc cameraPos, boolean wireframe) {
        int sectionsX = blockMap.getSectionsX();
        int sectionsY = blockMap.getSectionsY();
        int sectionsZ = blockMap.getSectionsZ();

        boolean cameraMoved = !renderData.getPrevCameraPos().equals(cameraPos);
        renderData.setPrevCameraPos(cameraPos);

        List<RenderSection> opaqueToRender = new ArrayList<>();
        List<RenderSection> translucentToRender = new ArrayList<>();

        int updated = 0;
        for (int sectionY = 0; sectionY < sectionsY; sectionY++) {
            for (int sectionZ = 0; sectionZ < sectionsZ; sectionZ++) {
                for (int sectionX = 0; sectionX < sectionsX; sectionX++) {
                    SectionRenderData renderSection = renderData.getSection(sectionX, sectionY, sectionZ);

                    int ox = sectionX * MapSection.SIZE;
                    int oy = sectionY * MapSection.SIZE;
                    int oz = sectionZ * MapSection.SIZE;

                    boolean updateTranslucent = cameraMoved;
                    if (renderSection.isMeshOutdated()) {
                        try (SectionGeometry geom = createSectionGeometry(blockMap, lightMap, sectionX, sectionY, sectionZ)) {
                            renderSection.getOpaqueLayer().updateMesh(renderer, geom.getOpaqueData());
                            renderSection.setTranslucentFaces(geom.getTranslucentFaces());
                            sharedIndexBuffer.ensureCapacity(geom.getRequiredFaceCount());
                            renderSection.clearOutdated();
                        }
                        updated++;
                        updateTranslucent = true;
                    }

                    if (updateTranslucent) {
                        // Reorder translucent faces from back to front
                        List<BlockFace> translucentFaces = renderSection.getTranslucentFaces();
                        translucentFaces.sort(Comparator.comparingDouble((face) -> -cameraPos.distanceSquared(face.getCenterPos(ox, oy, oz))));

                        try (VertexData data = new VertexData(BlockFace.VERTEX_LAYOUT)) {
                            for (BlockFace face : translucentFaces) {
                                face.addToMesh(data);
                            }
                            renderSection.getTranslucentLayer().updateMesh(renderer, data);
                        }
                    }

                    SectionRenderLayer opaque = renderSection.getOpaqueLayer();
                    SectionRenderLayer translucent = renderSection.getTranslucentLayer();
                    if (opaque.getElementCount() > 0) {
                        opaqueToRender.add(new RenderSection(ox, oy, oz, opaque));
                    }
                    if (translucent.getElementCount() > 0) {
                        translucentToRender.add(new RenderSection(ox, oy, oz, translucent));
                    }
                }
            }
        }
        if (updated > 0) {
            System.out.println("Updated " + updated + " section mesh(es)");
        }

        // Sort opaque sections front to back to minimize overdraw
        int halfSz = MapSection.SIZE / 2;
        opaqueToRender.sort(Comparator.comparingDouble((section) -> cameraPos.distanceSquared(
                section.blockX + halfSz,
                section.blockY + halfSz,
                section.blockZ + halfSz
        )));

        // Sort translucent sections back to front for correct blending
        translucentToRender.sort(Comparator.comparingDouble((section) -> -cameraPos.distanceSquared(
                section.blockX + halfSz,
                section.blockY + halfSz,
                section.blockZ + halfSz
        )));

        pipeline.setFillMode(wireframe ? FillMode.WIREFRAME : FillMode.FILLED);

        return new PreparedRender(pipeline, sharedIndexBuffer.getIndexBuffer(), atlasTexture, opaqueToRender, translucentToRender);
    }

    private SectionGeometry createSectionGeometry(BlockMap blockMap, LightMap lightMap, int sectionX, int sectionY, int sectionZ) {
        SectionGeometry geom = new SectionGeometry();
        SectionContext ctx = new SectionContext(blockMap, lightMap, sectionX, sectionY, sectionZ);
        if (ctx.isEmpty())
            return geom;

        for (int y = 0; y < MapSection.SIZE; y++) {
            for (int z = 0; z < MapSection.SIZE; z++) {
                for (int x = 0; x < MapSection.SIZE; x++) {
                    Block block = ctx.getLocalBlock(x, y, z);

                    block.getShape().mesh(ctx, block, x, y, z, geom);
                }
            }
        }

        return geom;
    }

    // All block positions at the outside surface of the section
    private static final List<Vector3i> STARTING_POINTS = new ArrayList<>();
    static {
        int size = MapSection.SIZE;
        
        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                STARTING_POINTS.add(new Vector3i(x, 0, z));
                STARTING_POINTS.add(new Vector3i(x, size - 1, z));
            }
        }

        for (int y = 1; y < size - 1; y++) {
            for (int x = 0; x < size; x++) {
                STARTING_POINTS.add(new Vector3i(x, y, 0));
                STARTING_POINTS.add(new Vector3i(x, y, size - 1));
            }
        }

        for (int y = 1; y < size - 1; y++) {
            for (int z = 1; z < size - 1; z++) {
                STARTING_POINTS.add(new Vector3i(0, y, z));
                STARTING_POINTS.add(new Vector3i(size - 1, y, z));
            }
        }
    }

    private static final class FaceSet {
        private static final byte BITS_ALL = (byte) 0b111111;

        private final byte bits;

        public FaceSet() {
            bits = 0;
        }

        public void clear() {
            bits = 0;
        }

        public void addFace(CubeFace face) {
            bits |= 1 << face.ordinal();
        }

        public boolean containsFace(CubeFace face) {
            return (bits & (1 << face.ordinal())) != 0;
        }

        public boolean containsAll() {
            return bits == BITS_ALL;
        }
    }

    private SectionVisibility calcSectionVisibility(MapSection section) {
        if (section.isEmpty())
            return SectionVisibility.all();

        BitSet visited = new BitSet(MapSection.SIZE_CUBED);
        Queue<Vector3i> frontier = new ArrayDeque<>();
        SectionVisibility visibility = SectionVisibility.none();
        FaceSet touchedFaces = new FaceSet();

        for (Vector3i start : STARTING_POINTS) {
            touchedFaces.clear();
            frontier.clear();
            frontier.add(start);

            Vector3i pos;
            while ((pos = frontier.poll()) != null) {
                
            }
        }
        
        return visibility;
    }

    @Override
    public void close() {
        shader.close();
        sharedIndexBuffer.close();
    }
}
