package com.github.rmheuer.voxel.render;

import com.github.rmheuer.azalea.io.ResourceUtil;
import com.github.rmheuer.azalea.math.CubeFace;
import com.github.rmheuer.azalea.render.Colors;
import com.github.rmheuer.azalea.render.Renderer;
import com.github.rmheuer.azalea.render.mesh.*;
import com.github.rmheuer.azalea.render.pipeline.*;
import com.github.rmheuer.azalea.render.shader.ShaderProgram;
import com.github.rmheuer.azalea.render.shader.ShaderUniform;
import com.github.rmheuer.azalea.render.utils.SharedIndexBuffer;
import com.github.rmheuer.azalea.utils.SafeCloseable;
import com.github.rmheuer.voxel.level.Blocks;
import com.github.rmheuer.voxel.level.BlockMap;
import com.github.rmheuer.voxel.level.LightMap;
import com.github.rmheuer.voxel.level.MapSection;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class LevelRenderer implements SafeCloseable {
    private static final VertexLayout LAYOUT = new VertexLayout(
            AttribType.VEC3, // Position
            AttribType.COLOR_RGBA, // Color
            AttribType.FLOAT // Shade
    );

    private final ShaderProgram shader;
    private final PipelineInfo pipeline;
    private final SharedIndexBuffer sharedIndexBuffer;

    public LevelRenderer(Renderer renderer) throws IOException {
        shader = renderer.createShaderProgram(
                ResourceUtil.readAsStream("shaders/block-vert.glsl"),
                ResourceUtil.readAsStream("shaders/block-frag.glsl")
        );
        pipeline = new PipelineInfo(shader)
                .setDepthTest(true)
                .setWinding(FaceWinding.CCW_FRONT)
                .setFillMode(FillMode.FILLED);

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
        private final List<RenderSection> opaqueToRender;
        private final List<RenderSection> translucentToRender;

        PreparedRender(PipelineInfo pipeline, IndexBuffer indexBuffer, List<RenderSection> opaqueToRender, List<RenderSection> translucentToRender) {
            this.pipeline = pipeline;
            this.indexBuffer = indexBuffer;
            this.opaqueToRender = opaqueToRender;
            this.translucentToRender = translucentToRender;
        }

        public void renderOpaqueLayer(Renderer renderer, Matrix4f viewProj) {
            pipeline.setCullMode(CullMode.BACK);
            renderLayer(renderer, viewProj, opaqueToRender);
        }

        public void renderTranslucentLayer(Renderer renderer, Matrix4f viewProj) {
            pipeline.setCullMode(CullMode.OFF);
            renderLayer(renderer, viewProj, translucentToRender);
        }

        private void renderLayer(Renderer renderer, Matrix4f viewProj, List<RenderSection> sectionsToRender) {
            try (ActivePipeline pipe = renderer.bindPipeline(pipeline)) {
                pipe.getUniform("u_ViewProj").setMat4(viewProj);

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

                        try (VertexData data = new VertexData(LAYOUT)) {
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

        return new PreparedRender(pipeline, sharedIndexBuffer.getIndexBuffer(), opaqueToRender, translucentToRender);
    }

    private static final class SectionContext {
        private final BlockMap blockMap;
        private final MapSection section;

        private final LightMap lightMap;
        private final int originX, originY, originZ;

        public SectionContext(BlockMap blockMap, LightMap lightMap, int x, int y, int z) {
            this.blockMap = blockMap;
            section = blockMap.getSection(x, y, z);

            this.lightMap = lightMap;
            originX = x * MapSection.SIZE;
            originY = y * MapSection.SIZE;
            originZ = z * MapSection.SIZE;
        }

        public boolean isEmpty() {
            return section.isEmpty();
        }

        // XYZ must be in bounds of the section
        public byte getLocalBlock(int x, int y, int z) {
            return section.getBlockId(x, y, z);
        }

        // Returns null if outside world
        public Byte getSurroundingBlock(int x, int y, int z) {
            int blockX = x + originX;
            int blockY = y + originY;
            int blockZ = z + originZ;

            int sectionX = Math.floorDiv(blockX, MapSection.SIZE);
            int sectionY = Math.floorDiv(blockY, MapSection.SIZE);
            int sectionZ = Math.floorDiv(blockZ, MapSection.SIZE);

            if (sectionX < 0 || sectionX >= blockMap.getSectionsX()
                    || sectionY < 0 || sectionY >= blockMap.getSectionsY()
                    || sectionZ < 0 || sectionZ >= blockMap.getSectionsZ()) {
                return null;
            }

            return blockMap.getSection(sectionX, sectionY, sectionZ).getBlockId(
                    Math.floorMod(blockX, MapSection.SIZE),
                    Math.floorMod(blockY, MapSection.SIZE),
                    Math.floorMod(blockZ, MapSection.SIZE)
            );
        }

        // XYZ must be in bounds for the world
        public boolean isLit(int x, int y, int z) {
            return lightMap.isLit(originX + x, originY + y, originZ + z);
        }
    }

    private static final class SectionGeometry implements SafeCloseable {
        private final VertexData opaqueData;
        private final List<BlockFace> translucentFaces;

        public SectionGeometry() {
            opaqueData = new VertexData(LAYOUT);
            translucentFaces = new ArrayList<>();
        }

        public void addOpaqueFace(BlockFace face) {
            face.addToMesh(opaqueData);
        }

        public void addTranslucentFace(BlockFace face) {
            translucentFaces.add(face);
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

    private SectionGeometry createSectionGeometry(BlockMap blockMap, LightMap lightMap, int sectionX, int sectionY, int sectionZ) {
        SectionGeometry geom = new SectionGeometry();
        SectionContext ctx = new SectionContext(blockMap, lightMap, sectionX, sectionY, sectionZ);
        if (ctx.isEmpty())
            return geom;

        for (int y = 0; y < MapSection.SIZE; y++) {
            for (int z = 0; z < MapSection.SIZE; z++) {
                for (int x = 0; x < MapSection.SIZE; x++) {
                    byte block = ctx.getLocalBlock(x, y, z);

                    if (block == Blocks.ID_AIR)
                        continue;
                    if (block == Blocks.ID_SOLID)
                        meshCube(ctx, x, y, z, geom);
                    if (block == Blocks.ID_WATER)
                        meshLiquid(ctx, x, y, z, Colors.RGBA.fromFloats(0.3f, 0.3f, 1.0f, 0.6f), Blocks.ID_WATER, geom);
                    if (block == Blocks.ID_LAVA)
                        meshLiquid(ctx, x, y, z, Colors.RGBA.fromFloats(1.0f, 0.5f, 0.0f), Blocks.ID_LAVA, geom);
                }
            }
        }

        return geom;
    }

    private static final class CubeFaceTemplate {
        public final CubeFace face;
        private final Vector3f v1, v2, v3, v4;
        private final float faceShade;

        public CubeFaceTemplate(CubeFace face, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float faceShade) {
            this.face = face;
            this.v1 = new Vector3f(x1, y1, z1);
            this.v2 = new Vector3f(x2, y2, z2);
            this.v3 = new Vector3f(x3, y3, z3);
            this.v4 = new Vector3f(x4, y4, z4);
            this.faceShade = faceShade;
        }

        public BlockFace makeFace(int x, int y, int z, int color, float lightShade) {
            return new BlockFace(
                    new Vector3f(v1).add(x, y, z),
                    new Vector3f(v2).add(x, y, z),
                    new Vector3f(v3).add(x, y, z),
                    new Vector3f(v4).add(x, y, z),
                    color,
                    faceShade * lightShade
            );
        }
    }

    private static final CubeFaceTemplate[] CUBE_TEMPLATES = {
            new CubeFaceTemplate(CubeFace.POS_X, 1, 1, 1, 1, 0, 1, 1, 0, 0, 1, 1, 0, LightingConstants.SHADE_LEFT_RIGHT),
            new CubeFaceTemplate(CubeFace.NEG_X, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 1, 1, LightingConstants.SHADE_LEFT_RIGHT),
            new CubeFaceTemplate(CubeFace.POS_Y, 0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0, LightingConstants.SHADE_UP),
            new CubeFaceTemplate(CubeFace.NEG_Y, 1, 0, 0, 1, 0, 1, 0, 0, 1, 0, 0, 0, LightingConstants.SHADE_DOWN),
            new CubeFaceTemplate(CubeFace.POS_Z, 0, 1, 1, 0, 0, 1, 1, 0, 1, 1, 1, 1, LightingConstants.SHADE_FRONT_BACK),
            new CubeFaceTemplate(CubeFace.NEG_Z, 1, 1, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, LightingConstants.SHADE_FRONT_BACK)
    };

    private void meshCube(SectionContext ctx, int x, int y, int z, SectionGeometry geom) {
        for (CubeFaceTemplate faceTemplate : CUBE_TEMPLATES) {
            int nx = x + faceTemplate.face.x;
            int ny = y + faceTemplate.face.y;
            int nz = z + faceTemplate.face.z;

            Byte neighbor = ctx.getSurroundingBlock(nx, ny, nz);
            if (neighbor == null && faceTemplate.face != CubeFace.POS_Y)
                continue;
            if (neighbor != null && neighbor == Blocks.ID_SOLID)
                continue;

            boolean lit = ctx.isLit(nx, ny, nz);
            float lightShade = lit ? LightingConstants.SHADE_LIT : LightingConstants.SHADE_SHADOW;

            int color = Colors.RGBA.WHITE;
            geom.addOpaqueFace(faceTemplate.makeFace(x, y, z, color, lightShade));
        }
    }

    private static final float LIQUID_SURFACE_HEIGHT = 0.9f;
    private static final float LIQUID_INSET = 0.0015f; // To prevent Z-fighting on touching faces

    private static final class LiquidSideTemplate {
        public final CubeFace face;
        private final float x1, z1, x2, z2;
        private final float faceShade;

        public LiquidSideTemplate(CubeFace face, float x1, float z1, float x2, float z2, float faceShade) {
            this.face = face;
            this.x1 = x1;
            this.z1 = z1;
            this.x2 = x2;
            this.z2 = z2;
            this.faceShade = faceShade;
        }

        public BlockFace makeFace(int x, int y, int z, int color, float lightShade, float bottomY, float topY) {
            return new BlockFace(
                    new Vector3f(x + x1, y + topY, z + z1),
                    new Vector3f(x + x1, y + bottomY, z + z1),
                    new Vector3f(x + x2, y + bottomY, z + z2),
                    new Vector3f(x + x2, y + topY, z + z2),
                    color,
                    faceShade * lightShade
            );
        }
    }

    private static final LiquidSideTemplate[] LIQUID_SIDE_TEMPLATES = {
            new LiquidSideTemplate(CubeFace.POS_X, 1 - LIQUID_INSET, 1, 1 - LIQUID_INSET, 0, LightingConstants.SHADE_LEFT_RIGHT),
            new LiquidSideTemplate(CubeFace.NEG_X, LIQUID_INSET, 0, LIQUID_INSET, 1, LightingConstants.SHADE_LEFT_RIGHT),
            new LiquidSideTemplate(CubeFace.POS_Z, 0, 1 - LIQUID_INSET, 1, 1 - LIQUID_INSET, LightingConstants.SHADE_FRONT_BACK),
            new LiquidSideTemplate(CubeFace.NEG_Z, 1, LIQUID_INSET, 0, LIQUID_INSET, LightingConstants.SHADE_FRONT_BACK)
    };

    private void meshLiquid(SectionContext ctx, int x, int y, int z, int color, byte selfId, SectionGeometry geom) {
        Byte above = ctx.getSurroundingBlock(x, y + 1, z);
        boolean tall = above != null && above == selfId;

        float lightShade = ctx.isLit(x, y, z) ? LightingConstants.SHADE_LIT : LightingConstants.SHADE_SHADOW;

        if (!tall) {
            boolean surface = false;
            for (int j = -1; j <= 1; j++) {
                for (int i = -1; i <= 1; i++) {
                    Byte aboveNeighbor = ctx.getSurroundingBlock(x + i, y + 1, z + j);
                    if (aboveNeighbor == null || (aboveNeighbor != selfId && !Blocks.isOpaque(aboveNeighbor))) {
                        surface = true;
                        break;
                    }
                }
            }

            if (surface) {
                float h = LIQUID_SURFACE_HEIGHT;
                geom.addTranslucentFace(new BlockFace(
                        new Vector3f(x, y + h, z),
                        new Vector3f(x, y + h, z + 1),
                        new Vector3f(x + 1, y + h, z + 1),
                        new Vector3f(x + 1, y + h, z),
                        color,
                        lightShade * LightingConstants.SHADE_UP
                ));
            }
        }

        for (LiquidSideTemplate sideTemplate : LIQUID_SIDE_TEMPLATES) {
            int nx = x + sideTemplate.face.x;
            int nz = z + sideTemplate.face.z;

            Byte neighbor = ctx.getSurroundingBlock(nx, y, nz);
            if (neighbor == null)
                continue;

            if (neighbor != selfId && !Blocks.isOpaque(neighbor)) {
                float h = tall ? 1 : LIQUID_SURFACE_HEIGHT;
                geom.addTranslucentFace(sideTemplate.makeFace(x, y, z, color, lightShade, 0, h));
            } else if (tall && neighbor == selfId) {
                Byte aboveNeighbor = ctx.getSurroundingBlock(nx, y + 1, nz);
                boolean neighborTall = aboveNeighbor != null && aboveNeighbor == selfId;

                if (!neighborTall)
                    geom.addTranslucentFace(sideTemplate.makeFace(x, y, z, color, lightShade, LIQUID_SURFACE_HEIGHT, 1));
            }
        }

        Byte below = ctx.getSurroundingBlock(x, y - 1, z);
        if (below != null && below != selfId && !Blocks.isOpaque(below)) {
            geom.addTranslucentFace(new BlockFace(
                    new Vector3f(x + 1, y + LIQUID_INSET, z),
                    new Vector3f(x + 1, y + LIQUID_INSET, z + 1),
                    new Vector3f(x, y + LIQUID_INSET, z + 1),
                    new Vector3f(x, y + LIQUID_INSET, z),
                    color,
                    lightShade * LightingConstants.SHADE_DOWN
            ));
        }
    }

    @Override
    public void close() {
        shader.close();
        sharedIndexBuffer.close();
    }
}
