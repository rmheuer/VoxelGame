package com.github.rmheuer.voxel.client.render;

import com.github.rmheuer.azalea.io.ResourceUtil;
import com.github.rmheuer.azalea.math.CubeFace;
import com.github.rmheuer.azalea.render.Colors;
import com.github.rmheuer.azalea.render.Renderer;
import com.github.rmheuer.azalea.render.mesh.*;
import com.github.rmheuer.azalea.render.pipeline.*;
import com.github.rmheuer.azalea.render.shader.ShaderProgram;
import com.github.rmheuer.azalea.render.shader.ShaderUniform;
import com.github.rmheuer.azalea.render.texture.Texture2D;
import com.github.rmheuer.azalea.render.utils.DebugLineRenderer;
import com.github.rmheuer.azalea.render.utils.SharedIndexBuffer;
import com.github.rmheuer.azalea.utils.SafeCloseable;
import com.github.rmheuer.voxel.block.Block;
import com.github.rmheuer.voxel.block.Blocks;
import com.github.rmheuer.voxel.block.OcclusionType;
import com.github.rmheuer.voxel.level.*;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3fc;
import org.joml.Vector3i;

import java.io.IOException;
import java.util.*;

/**
 * Handles all of the rendering for the level.
 */
public final class LevelRenderer implements SafeCloseable {
    private final Texture2D atlasTexture;

    private final ShaderProgram shader;
    private final PipelineInfo pipeline;
    private final SharedIndexBuffer sharedIndexBuffer;
    private final VisibilityCalculator visibilityCalc;

    /**
     * @param renderer renderer to create resources with
     * @param atlasTexture block atlas texture
     */
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

