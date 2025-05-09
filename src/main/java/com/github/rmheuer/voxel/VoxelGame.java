package com.github.rmheuer.voxel;

import com.github.rmheuer.azalea.input.keyboard.Key;
import com.github.rmheuer.azalea.input.keyboard.KeyPressEvent;
import com.github.rmheuer.azalea.input.keyboard.Keyboard;
import com.github.rmheuer.azalea.input.mouse.MouseButton;
import com.github.rmheuer.azalea.input.mouse.MouseButtonPressEvent;
import com.github.rmheuer.azalea.input.mouse.MouseMoveEvent;
import com.github.rmheuer.azalea.input.mouse.MouseScrollEvent;
import com.github.rmheuer.azalea.io.ResourceUtil;
import com.github.rmheuer.azalea.math.AABB;
import com.github.rmheuer.azalea.math.CubeFace;
import com.github.rmheuer.azalea.render.BufferType;
import com.github.rmheuer.azalea.render.Colors;
import com.github.rmheuer.azalea.render.Renderer;
import com.github.rmheuer.azalea.render.WindowSettings;
import com.github.rmheuer.azalea.render.camera.Camera;
import com.github.rmheuer.azalea.render.camera.PerspectiveProjection;
import com.github.rmheuer.azalea.render.texture.Bitmap;
import com.github.rmheuer.azalea.render.texture.Texture2D;
import com.github.rmheuer.azalea.render.utils.DebugLineRenderer;
import com.github.rmheuer.azalea.render2d.Renderer2D;
import com.github.rmheuer.azalea.runtime.BaseGame;
import com.github.rmheuer.azalea.runtime.FixedRateExecutor;
import com.github.rmheuer.voxel.anim.LavaAnimationGenerator;
import com.github.rmheuer.voxel.anim.WaterAnimationGenerator;
import com.github.rmheuer.voxel.block.Block;
import com.github.rmheuer.voxel.block.Blocks;
import com.github.rmheuer.voxel.block.Liquid;
import com.github.rmheuer.voxel.block.LiquidShape;
import com.github.rmheuer.voxel.level.BlockMap;
import com.github.rmheuer.voxel.level.LightMap;
import com.github.rmheuer.voxel.level.MapSection;
import com.github.rmheuer.voxel.particle.ParticleSystem;
import com.github.rmheuer.voxel.physics.Raycast;
import com.github.rmheuer.voxel.render.FogInfo;
import com.github.rmheuer.voxel.render.LevelRenderData;
import com.github.rmheuer.voxel.render.LevelRenderer;
import com.github.rmheuer.voxel.render.OutsideLevelRenderer;
import com.github.rmheuer.voxel.ui.*;
import org.joml.*;

import java.io.IOException;
import java.lang.Math;

public final class VoxelGame extends BaseGame {
    private static final WindowSettings WINDOW_SETTINGS =
            new WindowSettings(640, 480, "Voxel")
                .setVSync(false);

    private static final int GUI_WIDTH = 320;
    private static final int GUI_HEIGHT = 240;

    private static final int LEVEL_SIZE_SECTIONS = 64 / MapSection.SIZE;
    
    private static final float CAMERA_HEIGHT = 1.6f;

