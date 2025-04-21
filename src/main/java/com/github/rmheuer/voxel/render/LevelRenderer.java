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
import org.joml.Matrix4fc;
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

    private static final float SHADE_UP = 1.0f;
    private static final float SHADE_FRONT_BACK = 0.9f;
    private static final float SHADE_LEFT_RIGHT = 0.8f;
    private static final float SHADE_DOWN = 0.7f;

    private static final float SHADE_LIT = 1.0f;
    private static final float SHADE_SHADOW = 0.7f;

    private final ShaderProgram shader;
    private final PipelineInfo pipeline;
    private final SharedIndexBuffer sharedIndexBuffer;

    public LevelRenderer(Renderer renderer) throws IOException {
        shader = renderer.createShaderProgram(
                ResourceUtil.readAsStream("vertex.glsl"),
                ResourceUtil.readAsStream("fragment.glsl")
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

    private static final class TranslucentSection {
        public final int blockX, blockY, blockZ;
        public final SectionRenderLayer layer;

        public TranslucentSection(int blockX, int blockY, int blockZ, SectionRenderLayer layer) {
            this.blockX = blockX;
            this.blockY = blockY;
            this.blockZ = blockZ;
            this.layer = layer;
        }
    }

    public void renderLevel(Renderer renderer, BlockMap blockMap, LightMap lightMap, LevelRenderData renderData, Vector3fc cameraPos, Matrix4fc viewProj, boolean wireframe) {
        int sectionsX = blockMap.getSectionsX();
        int sectionsY = blockMap.getSectionsY();
        int sectionsZ = blockMap.getSectionsZ();

        boolean cameraMoved = !renderData.getPrevCameraPos().equals(cameraPos);

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
                        try (SectionGeometry2 geom = createSectionGeometry2(blockMap, lightMap, sectionX, sectionY, sectionZ)) {
                            renderSection.getOpaqueLayer().updateMesh(renderer, geom.getOpaqueData());
                            renderSection.setTranslucentFaces(geom.getTranslucentFaces());
                            sharedIndexBuffer.ensureCapacity(geom.getRequiredFaceCount());
                            renderSection.clearOutdated();
                        }
                        updated++;
                        updateTranslucent = true;
                    }

                    if (updateTranslucent) {
                        List<BlockFace> translucentFaces = renderSection.getTranslucentFaces();
                        translucentFaces.sort(Comparator.comparingDouble((face) -> -cameraPos.distanceSquared(face.getCenterPos(ox, oy, oz))));

                        try (VertexData data = new VertexData(LAYOUT)) {
                            for (BlockFace face : translucentFaces) {
                                face.addToMesh(data);
                            }
                            renderSection.getTranslucentLayer().updateMesh(renderer, data);
                        }
                    }
                }
            }
        }
        if (updated > 0) {
            System.out.println("Updated " + updated + " section mesh(es)");
        }

        pipeline.setFillMode(wireframe ? FillMode.WIREFRAME : FillMode.FILLED);

        List<TranslucentSection> translucentToRender = new ArrayList<>();
        try (ActivePipeline pipe = renderer.bindPipeline(pipeline.setCullMode(CullMode.BACK))) {
            pipe.getUniform("u_ViewProj").setMat4(viewProj);

            ShaderUniform offsetUniform = pipe.getUniform("u_SectionOffset");
            IndexBuffer indexBuffer = sharedIndexBuffer.getIndexBuffer();

            for (int sectionY = 0; sectionY < sectionsY; sectionY++) {
                for (int sectionZ = 0; sectionZ < sectionsZ; sectionZ++) {
                    for (int sectionX = 0; sectionX < sectionsX; sectionX++) {
                        SectionRenderData section = renderData.getSection(sectionX, sectionY, sectionZ);
                        SectionRenderLayer opaque = section.getOpaqueLayer();
                        SectionRenderLayer translucent = section.getTranslucentLayer();

                        if (translucent.getElementCount() > 0) {
                            translucentToRender.add(new TranslucentSection(
                                    sectionX * MapSection.SIZE,
                                    sectionY * MapSection.SIZE,
                                    sectionZ * MapSection.SIZE,
                                    translucent
                            ));
                        }

                        if (opaque.getElementCount() > 0) {
                            offsetUniform.setVec3(
                                    sectionX * MapSection.SIZE,
                                    sectionY * MapSection.SIZE,
                                    sectionZ * MapSection.SIZE
                            );
                            pipe.draw(opaque.getVertexBuffer(), indexBuffer, 0, opaque.getElementCount());
                        }
                    }
                }
            }
        }

        try (ActivePipeline pipe = renderer.bindPipeline(pipeline.setCullMode(CullMode.OFF))) {
            pipe.getUniform("u_ViewProj").setMat4(viewProj);

            ShaderUniform offsetUniform = pipe.getUniform("u_SectionOffset");
            IndexBuffer indexBuffer = sharedIndexBuffer.getIndexBuffer();

            int halfSz = MapSection.SIZE / 2;
            translucentToRender.sort(Comparator.comparingDouble((section) -> -cameraPos.distanceSquared(
                    section.blockX + halfSz,
                    section.blockY + halfSz,
                    section.blockZ + halfSz
            )));
            for (TranslucentSection section : translucentToRender) {
                offsetUniform.setVec3(section.blockX, section.blockY, section.blockZ);
                pipe.draw(section.layer.getVertexBuffer(), indexBuffer, 0, section.layer.getElementCount());
            }
        }

        renderData.setPrevCameraPos(cameraPos);
    }

    private boolean shouldDraw(byte block, byte neighbor) {
        return neighbor != block && neighbor != Blocks.ID_SOLID;
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
            System.out.println(x + ", " + y + ", " + z);
            return lightMap.isLit(originX + x, originY + y, originZ + z);
        }
    }

    private static final class SectionGeometry2 implements SafeCloseable {
        private final VertexData opaqueData;
        private final List<BlockFace> translucentFaces;

        public SectionGeometry2() {
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

    private SectionGeometry2 createSectionGeometry2(BlockMap blockMap, LightMap lightMap, int sectionX, int sectionY, int sectionZ) {
        SectionGeometry2 geom = new SectionGeometry2();
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
                        meshWater(ctx, x, y, z, geom);
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
            new CubeFaceTemplate(CubeFace.POS_X, 1, 1, 1, 1, 0, 1, 1, 0, 0, 1, 1, 0, SHADE_LEFT_RIGHT),
            new CubeFaceTemplate(CubeFace.NEG_X, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 1, 1, SHADE_LEFT_RIGHT),
            new CubeFaceTemplate(CubeFace.POS_Y, 0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0, SHADE_UP),
            new CubeFaceTemplate(CubeFace.NEG_Y, 1, 0, 0, 1, 0, 1, 0, 0, 1, 0, 0, 0, SHADE_DOWN),
            new CubeFaceTemplate(CubeFace.POS_Z, 0, 1, 1, 0, 0, 1, 1, 0, 1, 1, 1, 1, SHADE_FRONT_BACK),
            new CubeFaceTemplate(CubeFace.NEG_Z, 1, 1, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, SHADE_FRONT_BACK)
    };

    private void meshCube(SectionContext ctx, int x, int y, int z, SectionGeometry2 geom) {
        for (CubeFaceTemplate faceTemplate : CUBE_TEMPLATES) {
            int nx = x + faceTemplate.face.x;
            int ny = y + faceTemplate.face.y;
            int nz = z + faceTemplate.face.z;

            Byte neighbor = ctx.getSurroundingBlock(nx, ny, nz);
            if (neighbor == null && faceTemplate.face != CubeFace.POS_Y)
                continue;
            if (neighbor != null && neighbor == Blocks.ID_SOLID)
                continue;

            System.out.println(faceTemplate.face + " and neighbor " + neighbor);
            System.out.println(ctx.originX + " " + ctx.originY + " " + ctx.originZ);
            boolean lit = ctx.isLit(nx, ny, nz);
            float lightShade = lit ? SHADE_LIT : SHADE_SHADOW;

            int color = Colors.RGBA.WHITE;
            geom.addOpaqueFace(faceTemplate.makeFace(x, y, z, color, lightShade));
        }
    }

    private static final float WATER_SURFACE_HEIGHT = 0.9f;

    private static final class WaterSideTemplate {
        public final CubeFace face;
        private final float x1, z1, x2, z2;
        private final float faceShade;

        public WaterSideTemplate(CubeFace face, float x1, float z1, float x2, float z2, float faceShade) {
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

    private static final WaterSideTemplate[] WATER_SIDE_TEMPLATES = {
            new WaterSideTemplate(CubeFace.POS_X, 1, 1, 1, 0, SHADE_LEFT_RIGHT),
            new WaterSideTemplate(CubeFace.NEG_X, 0, 0, 0, 1, SHADE_LEFT_RIGHT),
            new WaterSideTemplate(CubeFace.POS_Z, 0, 1, 1, 1, SHADE_FRONT_BACK),
            new WaterSideTemplate(CubeFace.NEG_Z, 1, 0, 0, 0, SHADE_FRONT_BACK)
    };

    private void meshWater(SectionContext ctx, int x, int y, int z, SectionGeometry2 geom) {
        Byte above = ctx.getSurroundingBlock(x, y + 1, z);
        boolean tall = above != null && above == Blocks.ID_WATER;

        int color = Colors.RGBA.fromFloats(0.3f, 0.3f, 1.0f, 0.6f);
        float lightShade = ctx.isLit(x, y, z) ? SHADE_LIT : SHADE_SHADOW;

        if (!tall) {
            boolean surface = false;
            for (int j = -1; j <= 1; j++) {
                for (int i = -1; i <= 1; i++) {
                    Byte aboveNeighbor = ctx.getSurroundingBlock(x + i, y + 1, z + j);
                    if (aboveNeighbor == null || aboveNeighbor == Blocks.ID_AIR) {
                        surface = true;
                        break;
                    }
                }
            }

            if (surface) {
                float h = WATER_SURFACE_HEIGHT;
                geom.addTranslucentFace(new BlockFace(
                        new Vector3f(x, y + h, z),
                        new Vector3f(x, y + h, z + 1),
                        new Vector3f(x + 1, y + h, z + 1),
                        new Vector3f(x + 1, y + h, z),
                        color,
                        lightShade * SHADE_UP
                ));
            }
        }

        for (WaterSideTemplate sideTemplate : WATER_SIDE_TEMPLATES) {
            int nx = x + sideTemplate.face.x;
            int nz = z + sideTemplate.face.z;

            Byte neighbor = ctx.getSurroundingBlock(nx, y, nz);
            if (neighbor == null)
                continue;

            if (neighbor == Blocks.ID_AIR) {
                float h = tall ? 1 : WATER_SURFACE_HEIGHT;
                geom.addTranslucentFace(sideTemplate.makeFace(x, y, z, color, lightShade, 0, h));
            } else if (tall && neighbor == Blocks.ID_WATER) {
                Byte aboveNeighbor = ctx.getSurroundingBlock(nx, y + 1, nz);
                boolean neighborTall = aboveNeighbor != null && aboveNeighbor == Blocks.ID_WATER;

                if (!neighborTall)
                    geom.addTranslucentFace(sideTemplate.makeFace(x, y, z, color, lightShade, WATER_SURFACE_HEIGHT, 1));
            }
        }

        Byte below = ctx.getSurroundingBlock(x, y - 1, z);
        if (below != null && below == Blocks.ID_AIR) {
            geom.addTranslucentFace(new BlockFace(
                    new Vector3f(x + 1, y, z),
                    new Vector3f(x + 1, y, z + 1),
                    new Vector3f(x, y, z + 1),
                    new Vector3f(x, y, z),
                    color,
                    lightShade * SHADE_DOWN
            ));
        }
    }

    private SectionGeometry createSectionGeometry(BlockMap blockMap, LightMap lightMap, int sectionX, int sectionY, int sectionZ) {
        MapSection section = blockMap.getSection(sectionX, sectionY, sectionZ);
        VertexData opaqueData = new VertexData(LAYOUT);
        List<WaterFace> waterFaces = new ArrayList<>();

        if (section.isEmpty())
            return new SectionGeometry(opaqueData, waterFaces);

        MapSection sectionNX = sectionX > 0 ? blockMap.getSection(sectionX - 1, sectionY, sectionZ) : null;
        MapSection sectionNY = sectionY > 0 ? blockMap.getSection(sectionX, sectionY - 1, sectionZ) : null;
        MapSection sectionNZ = sectionZ > 0 ? blockMap.getSection(sectionX, sectionY, sectionZ - 1) : null;
        MapSection sectionPX = sectionX < blockMap.getSectionsX() - 1 ? blockMap.getSection(sectionX + 1, sectionY, sectionZ) : null;
        MapSection sectionPY = sectionY < blockMap.getSectionsY() - 1 ? blockMap.getSection(sectionX, sectionY + 1, sectionZ) : null;
        MapSection sectionPZ = sectionZ < blockMap.getSectionsZ() - 1 ? blockMap.getSection(sectionX, sectionY, sectionZ + 1) : null;

        MapSection sectionPYPX = sectionPY != null && sectionPX != null ? blockMap.getSection(sectionX + 1, sectionY + 1, sectionZ) : null;
        MapSection sectionPYPZ = sectionPY != null && sectionPZ != null ? blockMap.getSection(sectionX, sectionY + 1, sectionZ + 1) : null;
        MapSection sectionPYNX = sectionPY != null && sectionNX != null ? blockMap.getSection(sectionX - 1, sectionY + 1, sectionZ) : null;
        MapSection sectionPYNZ = sectionPY != null && sectionNZ != null ? blockMap.getSection(sectionX, sectionY + 1, sectionZ - 1) : null;

        int ox = sectionX * MapSection.SIZE;
        int oy = sectionY * MapSection.SIZE;
        int oz = sectionZ * MapSection.SIZE;

        for (int y = 0; y < MapSection.SIZE; y++) {
            for (int z = 0; z < MapSection.SIZE; z++) {
                for (int x = 0; x < MapSection.SIZE; x++) {
                    byte block = section.getBlockId(x, y, z);
                    if (block == Blocks.ID_AIR)
                        continue;

                    Byte blockNX = x > 0
                            ? Byte.valueOf(section.getBlockId(x - 1, y, z))
                            : (sectionNX != null ? sectionNX.getBlockId(MapSection.SIZE - 1, y, z) : null);
                    Byte blockNY = y > 0
                            ? Byte.valueOf(section.getBlockId(x, y - 1, z))
                            : (sectionNY != null ? sectionNY.getBlockId(x, MapSection.SIZE - 1, z) : null);
                    Byte blockNZ = z > 0
                            ? Byte.valueOf(section.getBlockId(x, y, z - 1))
                            : (sectionNZ != null ? sectionNZ.getBlockId(x, y, MapSection.SIZE - 1) : null);
                    Byte blockPX = x < MapSection.SIZE - 1
                            ? Byte.valueOf(section.getBlockId(x + 1, y, z))
                            : (sectionPX != null ? sectionPX.getBlockId(0, y, z) : null);
                    Byte blockPY = y < MapSection.SIZE - 1
                            ? Byte.valueOf(section.getBlockId(x, y + 1, z))
                            : (sectionPY != null ? sectionPY.getBlockId(x, 0, z) : null);
                    Byte blockPZ = z < MapSection.SIZE - 1
                            ? Byte.valueOf(section.getBlockId(x, y, z + 1))
                            : (sectionPZ != null ? sectionPZ.getBlockId(x, y, 0) : null);

                    int blockX = ox + x;
                    int blockY = oy + y;
                    int blockZ = oz + z;

                    boolean drawNX = blockNX != null && shouldDraw(block, blockNX);
                    boolean drawNY = blockNY != null && shouldDraw(block, blockNY);
                    boolean drawNZ = blockNZ != null && shouldDraw(block, blockNZ);
                    boolean drawPX = blockPX != null && shouldDraw(block, blockPX);
                    boolean drawPY = blockPY == null || shouldDraw(block, blockPY);
                    boolean drawPZ = blockPZ != null && shouldDraw(block, blockPZ);

                    if (block == Blocks.ID_SOLID) {
                        int color = Colors.RGBA.WHITE;

                        if (drawNX)
                            meshFace(opaqueData, lightMap, x, y, z, blockX, blockY, blockZ, CubeFace.NEG_X, color);
                        if (drawPX)
                            meshFace(opaqueData, lightMap, x, y, z, blockX, blockY, blockZ, CubeFace.POS_X, color);
                        if (drawNY)
                            meshFace(opaqueData, lightMap, x, y, z, blockX, blockY, blockZ, CubeFace.NEG_Y, color);
                        if (drawPY)
                            meshFace(opaqueData, lightMap, x, y, z, blockX, blockY, blockZ, CubeFace.POS_Y, color);
                        if (drawNZ)
                            meshFace(opaqueData, lightMap, x, y, z, blockX, blockY, blockZ, CubeFace.NEG_Z, color);
                        if (drawPZ)
                            meshFace(opaqueData, lightMap, x, y, z, blockX, blockY, blockZ, CubeFace.POS_Z, color);
                    } else if (block == Blocks.ID_WATER) {
                        float shortHeight = 14 / 16.0f;

                        boolean tall = blockPY != null && blockPY == Blocks.ID_WATER;

                        if (drawNY)
                            waterFaces.add(new WaterFace(x, y, z, CubeFace.NEG_Y, 0, 1, 0));
                        if (!tall)
                            waterFaces.add(new WaterFace(x, y, z, CubeFace.POS_Y, 0, 1, 1 - shortHeight));

                        Byte blockPYPX;
                        if (y < MapSection.SIZE - 1) {
                            blockPYPX = x < MapSection.SIZE - 1
                                    ? Byte.valueOf(section.getBlockId(x + 1, y + 1, z))
                                    : (sectionPX != null ? sectionPX.getBlockId(0, y + 1, z) : null);
                        } else {
                            blockPYPX = x < MapSection.SIZE - 1
                                    ? Byte.valueOf(section.getBlockId(x + 1, 0, z))
                                    : (sectionPYPX != null ? sectionPYPX.getBlockId(0, 0, z) : null);
                        }
                        if (blockPX != null && blockPX != Blocks.ID_SOLID) {
                            if (blockPX == Blocks.ID_WATER) {
                                boolean neighborTall = blockPYPX != null && blockPYPX == Blocks.ID_WATER;

                                if (tall && !neighborTall) {
                                    waterFaces.add(new WaterFace(x, y, z, CubeFace.POS_X, shortHeight, 1, 0));
                                }
                            } else {
                                if (tall)
                                    waterFaces.add(new WaterFace(x, y, z, CubeFace.POS_X, 0, 1, 0));
                                else
                                    waterFaces.add(new WaterFace(x, y, z, CubeFace.POS_X, 0, shortHeight, 0));
                            }
                        }

                        Byte blockPYNX;
                        if (y < MapSection.SIZE - 1) {
                            blockPYNX = x > 0
                                    ? Byte.valueOf(section.getBlockId(x - 1, y + 1, z))
                                    : (sectionNX != null ? sectionNX.getBlockId(MapSection.SIZE - 1, y + 1, z) : null);
                        } else {
                            blockPYNX = x > 0
                                    ? Byte.valueOf(section.getBlockId(x - 1, 0, z))
                                    : (sectionPYNX != null ? sectionPYNX.getBlockId(MapSection.SIZE - 1, 0, z) : null);
                        }
                        if (blockNX != null && blockNX != Blocks.ID_SOLID) {
                            if (blockNX == Blocks.ID_WATER) {
                                boolean neighborTall = blockPYNX != null && blockPYNX == Blocks.ID_WATER;

                                if (tall && !neighborTall) {
                                    waterFaces.add(new WaterFace(x, y, z, CubeFace.NEG_X, shortHeight, 1, 0));
                                }
                            } else {
                                if (tall)
                                    waterFaces.add(new WaterFace(x, y, z, CubeFace.NEG_X, 0, 1, 0));
                                else
                                    waterFaces.add(new WaterFace(x, y, z, CubeFace.NEG_X, 0, shortHeight, 0));
                            }
                        }

                        Byte blockPYPZ;
                        if (y < MapSection.SIZE - 1) {
                            blockPYPZ = z < MapSection.SIZE - 1
                                    ? Byte.valueOf(section.getBlockId(x, y + 1, z + 1))
                                    : (sectionPZ != null ? sectionPZ.getBlockId(x, y + 1, 0) : null);
                        } else {
                            blockPYPZ = z < MapSection.SIZE - 1
                                    ? Byte.valueOf(section.getBlockId(x, 0, z + 1))
                                    : (sectionPYPZ != null ? sectionPYPZ.getBlockId(x, 0, 0) : null);
                        }
                        if (blockPZ != null && blockPZ != Blocks.ID_SOLID) {
                            if (blockPZ == Blocks.ID_WATER) {
                                boolean neighborTall = blockPYPZ != null && blockPYPZ == Blocks.ID_WATER;

                                if (tall && !neighborTall) {
                                    waterFaces.add(new WaterFace(x, y, z, CubeFace.POS_Z, shortHeight, 1, 0));
                                }
                            } else {
                                if (tall)
                                    waterFaces.add(new WaterFace(x, y, z, CubeFace.POS_Z, 0, 1, 0));
                                else
                                    waterFaces.add(new WaterFace(x, y, z, CubeFace.POS_Z, 0, shortHeight, 0));
                            }
                        }

                        Byte blockPYNZ;
                        if (y < MapSection.SIZE - 1) {
                            blockPYNZ = z > 0
                                    ? Byte.valueOf(section.getBlockId(x, y + 1, z - 1))
                                    : (sectionNZ != null ? sectionNZ.getBlockId(x, y + 1, MapSection.SIZE - 1) : null);
                        } else {
                            blockPYNZ = z > 0
                                    ? Byte.valueOf(section.getBlockId(x, 0, z - 1))
                                    : (sectionPYNZ != null ? sectionPYNZ.getBlockId(x, 0, MapSection.SIZE - 1) : null);
                        }
                        if (blockNZ != null && blockNZ != Blocks.ID_SOLID) {
                            if (blockNZ == Blocks.ID_WATER) {
                                boolean neighborTall = blockPYNZ != null && blockPYNZ == Blocks.ID_WATER;

                                if (tall && !neighborTall) {
                                    waterFaces.add(new WaterFace(x, y, z, CubeFace.NEG_Z, shortHeight, 1, 0));
                                }
                            } else {
                                if (tall)
                                    waterFaces.add(new WaterFace(x, y, z, CubeFace.NEG_Z, 0, 1, 0));
                                else
                                    waterFaces.add(new WaterFace(x, y, z, CubeFace.NEG_Z, 0, shortHeight, 0));
                            }
                        }
                    }
                }
            }
        }

        return new SectionGeometry(opaqueData, waterFaces);
    }

    private void meshFace(VertexData data, LightMap lightMap, int x, int y, int z, int blockX, int blockY, int blockZ, CubeFace face, int color) {
        meshFace(data, lightMap, x, y, z, blockX, blockY, blockZ, face, 0, 1, 0, color);
    }

    // TODO: Make this method not horrible
    private void meshFace(VertexData data, LightMap lightMap, int x, int y, int z, int blockX, int blockY, int blockZ, CubeFace face, float botH, float topH, float depth, int color) {
        float x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4;
        float faceShade;
        switch (face) {
            case POS_X:
                x1 = x + 1 - depth; y1 = y + topH; z1 = z + 1;
                x2 = x + 1 - depth; y2 = y + botH; z2 = z + 1;
                x3 = x + 1 - depth; y3 = y + botH; z3 = z;
                x4 = x + 1 - depth; y4 = y + topH; z4 = z;
                faceShade = SHADE_LEFT_RIGHT;
                break;
            case NEG_X:
                x1 = x + depth; y1 = y + topH; z1 = z;
                x2 = x + depth; y2 = y + botH; z2 = z;
                x3 = x + depth; y3 = y + botH; z3 = z + 1;
                x4 = x + depth; y4 = y + topH; z4 = z + 1;
                faceShade = SHADE_LEFT_RIGHT;
                break;
            case POS_Y:
                x1 = x;     y1 = y + 1 - depth; z1 = z;
                x2 = x;     y2 = y + 1 - depth; z2 = z + 1;
                x3 = x + 1; y3 = y + 1 - depth; z3 = z + 1;
                x4 = x + 1; y4 = y + 1 - depth; z4 = z;
                faceShade = SHADE_UP;
                break;
            case NEG_Y:
                x1 = x + 1; y1 = y + depth; z1 = z;
                x2 = x + 1; y2 = y + depth; z2 = z + 1;
                x3 = x;     y3 = y + depth; z3 = z + 1;
                x4 = x;     y4 = y + depth; z4 = z;
                faceShade = SHADE_DOWN;
                break;
            case POS_Z:
                x1 = x;     y1 = y + topH; z1 = z + 1 - depth;
                x2 = x;     y2 = y + botH; z2 = z + 1 - depth;
                x3 = x + 1; y3 = y + botH; z3 = z + 1 - depth;
                x4 = x + 1; y4 = y + topH; z4 = z + 1 - depth;
                faceShade = SHADE_FRONT_BACK;
                break;
            case NEG_Z:
                x1 = x + 1; y1 = y + topH; z1 = z + depth;
                x2 = x + 1; y2 = y + botH; z2 = z + depth;
                x3 = x;     y3 = y + botH; z3 = z + depth;
                x4 = x;     y4 = y + topH; z4 = z + depth;
                faceShade = SHADE_FRONT_BACK;
                break;
            default:
                throw new IllegalArgumentException();
        }

        float lightShade = lightMap.isLit(blockX + face.x, blockY + face.y, blockZ + face.z)
                ? SHADE_LIT
                : SHADE_SHADOW;

        float shade = faceShade * lightShade;
        data.putVec3(x1, y1, z1); data.putColorRGBA(color); data.putFloat(shade);
        data.putVec3(x2, y2, z2); data.putColorRGBA(color); data.putFloat(shade);
        data.putVec3(x3, y3, z3); data.putColorRGBA(color); data.putFloat(shade);
        data.putVec3(x4, y4, z4); data.putColorRGBA(color); data.putFloat(shade);
    }

    @Override
    public void close() {
        shader.close();
        sharedIndexBuffer.close();
    }
}