        visibilityCalc = new VisibilityCalculator();
    }

    // Section layer to render and where to render it
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

    /**
     * Stores the information about what to render this frame. This is needed in
     * order to render the opaque and translucent layers separately while only
     * processing the level sections once per frame.
     */
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

        /**
         * Renders the opaque layer of the level.
         *
         * @param renderer renderer to render with
         * @param view camera view matrix
         * @param proj camera projection matrix
         * @param fogInfo information for distance fog
         */
        public void renderOpaqueLayer(Renderer renderer, Matrix4f view, Matrix4f proj, FogInfo fogInfo) {
            renderLayer(renderer, view, proj, fogInfo, opaqueToRender);
        }

        /**
         * Renders the translucent layer of the level.
         *
         * @param renderer renderer to render with
         * @param view camera view matrix
         * @param proj camera projection matrix
         * @param fogInfo information for distance fog
         */
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

        public int getOpaqueCount() {
            return opaqueToRender.size();
        }

        public int getTranslucentCount() {
            return translucentToRender.size();
        }
    }

    /**
     * Renders lines to show the visibility connections for each section.
     *
     * @param r line renderer to render with
     * @param renderData level render data to render
     */
    public void renderVisibilityDebug(DebugLineRenderer r, LevelRenderData renderData) {
        for (int sectionY = 0; sectionY < renderData.getSectionsY(); sectionY++) {
            for (int sectionZ = 0; sectionZ < renderData.getSectionsZ(); sectionZ++) {
                for (int sectionX = 0; sectionX < renderData.getSectionsX(); sectionX++) {
                    SectionRenderData section = renderData.getSection(sectionX, sectionY, sectionZ);
                    SectionVisibility vis = section.getVisibility();
                    if (vis == null)
                        continue;

                    for (CubeFace face : CubeFace.values()) {
                        for (CubeFace face2 : CubeFace.values()) {
                            if (vis.isVisible(face, face2)) {
                                int x1 = sectionX * 16 + 8 + 8 * face.x;
                                int y1 = sectionY * 16 + 8 + 8 * face.y;
                                int z1 = sectionZ * 16 + 8 + 8 * face.z;
                                int x2 = sectionX * 16 + 8 + 8 * face2.x;
                                int y2 = sectionY * 16 + 8 + 8 * face2.y;
                                int z2 = sectionZ * 16 + 8 + 8 * face2.z;

                                r.addLine(x1, y1, z1, x2, y2, z2, Colors.RGBA.RED);
                            }
                        }
                    }
                }
            }
        }
    }

    private static final class VisNode {
        public final int x, y, z;
        public final CubeFace cameFrom;
        public final FaceSet backwards;

        public VisNode(int x, int y, int z, CubeFace cameFrom, FaceSet backwards) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.cameFrom = cameFrom;
            this.backwards = backwards;
        }
    }


    /**
     * Determines what should be rendered this frame.
     *
     * @param renderer renderer to render with
     * @param blockMap block map to render
     * @param lightMap light map for lighting
     * @param renderData level render data to render
     * @param viewProj combined camera view and projection matrices
     * @param cameraPos position of the camera in the level
     * @param wireframe whether to render the level as a wireframe mesh
     *
     * @return prepared render information to be rendered later
     */
    public PreparedRender prepareRender(Renderer renderer, BlockMap blockMap, LightMap lightMap, LevelRenderData renderData, Matrix4f viewProj, Vector3fc cameraPos, boolean wireframe) {
        int sectionsX = blockMap.getSectionsX();
        int sectionsY = blockMap.getSectionsY();
        int sectionsZ = blockMap.getSectionsZ();

        boolean cameraMoved = !renderData.getPrevCameraPos().equals(cameraPos);
        renderData.setPrevCameraPos(cameraPos);

        int cameraX = (int) Math.floor(cameraPos.x() / MapSection.SIZE);
        int cameraY = (int) Math.floor(cameraPos.y() / MapSection.SIZE);
        int cameraZ = (int) Math.floor(cameraPos.z() / MapSection.SIZE);
        
        BitSet visited = new BitSet(sectionsX * sectionsY * sectionsZ);
        Queue<VisNode> frontier = new ArrayDeque<>();

        frontier.add(new VisNode(cameraX, cameraY, cameraZ, null, FaceSet.none()));

        List<RenderSection> opaqueToRender = new ArrayList<>();
        List<RenderSection> translucentToRender = new ArrayList<>();

        FrustumIntersection frustum = new FrustumIntersection(viewProj, false);

        // TODO: Clean up
        long startTime = System.currentTimeMillis();
        VisNode node;
        while ((node = frontier.poll()) != null) {
            if (node.x < 0 || node.x >= sectionsX)
                continue;
            if (node.y < 0 || node.y >= sectionsY)
                continue;
            if (node.z < 0 || node.z >= sectionsZ)
                continue;

            int index = node.x + node.z * sectionsX + node.y * sectionsX * sectionsZ;
            if (visited.get(index))
                continue;
            visited.set(index);

            int ox = node.x * MapSection.SIZE;
            int oy = node.y * MapSection.SIZE;
            int oz = node.z * MapSection.SIZE;

            // Don't test first section to prevent culling the entire world
            if (node.cameFrom != null && !frustum.testAab(ox, oy, oz, ox + MapSection.SIZE, oy + MapSection.SIZE, oz + MapSection.SIZE))
                continue;

            SectionRenderData renderSection = renderData.getSection(node.x, node.y, node.z);

            if (renderSection.isVisibilityOutdated()) {
                MapSection mapSection = blockMap.getSection(node.x, node.y, node.z);
                renderSection.updateVisibility(visibilityCalc.calculate(mapSection));
            }
            
            if (!node.backwards.containsFace(CubeFace.POS_X) && (node.cameFrom == null || renderSection.canSeeThrough(node.cameFrom, CubeFace.POS_X))) {
                FaceSet backwards = new FaceSet(node.backwards);
                backwards.addFace(CubeFace.NEG_X);
                frontier.add(new VisNode(node.x + 1, node.y, node.z, CubeFace.NEG_X, backwards));
            }
            if (!node.backwards.containsFace(CubeFace.NEG_X) && (node.cameFrom == null || renderSection.canSeeThrough(node.cameFrom, CubeFace.NEG_X))) {
                FaceSet backwards = new FaceSet(node.backwards);
                backwards.addFace(CubeFace.POS_X);
                frontier.add(new VisNode(node.x - 1, node.y, node.z, CubeFace.POS_X, backwards));
            }
            if (!node.backwards.containsFace(CubeFace.POS_Y) && (node.cameFrom == null || renderSection.canSeeThrough(node.cameFrom, CubeFace.POS_Y))) {
                FaceSet backwards = new FaceSet(node.backwards);
                backwards.addFace(CubeFace.NEG_Y);
                frontier.add(new VisNode(node.x, node.y + 1, node.z, CubeFace.NEG_Y, backwards));
            }
            if (!node.backwards.containsFace(CubeFace.NEG_Y) && (node.cameFrom == null || renderSection.canSeeThrough(node.cameFrom, CubeFace.NEG_Y))) {
                FaceSet backwards = new FaceSet(node.backwards);
                backwards.addFace(CubeFace.POS_Y);
                frontier.add(new VisNode(node.x, node.y - 1, node.z, CubeFace.POS_Y, backwards));
            }
            if (!node.backwards.containsFace(CubeFace.POS_Z) && (node.cameFrom == null || renderSection.canSeeThrough(node.cameFrom, CubeFace.POS_Z))) {
                FaceSet backwards = new FaceSet(node.backwards);
                backwards.addFace(CubeFace.NEG_Z);
                frontier.add(new VisNode(node.x, node.y, node.z + 1, CubeFace.NEG_Z, backwards));
            }
            if (!node.backwards.containsFace(CubeFace.NEG_Z) && (node.cameFrom == null || renderSection.canSeeThrough(node.cameFrom, CubeFace.NEG_Z))) {
                FaceSet backwards = new FaceSet(node.backwards);
                backwards.addFace(CubeFace.POS_Z);
                frontier.add(new VisNode(node.x, node.y, node.z - 1, CubeFace.POS_Z, backwards));
            }

            if (System.currentTimeMillis() - startTime < 5) {
                boolean updateTranslucent = cameraMoved;
                if (renderSection.isMeshOutdated()) {
                    try (SectionGeometry geom = createSectionGeometry(blockMap, lightMap, node.x, node.y, node.z)) {
                        renderSection.getOpaqueLayer().updateMesh(renderer, geom.getOpaqueData());
                        renderSection.setTranslucentFaces(geom.getTranslucentFaces());
                        sharedIndexBuffer.ensureCapacity(geom.getRequiredFaceCount());
                        renderSection.clearOutdated();
                    }
                    //updated++;
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

        Collections.reverse(translucentToRender);

        pipeline.setFillMode(wireframe ? FillMode.WIREFRAME : FillMode.FILLED);

        return new PreparedRender(pipeline, sharedIndexBuffer.getIndexBuffer(), atlasTexture, opaqueToRender, translucentToRender);
    }

    // Creates the updated render geometry for one level section
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

    // Helper to calculate the visibility info for a level section
    private static final class VisibilityCalculator {
        private static final CubeFace[] FACES = CubeFace.values();
        
        private final BitSet visited;
        private final Queue<Vector3i> frontier;
        private final FaceSet touchedFaces;

        private MapSection section;
        private SectionVisibility visibility;

        public VisibilityCalculator() {
            visited = new BitSet(MapSection.SIZE_CUBED);
            frontier = new ArrayDeque<>();
            touchedFaces = FaceSet.none();
        }

        private void checkNeighbor(Vector3i pos, Block atPos, CubeFace dir) {
            int nx = pos.x + dir.x;
            int ny = pos.y + dir.y;
            int nz = pos.z + dir.z;
            Block neighbor = Blocks.getBlock(section.getBlockId(nx, ny, nz));
            if (neighbor.getOcclusion(dir.getReverse()) == OcclusionType.FULL)
                return;

            int index = nx + nz * MapSection.SIZE + ny * MapSection.SIZE_SQUARED;
            if (visited.get(index))
                return;

            visited.set(index);
            frontier.add(new Vector3i(nx, ny, nz));
        }

        private void floodFrom(Vector3i start) {
            touchedFaces.clear();
            frontier.clear();
            frontier.add(start);

            Vector3i pos;
            while ((pos = frontier.poll()) != null) {
                Block at = Blocks.getBlock(section.getBlockId(pos.x, pos.y, pos.z));

                if (at.getOcclusion(CubeFace.NEG_X) != OcclusionType.FULL) {
                    if (pos.x - 1 < 0)
                        touchedFaces.addFace(CubeFace.NEG_X);
                    else
                        checkNeighbor(pos, at, CubeFace.NEG_X);
                }
                if (at.getOcclusion(CubeFace.POS_X) != OcclusionType.FULL) {
                    if (pos.x + 1 >= MapSection.SIZE)
                        touchedFaces.addFace(CubeFace.POS_X);
                    else
                        checkNeighbor(pos, at, CubeFace.POS_X);
                }

                if (at.getOcclusion(CubeFace.NEG_Y) != OcclusionType.FULL) {
                    if (pos.y - 1 < 0)
                        touchedFaces.addFace(CubeFace.NEG_Y);
                    else
                        checkNeighbor(pos, at, CubeFace.NEG_Y);
                }
                if (at.getOcclusion(CubeFace.POS_Y) != OcclusionType.FULL) {
                    if (pos.y + 1 >= MapSection.SIZE)
                        touchedFaces.addFace(CubeFace.POS_Y);
                    else
                        checkNeighbor(pos, at, CubeFace.POS_Y);
                }

                if (at.getOcclusion(CubeFace.NEG_Z) != OcclusionType.FULL) {
                    if (pos.z - 1 < 0)
                        touchedFaces.addFace(CubeFace.NEG_Z);
                    else
                        checkNeighbor(pos, at, CubeFace.NEG_Z);
                }
                if (at.getOcclusion(CubeFace.POS_Z) != OcclusionType.FULL) {
                    if (pos.z + 1 >= MapSection.SIZE)
                        touchedFaces.addFace(CubeFace.POS_Z);
                    else
                        checkNeighbor(pos, at, CubeFace.POS_Z);
                }
                
                if (touchedFaces.isAll())
                    break;
            }

            if (touchedFaces.isNone())
                return;

            for (CubeFace from : FACES) {
                if (touchedFaces.containsFace(from)) {
                    for (CubeFace to : FACES) {
                        if (to == from)
                            continue;

                        if (touchedFaces.containsFace(to)) {
                            visibility.setVisible(from, to);

                            if (visibility.isAll())
                                return;
                        }
                    }
                }
            }
        }

        public SectionVisibility calculate(MapSection section) {
            if (section.isEmpty())
                return SectionVisibility.all();

            this.section = section;
            visited.clear();
            visibility = SectionVisibility.none();

            for (Vector3i start : STARTING_POINTS) {
                floodFrom(start);
                
                if (visibility.isAll())
                    break;
            }

            return visibility;
        }
    }

    @Override
    public void close() {
        shader.close();
        sharedIndexBuffer.close();
    }
}