    private static final FogInfo AIR_FOG = new FogInfo(0, 512, new Vector4f(225 / 255.0f, 240 / 255.0f, 255 / 255.0f, 1.0f), new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
    private static final FogInfo WATER_FOG = new FogInfo(0, 20, new Vector4f(0.02f, 0.02f, 0.2f, 1.0f), new Vector4f(0.4f, 0.4f, 0.9f, 1.0f));
    private static final FogInfo LAVA_FOG = new FogInfo(-0.3f, 1.6f, new Vector4f(0.6f, 0.1f, 0.0f, 1.0f), new Vector4f(0.4f, 0.3f, 0.3f, 1.0f));

    private final FixedRateExecutor ticker;
    private final Renderer2D renderer2D;

    private final Texture2D atlasTexture;
    private final UISprites uiSprites;
    private final TextRenderer textRenderer;

    private final LevelRenderer levelRenderer;
    private final DebugLineRenderer lineRenderer;
    private final ParticleSystem particleSystem;
    private final OutsideLevelRenderer outsideRenderer;

    private final WaterAnimationGenerator waterAnimationGenerator;
    private final LavaAnimationGenerator lavaAnimationGenerator;

    private final Camera camera;
    private Player player;

    private BlockMap blockMap;
    private LightMap lightMap;
    private LevelRenderData levelRenderData;

    private boolean mouseCaptured;
    private Raycast.Result raycastResult;
    private double partialScroll;

    private int guiScale;

    private final byte[] hotbar;
    private int selectedSlot;

    private UI ui;

    private boolean drawSectionBoundaries;
    private boolean drawLightHeights;

    private boolean ticked;

    public VoxelGame() throws IOException {
        super(WINDOW_SETTINGS);

        ticker = new FixedRateExecutor(1 / 20.0f, this::fixedTick);
        renderer2D = new Renderer2D(getRenderer());

        atlasTexture = getRenderer().createTexture2D(ResourceUtil.readAsStream("terrain.png"));
        uiSprites = new UISprites(getRenderer());
        textRenderer = new TextRenderer(getRenderer());

        levelRenderer = new LevelRenderer(getRenderer(), atlasTexture);
        lineRenderer = new DebugLineRenderer(getRenderer());
        particleSystem = new ParticleSystem(getRenderer(), atlasTexture);

        waterAnimationGenerator = new WaterAnimationGenerator();
        lavaAnimationGenerator = new LavaAnimationGenerator();

        camera = new Camera(new PerspectiveProjection((float) Math.toRadians(90), 0.01f, 1000f));
        outsideRenderer = new OutsideLevelRenderer(getRenderer());

        resetLevel(4);
        
        setMouseCaptured(false);
        getEventBus().addHandler(KeyPressEvent.class, this::keyPressed);
        getEventBus().addHandler(MouseScrollEvent.class, this::mouseScrolled);
        getEventBus().addHandler(MouseMoveEvent.class, this::mouseMoved);
        getEventBus().addHandler(MouseButtonPressEvent.class, this::mousePressed);
        partialScroll = 0;

        guiScale = 2;

        hotbar = new byte[] {
                Blocks.ID_STONE, Blocks.ID_GRASS, Blocks.ID_DIRT,
                Blocks.ID_COBBLESTONE, Blocks.ID_PLANKS, Blocks.ID_SAPLING,
                Blocks.ID_LOG, Blocks.ID_STILL_WATER, Blocks.ID_STILL_LAVA
        };
        selectedSlot = 0;

        ui = null;

        drawSectionBoundaries = false;
        drawLightHeights = false;
    }

    public void resetLevel(int sizeSections) {
        blockMap = new BlockMap(sizeSections, 4, sizeSections);
        lightMap = new LightMap(blockMap.getBlocksX(), blockMap.getBlocksZ());
        
        for (int z = 0; z < blockMap.getBlocksZ(); z++) {
            for (int x = 0; x < blockMap.getBlocksX(); x++) {
                for (int y = 0; y < 32; y++) {
                    blockMap.setBlockId(x, y, z, Blocks.ID_STONE);
                }
                for (int y = 32; y < blockMap.getBlocksY(); y++) {
                    blockMap.setBlockId(x, y, z, Blocks.ID_AIR);
                }
            }
        }

        for (byte block = 0; block < Blocks.BLOCK_COUNT; block++) {
            int x = 2 + (block % 10) * 2;
            int z = 2 + (block / 10) * 2;
            blockMap.setBlockId(x, 33, z, block);
        }

        lightMap.recalculateAll(blockMap);

        if (levelRenderData != null)
            levelRenderData.close();
        levelRenderData = new LevelRenderData(
            blockMap.getSectionsX(), blockMap.getSectionsY(), blockMap.getSectionsZ());

        outsideRenderer.updateLevelSize(blockMap.getBlocksX(), blockMap.getBlocksZ());
        
        player = new Player(32, 40, 32);
    }

    private void setMouseCaptured(boolean mouseCaptured) {
        if (this.mouseCaptured == mouseCaptured)
            return;

        this.mouseCaptured = mouseCaptured;
        getWindow().getMouse().setCursorCaptured(mouseCaptured);
    }

    public void setUI(UI ui) {
        setMouseCaptured(ui == null);
        this.ui = ui;
    }

    private void blockPicked(byte blockId) {
        hotbar[selectedSlot] = blockId;
        setUI(null);
    }

    private void keyPressed(KeyPressEvent event) {
        Key key = event.getKey();
        switch (key) {
            case ESCAPE:
                if (ui != null) {
                    setUI(null);
                } else {
                    setUI(new PauseMenuUI(this));
                }
                break;

            case F1:
                drawSectionBoundaries = !drawSectionBoundaries;
                break;
            case F2:
                drawLightHeights = !drawLightHeights;
                break;

            case B:
            case E:
                if (ui == null)
                    setUI(new BlockPickerUI(this::blockPicked));
                else if (ui instanceof BlockPickerUI)
                    setUI(null);
                break;

            default:
                if (key.isNumberKey() && key.getNumber() > 0) {
                    selectedSlot = key.getNumber() - 1;
                }
                break;
        }
    }

    private void mouseScrolled(MouseScrollEvent event) {
        partialScroll -= event.getScrollY();

        int steps = (int) partialScroll;
        partialScroll %= 1.0;

        selectedSlot += steps;
        selectedSlot = Math.floorMod(selectedSlot, hotbar.length);
    }

    private void mouseMoved(MouseMoveEvent event) {
        if (!mouseCaptured)
            return;

        final float sensitivity = 0.0025f;

        Vector2d delta = event.getCursorDelta();
        float deltaPitch = (float) -delta.y * sensitivity;
        float deltaYaw = (float) -delta.x * sensitivity;

        player.turn(deltaPitch, deltaYaw);
    }

    private void setBlock(Vector3i pos, byte blockId) {
        byte prevId = blockMap.setBlockId(pos.x, pos.y, pos.z, blockId);
        if (blockId == prevId)
            return;

        levelRenderData.blockChanged(pos.x, pos.y, pos.z);

        LightMap.Change lightChange = lightMap.blockChanged(blockMap, pos.x, pos.y, pos.z, prevId, blockId);
        if (lightChange != null)
            levelRenderData.lightChanged(pos.x, pos.z, lightChange.prevHeight, lightChange.newHeight);
    }

    private void mousePressed(MouseButtonPressEvent event) {
        if (ui != null) {
            Vector2d pos = event.getCursorPos();
            Vector2i uiMousePos = new Vector2i((int) (pos.x / guiScale), (int) (pos.y / guiScale));

            ui.mouseClicked(uiMousePos);
            return;
        }

        if (!mouseCaptured)
            return;

        if (event.getButton() == MouseButton.LEFT) {
            if (raycastResult != null) {
                Vector3i pos = raycastResult.blockPos;
                byte brokenBlock = blockMap.getBlockId(pos.x, pos.y, pos.z);
                if (brokenBlock != Blocks.ID_AIR) {
                    setBlock(pos, Blocks.ID_AIR);

                    particleSystem.spawnBreakParticles(pos.x, pos.y, pos.z, Blocks.getBlock(brokenBlock));
                }
            }
        } else if (event.getButton() == MouseButton.RIGHT) {
            if (raycastResult != null && raycastResult.hitFace != null) {
                byte held = hotbar[selectedSlot];
                byte placedOn = blockMap.getBlockId(raycastResult.blockPos.x, raycastResult.blockPos.y, raycastResult.blockPos.z);

                Vector3i placePos;
                byte toPlace;
                if (held == Blocks.ID_SLAB && placedOn == Blocks.ID_SLAB && raycastResult.hitFace == CubeFace.POS_Y) {
                    placePos = raycastResult.blockPos;
                    toPlace = Blocks.ID_DOUBLE_SLAB;
                    setBlock(raycastResult.blockPos, Blocks.ID_DOUBLE_SLAB);
                } else {
                    placePos = new Vector3i(raycastResult.blockPos)
                            .add(raycastResult.hitFace.getDirection());

                    toPlace = hotbar[selectedSlot];
                    byte existing = blockMap.getBlockId(placePos.x, placePos.y, placePos.z);
                    if (toPlace == Blocks.ID_SLAB && existing == Blocks.ID_SLAB)
                        toPlace = Blocks.ID_DOUBLE_SLAB;
                }

                Block toPlaceBlock = Blocks.getBlock(toPlace);
                if (toPlaceBlock.getBoundingBox() != null) {
                    AABB blockAABB = toPlaceBlock.getBoundingBox().translate(placePos.x, placePos.y, placePos.z);
                    AABB playerAABB = player.getBoundingBox();
                    if (blockAABB.intersects(playerAABB))
                        return;
                }
                setBlock(placePos, toPlace);
            }
        } else if (event.getButton() == MouseButton.MIDDLE) {
            if (raycastResult != null) {
                byte clicked = blockMap.getBlockId(raycastResult.blockPos.x, raycastResult.blockPos.y, raycastResult.blockPos.z);
                blockPicked(Blocks.getBlock(clicked).getItemId());
            }
        }
    }

    @Override
    protected void tick(float dt) {
        // Don't update game if paused
        if (ui != null && ui.shouldPauseGame())
            return;
        
        Vector3f pos = camera.getTransform().position;
        Vector3f dir = camera.getTransform().getForward();
        raycastResult = Raycast.raycast(blockMap, pos, dir, 32);

        ticker.update(dt);
    }

    private void fixedTick(float dt) {
        ticked = true;

        waterAnimationGenerator.tick();
        lavaAnimationGenerator.tick();

        outsideRenderer.tick();

        particleSystem.tickParticles(blockMap);

        Keyboard kb = getWindow().getKeyboard();
        float inputForward = 0, inputRight = 0;
        boolean jump = false;
        if (ui == null) {
            if (kb.isKeyPressed(Key.W))
                inputForward += 1;
            if (kb.isKeyPressed(Key.S))
                inputForward -= 1;
            if (kb.isKeyPressed(Key.D))
                inputRight += 1;
            if (kb.isKeyPressed(Key.A))
                inputRight -= 1;
            jump = kb.isKeyPressed(Key.SPACE);
        }
        player.tickMovement(blockMap, inputForward, inputRight, jump);
    }

    private boolean isCameraInLiquid(Liquid liquid) {
        Vector3f pos = new Vector3f(camera.getTransform().position)
            .add(camera.getTransform().getForward().mul(0.01f));
        int blockX = (int) pos.x;
        int blockY = (int) pos.y;
        int blockZ = (int) pos.z;

        if (!blockMap.isBlockInBounds(blockX, blockY, blockZ))
            return false;
        
        Liquid in = Blocks.getBlock(blockMap.getBlockId(blockX, blockY, blockZ)).getLiquid();
        if (in != liquid)
            return false;

        Liquid above = blockMap.isBlockInBounds(blockX, blockY + 1, blockZ)
            ? Blocks.getBlock(blockMap.getBlockId(blockX, blockY + 1, blockZ)).getLiquid()
            : null;

        float surfaceY = above == liquid ? 1.0f : LiquidShape.LIQUID_SURFACE_HEIGHT;
        return (pos.y % 1.0f) <= surfaceY;
    }

    @Override
    protected void render(Renderer renderer) {
        if (ticked) {
            try (Bitmap img = waterAnimationGenerator.getImage()) {
                atlasTexture.setSubData(img, 14 * 16, 0);
            }
            try (Bitmap img = lavaAnimationGenerator.getImage()) {
                atlasTexture.setSubData(img, 14 * 16, 16);
            }
            ticked = false;
        }

        float subtick = ticker.getSubtickPercent();
        camera.getTransform().setPosition(player.getPosition(subtick));
        camera.getTransform().position.y += CAMERA_HEIGHT;
        camera.getTransform().rotation.set(
                player.getPitch(),
                player.getYaw(),
                0
        );

        Vector2i windowSize = getWindow().getFramebufferSize();
        Matrix4f proj = camera.getProjectionMatrix(windowSize.x, windowSize.y);
        Matrix4f view = camera.getViewMatrix();
        Matrix4f viewProj = new Matrix4f(proj).mul(view);

        int guiScaleX = windowSize.x / GUI_WIDTH;
        int guiScaleY = windowSize.y / GUI_HEIGHT;
        guiScale = Math.min(guiScaleX, guiScaleY);
        if (guiScale < 1)
            guiScale = 1;

        boolean wireframe = getWindow().getKeyboard().isKeyPressed(Key.TAB);
        LevelRenderer.PreparedRender levelRender = levelRenderer.prepareRender(
                renderer,
                blockMap,
                lightMap,
                levelRenderData,
                camera.getTransform().position,
                wireframe
        );

        FogInfo fogInfo = AIR_FOG;
        if (isCameraInLiquid(Liquid.WATER))
            fogInfo = WATER_FOG;
        else if (isCameraInLiquid(Liquid.LAVA))
            fogInfo = LAVA_FOG;
        renderer.setClearColor(Colors.RGBA.fromFloats(fogInfo.color));
        renderer.clear(BufferType.COLOR);

        levelRender.renderOpaqueLayer(renderer, view, proj, fogInfo);
        outsideRenderer.renderOpaqueLayer(renderer, view, proj, fogInfo, subtick);

        particleSystem.renderParticles(renderer, view, proj, fogInfo, subtick, lightMap);

        outsideRenderer.renderTranslucentLayer(renderer, view, proj, fogInfo);
        levelRender.renderTranslucentLayer(renderer, view, proj, fogInfo);

        if (raycastResult != null) {
            int col = Colors.RGBA.BLUE;
            int x = raycastResult.blockPos.x;
            int y = raycastResult.blockPos.y;
            int z = raycastResult.blockPos.z;

            AABB bb = Blocks.getBlock(blockMap.getBlockId(x, y, z)).getBoundingBox();

            lineRenderer.addLine(x + bb.minX, y + bb.minY, z + bb.minZ, x + bb.maxX, y + bb.minY, z + bb.minZ, col);
            lineRenderer.addLine(x + bb.minX, y + bb.minY, z + bb.minZ, x + bb.minX, y + bb.maxY, z + bb.minZ, col);
            lineRenderer.addLine(x + bb.minX, y + bb.minY, z + bb.minZ, x + bb.minX, y + bb.minY, z + bb.maxZ, col);
            lineRenderer.addLine(x + bb.maxX, y + bb.minY, z + bb.minZ, x + bb.maxX, y + bb.maxY, z + bb.minZ, col);
            lineRenderer.addLine(x + bb.maxX, y + bb.minY, z + bb.minZ, x + bb.maxX, y + bb.minY, z + bb.maxZ, col);
            lineRenderer.addLine(x + bb.minX, y + bb.maxY, z + bb.minZ, x + bb.maxX, y + bb.maxY, z + bb.minZ, col);
            lineRenderer.addLine(x + bb.minX, y + bb.maxY, z + bb.minZ, x + bb.minX, y + bb.maxY, z + bb.maxZ, col);
            lineRenderer.addLine(x + bb.minX, y + bb.minY, z + bb.maxZ, x + bb.maxX, y + bb.minY, z + bb.maxZ, col);
            lineRenderer.addLine(x + bb.minX, y + bb.minY, z + bb.maxZ, x + bb.minX, y + bb.maxY, z + bb.maxZ, col);
            lineRenderer.addLine(x + bb.maxX, y + bb.maxY, z + bb.maxZ, x + bb.minX, y + bb.maxY, z + bb.maxZ, col);
            lineRenderer.addLine(x + bb.maxX, y + bb.maxY, z + bb.maxZ, x + bb.maxX, y + bb.minY, z + bb.maxZ, col);
            lineRenderer.addLine(x + bb.maxX, y + bb.maxY, z + bb.maxZ, x + bb.maxX, y + bb.maxY, z + bb.minZ, col);
        }

        if (drawSectionBoundaries) {
            int col = Colors.RGBA.YELLOW;

            int s = MapSection.SIZE;
            int nx = blockMap.getSectionsX() * s;
            int ny = blockMap.getSectionsY() * s;
            int nz = blockMap.getSectionsZ() * s;

            for (int x = 0; x <= nx; x += s) {
                for (int y = 0; y <= ny; y += s) {
                    lineRenderer.addLine(x, y, 0, x, y, nz, col);
                }
            }
            for (int x = 0; x <= nx; x += s) {
                for (int z = 0; z <= nz; z += s) {
                    lineRenderer.addLine(x, 0, z, x, ny, z, col);
                }
            }
            for (int y = 0; y <= ny; y += s) {
                for (int z = 0; z <= nz; z += s) {
                    lineRenderer.addLine(0, y, z, nx, y, z, col);
                }
            }
        }

        if (drawLightHeights) {
            int col = Colors.RGBA.GREEN;
            for (int z = 0; z < blockMap.getBlocksZ(); z++) {
                for (int x = 0; x < blockMap.getBlocksX(); x++) {
                    int lightHeight = lightMap.getLightHeight(x, z);
                    lineRenderer.addLine(x, lightHeight, z, x + 1, lightHeight, z + 1, col);
                    lineRenderer.addLine(x + 1, lightHeight, z, x, lightHeight, z + 1, col);
                }
            }
        }

        lineRenderer.flush(renderer, viewProj);

        Vector2d mousePos = getWindow().getMouse().getCursorPos();
        Vector2i uiMousePos = new Vector2i((int) (mousePos.x / guiScale), (int) (mousePos.y / guiScale));

        try (UIDrawList uiDraw = new UIDrawList(windowSize.x / guiScale, windowSize.y / guiScale, atlasTexture, textRenderer)) {
            int fps = (int) getFpsCounter().getFrameRate();
            float mspf = getFpsCounter().getFrameTime() * 1000.0f;
            uiDraw.drawText(1, 8, String.format("%d FPS, %.2f ms/frame", fps, mspf));
            
            uiDraw.drawSprite(uiDraw.getWidth() / 2 - 8, uiDraw.getHeight() / 2 - 8, uiSprites.getCrosshair());
            drawHotbar(uiDraw);

            if (ui != null)
                ui.draw(uiDraw, uiSprites, uiMousePos);

            renderer2D.draw(uiDraw.getDrawList(), new Matrix4f().ortho(0, (float) windowSize.x / guiScale, (float) windowSize.y / guiScale, 0, -1, 1));
        }
    }

    private void drawHotbar(UIDrawList draw) {
        UISprite hotbarSprite = uiSprites.getHotbar();
        UISprite highlightSprite = uiSprites.getHotbarHighlight();

        int x = draw.getWidth() / 2 - hotbarSprite.getWidth() / 2;
        int y = draw.getHeight() - hotbarSprite.getHeight();
        draw.drawSprite(x, y, hotbarSprite);

        for (int slot = 0; slot < 9; slot++) {
            draw.drawBlockAsItem(x + 3 + slot * 20, y + 3, 16, 16, Blocks.getBlock(hotbar[slot]));
        }

        draw.drawSprite(x - 1 + selectedSlot * 20, y - 1, highlightSprite);
    }

    @Override
    protected void cleanUp() {
        outsideRenderer.close();
        levelRenderData.close();
        levelRenderer.close();
        particleSystem.close();
        textRenderer.close();
        uiSprites.close();
        atlasTexture.close();
        renderer2D.close();
    }

    public static void main(String[] args) {
        VoxelGame game;
        try {
            game = new VoxelGame();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load resources", e);
        }

        game.run();
    }
}
