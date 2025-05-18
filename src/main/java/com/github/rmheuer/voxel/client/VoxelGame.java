package com.github.rmheuer.voxel.client;

import com.github.rmheuer.azalea.input.keyboard.CharTypeEvent;
import com.github.rmheuer.azalea.input.keyboard.Key;
import com.github.rmheuer.azalea.input.keyboard.KeyPressEvent;
import com.github.rmheuer.azalea.input.keyboard.Keyboard;
import com.github.rmheuer.azalea.input.mouse.MouseButton;
import com.github.rmheuer.azalea.input.mouse.MouseButtonPressEvent;
import com.github.rmheuer.azalea.input.mouse.MouseMoveEvent;
import com.github.rmheuer.azalea.input.mouse.MouseScrollEvent;
import com.github.rmheuer.azalea.io.ResourceUtil;
import com.github.rmheuer.azalea.math.AABB;
import com.github.rmheuer.azalea.math.MathUtil;
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
import com.github.rmheuer.voxel.block.Block;
import com.github.rmheuer.voxel.block.Blocks;
import com.github.rmheuer.voxel.block.Liquid;
import com.github.rmheuer.voxel.block.LiquidShape;
import com.github.rmheuer.voxel.client.anim.LavaAnimationGenerator;
import com.github.rmheuer.voxel.client.anim.WaterAnimationGenerator;
import com.github.rmheuer.voxel.client.particle.ParticleSystem;
import com.github.rmheuer.voxel.client.render.*;
import com.github.rmheuer.voxel.client.ui.*;
import com.github.rmheuer.voxel.level.BlockMap;
import com.github.rmheuer.voxel.level.LightMap;
import com.github.rmheuer.voxel.level.MapSection;
import com.github.rmheuer.voxel.network.packet.BidiChatMessagePacket;
import com.github.rmheuer.voxel.network.packet.BidiPlayerPositionPacket;
import com.github.rmheuer.voxel.network.packet.ClientSetBlockPacket;
import com.github.rmheuer.voxel.physics.Raycast;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import org.joml.*;

import java.io.IOException;
import java.lang.Math;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class VoxelGame extends BaseGame {
    private static final WindowSettings WINDOW_SETTINGS =
            new WindowSettings(640, 480, "Voxel")
                    .setVSync(false);

    private static final int GUI_WIDTH = 320;
    private static final int GUI_HEIGHT = 240;

    private static final float CAMERA_HEIGHT = 1.6f;

    private static final FogInfo WATER_FOG = new FogInfo(0, 20, new Vector4f(0.02f, 0.02f, 0.2f, 1.0f), new Vector4f(0.4f, 0.4f, 0.9f, 1.0f));
    private static final FogInfo LAVA_FOG = new FogInfo(-0.3f, 1.6f, new Vector4f(0.6f, 0.1f, 0.0f, 1.0f), new Vector4f(0.4f, 0.3f, 0.3f, 1.0f));

    private static final int CHAT_FADE_START = 5 * 20;
    private static final int CHAT_FADE_TIME = 20;
    private static final int CHAT_MAX_SHOWING = 10;

    private static final int IDLE_POSITION_SEND_INTERVAL = 20;

    private static final class ChatMessage {
        public final String message;
        public int age;

        public ChatMessage(String message) {
            this.message = message;
            age = 0;
        }
    }

    private final EventLoopGroup nettyEventLoop;
    private final FixedRateExecutor ticker;
    private final Renderer2D renderer2D;

    private final Texture2D atlasTexture;
    private final UISprites uiSprites;
    private final TextRenderer textRenderer;

    private final LevelRenderer levelRenderer;
    private final DebugLineRenderer lineRenderer;
    private final ParticleSystem particleSystem;
    private final EnvironmentRenderer environmentRenderer;
    private final RemotePlayerRenderer remotePlayerRenderer;

    private final WaterAnimationGenerator waterAnimationGenerator;
    private final LavaAnimationGenerator lavaAnimationGenerator;

    private final PerspectiveProjection cameraProj;
    private final Camera camera;
    private LocalPlayer localPlayer;

    private ClientLevel level;

    private boolean mouseCaptured;
    private Raycast.Result raycastResult;
    private double partialScroll;

    private int guiScale;

    private final byte[] hotbar;
    private int selectedSlot;

    private UI ui;

    private boolean drawSectionBoundaries;
    private boolean drawLightHeights;
    private boolean drawLevelAsTranslucent;

    private boolean ticked;

    private RenderDistance renderDistance;

    private Matrix4f capturedViewProj;
    private Vector3f capturedCameraPos;

    private ServerConnection connection;
    private NetworkHandler networkHandler;
    private int positionSendTimer;
    private final Map<Byte, RemotePlayer> remotePlayers;

    private final List<ChatMessage> chatHistory;

    private final Queue<Runnable> scheduledTasks;

    public VoxelGame(String host, int port, String username) throws IOException {
        super(WINDOW_SETTINGS);

        nettyEventLoop = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        ticker = new FixedRateExecutor(1 / 20.0f, this::fixedTick);
        renderer2D = new Renderer2D(getRenderer());

        atlasTexture = getRenderer().createTexture2D(ResourceUtil.readAsStream("terrain.png"));
        uiSprites = new UISprites(getRenderer());
        textRenderer = new TextRenderer(getRenderer());
        remotePlayerRenderer = new RemotePlayerRenderer(getRenderer());

        levelRenderer = new LevelRenderer(getRenderer(), atlasTexture);
        lineRenderer = new DebugLineRenderer(getRenderer());
        particleSystem = new ParticleSystem(getRenderer(), atlasTexture);

        waterAnimationGenerator = new WaterAnimationGenerator();
        lavaAnimationGenerator = new LavaAnimationGenerator();

        cameraProj = new PerspectiveProjection((float) Math.toRadians(90), 0.05f, 1000f);
        camera = new Camera(cameraProj);
        environmentRenderer = new EnvironmentRenderer(getRenderer());

        setMouseCaptured(false);
        getEventBus().addHandler(KeyPressEvent.class, this::keyPressed);
        getEventBus().addHandler(CharTypeEvent.class, this::charTyped);
        getEventBus().addHandler(MouseScrollEvent.class, this::mouseScrolled);
        getEventBus().addHandler(MouseMoveEvent.class, this::mouseMoved);
        getEventBus().addHandler(MouseButtonPressEvent.class, this::mousePressed);
        partialScroll = 0;

        guiScale = 2;

        hotbar = new byte[] {
                Blocks.ID_STONE, Blocks.ID_DIRT, Blocks.ID_COBBLESTONE,
                Blocks.ID_PLANKS, Blocks.ID_SAPLING, Blocks.ID_LOG,
                Blocks.ID_LEAVES, Blocks.ID_SAND, Blocks.ID_BRICKS
        };
        selectedSlot = 0;

        drawSectionBoundaries = false;
        drawLightHeights = false;

        renderDistance = RenderDistance.FAR;

        capturedViewProj = null;
        capturedCameraPos = null;

        scheduledTasks = new ConcurrentLinkedQueue<>();
        remotePlayers = new HashMap<>();

        chatHistory = new ArrayList<>();

        beginConnecting(host, port, username);
    }

    private void beginConnecting(String host, int port, String username) {
        ConnectingToServerUI connectingUI = new ConnectingToServerUI();
        setUI(connectingUI);

        InetSocketAddress addr = new InetSocketAddress(host, port);
        ChannelFuture connectFuture = ServerConnection.connectToServer(nettyEventLoop, addr);
        connectFuture.addListener((future) -> {
            if (future.isSuccess()) {
                System.out.println("Socket connected");

                runOnMainThread(() -> {
                    connectingUI.setState(ConnectingToServerUI.State.LOGGING_IN);

                    connection = connectFuture.channel().pipeline().get(ServerConnection.class);
                    networkHandler = new NetworkHandler(this, connection, username);
                });
            } else {
                System.err.println("Failed to connect to server");
                future.cause().printStackTrace();
                runOnMainThread(this::stop);
            }
        });
    }

    public Player getPlayer(byte playerId) {
        if (playerId == -1)
            return localPlayer;

        return remotePlayers.get(playerId);
    }

    public void addRemotePlayer(byte playerId, RemotePlayer player) {
        remotePlayers.put(playerId, player);
    }

    public void removeRemotePlayer(byte playerId) {
        remotePlayers.remove(playerId);
    }

    public void resetLocalPlayer(float x, float y, float z, float pitch, float yaw) {
        localPlayer = new LocalPlayer(x, y, z, pitch, yaw);
    }

    public void initLevel(int sizeX, int sizeY, int sizeZ, byte[] blockData) {
        System.out.println("Level: " + sizeX + ", " + sizeY + ", " + sizeZ);

        level = new ClientLevel(sizeX, sizeY, sizeZ, blockData);

        environmentRenderer.updateLevelSize(sizeX, sizeZ);
    }

    public void clearLevel() {
        if (level != null) {
            level.close();
            level = null;
        }
    }

    public void addChatMessage(String message) {
        chatHistory.add(new ChatMessage(message));
        if (chatHistory.size() > CHAT_MAX_SHOWING)
            chatHistory.remove(0);
    }

    public void sendChatMessage(String message) {
        if (connection != null)
            connection.sendPacket(new BidiChatMessagePacket((byte) -1, message));
    }

    public void runOnMainThread(Runnable fn) {
        scheduledTasks.add(fn);
    }

    private void setMouseCaptured(boolean mouseCaptured) {
        if (this.mouseCaptured == mouseCaptured)
            return;

        this.mouseCaptured = mouseCaptured;
        getWindow().getMouse().setCursorCaptured(mouseCaptured);
    }

    public UI getUI() {
        return ui;
    }

    /**
     * Sets the currently shown UI.
     *
     * @param ui new UI to show. If null, no UI will be shown
     */
    public void setUI(UI ui) {
        setMouseCaptured(ui == null);
        this.ui = ui;
    }

    public void pickBlock(byte blockId) {
        hotbar[selectedSlot] = blockId;
    }

    private void keyPressed(KeyPressEvent event) {
        Key key = event.getKey();

        if (ui != null) {
            if (ui.keyPressed(key)) {
                // UI captured key press
                return;
            }
        }

        switch (key) {
            case ESCAPE:
                if (ui == null)
                    setUI(new PauseMenuUI(this));
                break;

            case F1:
                drawSectionBoundaries = !drawSectionBoundaries;
                break;
            case F2:
                drawLightHeights = !drawLightHeights;
                break;
            case F3:
                if (capturedViewProj == null) {
                    Vector2i size = getWindow().getFramebufferSize();
                    Matrix4f proj = camera.getProjectionMatrix(size.x, size.y);
                    Matrix4f view = camera.getViewMatrix();
                    capturedViewProj = proj.mul(view);
                    capturedCameraPos = new Vector3f(camera.getTransform().position);
                    System.out.println("Captured frustum");
                } else {
                    capturedViewProj = null;
                    capturedCameraPos = null;
                    System.out.println("Reset frustum");
                }
                break;
            case F4:
                drawLevelAsTranslucent = !drawLevelAsTranslucent;
                break;

            case B:
            case E:
                if (ui == null)
                    setUI(new BlockPickerUI(this));
                break;

            default:
                if (key.isNumberKey() && key.getNumber() > 0) {
                    selectedSlot = key.getNumber() - 1;
                }
                break;
        }
    }

    private void charTyped(CharTypeEvent event) {
        if (ui != null) {
            ui.charTyped(event.getChar());
        }

        // Checked here instead of keyPressed() to prevent the t from being
        // immediately typed into the chat
        if (ui == null && Character.toLowerCase(event.getChar()) == 't') {
            setUI(new ChatInputUI(this));
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

        if (capturedViewProj != null)
            camera.getTransform().rotation.add(deltaPitch, deltaYaw, 0);
        else
            localPlayer.turn(deltaPitch, deltaYaw);
    }

    public void setBlock(Vector3i pos, byte blockId) {
        BlockMap blockMap = level.getBlockMap();
        LightMap lightMap = level.getLightMap();
        LevelRenderData levelRenderData = level.getRenderData();

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

        if (!mouseCaptured || level == null)
            return;

        if (event.getButton() == MouseButton.LEFT) {
            if (raycastResult != null) {
                Vector3i pos = raycastResult.blockPos;
                byte brokenBlock = level.getBlockMap().getBlockId(pos.x, pos.y, pos.z);
                if (brokenBlock != Blocks.ID_AIR) {
                    setBlock(pos, Blocks.ID_AIR);
                    particleSystem.spawnBreakParticles(pos.x, pos.y, pos.z, Blocks.getBlock(brokenBlock));

                    connection.sendPacket(new ClientSetBlockPacket(
                            (short) pos.x, (short) pos.y, (short) pos.z,
                            ClientSetBlockPacket.Mode.DESTROYED,
                            hotbar[selectedSlot]
                    ));
                }
            }
        } else if (event.getButton() == MouseButton.RIGHT) {
            if (raycastResult != null && raycastResult.hitFace != null) {
                byte held = hotbar[selectedSlot];

                Vector3i placePos = new Vector3i(raycastResult.blockPos)
                            .add(raycastResult.hitFace.getDirection());
                byte toPlace = held;

                Block replacing = Blocks.getBlock(level.getBlockMap().getBlockId(placePos.x, placePos.y, placePos.z));
                if (!replacing.isReplaceable())
                    return;

                if (toPlace == Blocks.ID_SLAB && placePos.y > 0 && level.getBlockMap().getBlockId(placePos.x, placePos.y - 1, placePos.z) == Blocks.ID_SLAB) {
                    toPlace = Blocks.ID_DOUBLE_SLAB;

                    // Bug in original, but needed to be consistent with server behavior
                    setBlock(placePos, Blocks.ID_AIR);
                    placePos.y--;
                }

                Block toPlaceBlock = Blocks.getBlock(toPlace);
                if (toPlaceBlock.getBoundingBox() != null) {
                    AABB blockAABB = toPlaceBlock.getBoundingBox().translate(placePos.x, placePos.y, placePos.z);
                    AABB playerAABB = localPlayer.getBoundingBox();
                    if (blockAABB.intersects(playerAABB))
                        return;
                }
                setBlock(placePos, toPlace);

                short packetY = (short) placePos.y;
                if (toPlace == Blocks.ID_DOUBLE_SLAB)
                    packetY++;
                connection.sendPacket(new ClientSetBlockPacket(
                        (short) placePos.x, packetY, (short) placePos.z,
                        ClientSetBlockPacket.Mode.PLACED,
                        held
                ));
            }
        } else if (event.getButton() == MouseButton.MIDDLE) {
            if (raycastResult != null) {
                byte clicked = level.getBlockMap().getBlockId(raycastResult.blockPos.x, raycastResult.blockPos.y, raycastResult.blockPos.z);
                pickBlock(Blocks.getBlock(clicked).getItemId());
            }
        }
    }

    @Override
    protected void tick(float dt) {
        Runnable fn;
        while ((fn = scheduledTasks.poll()) != null) {
            fn.run();
        }

        if (connection != null && !connection.isConnected()) {
            stop();
        }

        if (!getWindow().isFocused() && ui == null)
            setUI(new PauseMenuUI(this));

        if (level != null) {
            Vector3f pos = camera.getTransform().position;
            Vector3f dir = camera.getTransform().getForward();
            raycastResult = Raycast.raycast(level.getBlockMap(), pos, dir, 5);
        }

        if (capturedCameraPos != null)
            freeMoveCamera(getWindow().getKeyboard(), dt);

        ticker.update(dt);
    }

    private void freeMoveCamera(Keyboard kb, float dt) {
        Vector3f pos = camera.getTransform().position;
        Vector3f rot = camera.getTransform().rotation;

        float move = dt * 20;
        Vector3f forward = new Vector3f(0, 0, -move).rotateY(rot.y);
        Vector3f right = new Vector3f(move, 0, 0).rotateY(rot.y);
        Vector3f up = new Vector3f(0, move, 0);

        if (kb.isKeyPressed(Key.W))
            pos.add(forward);
        if (kb.isKeyPressed(Key.S))
            pos.sub(forward);
        if (kb.isKeyPressed(Key.D))
            pos.add(right);
        if (kb.isKeyPressed(Key.A))
            pos.sub(right);
        if (kb.isKeyPressed(Key.SPACE))
            pos.add(up);
        if (kb.isKeyPressed(Key.LEFT_SHIFT))
            pos.sub(up);

        float turn = (float) (dt * Math.PI);
        if (kb.isKeyPressed(Key.LEFT))
            rot.y += turn;
        if (kb.isKeyPressed(Key.RIGHT))
            rot.y -= turn;
        if (kb.isKeyPressed(Key.UP))
            rot.x += turn;
        if (kb.isKeyPressed(Key.DOWN))
            rot.x -= turn;
    }

    // Fixed tick at 20 Hz
    private void fixedTick(float dt) {
        ticked = true;

        waterAnimationGenerator.tick();
        lavaAnimationGenerator.tick();

        environmentRenderer.tick();

        for (int i = chatHistory.size() - 1; i >= 0; i--) {
            ChatMessage msg = chatHistory.get(i);
            if (msg.age >= CHAT_FADE_START + CHAT_FADE_TIME)
                break;

            msg.age++;
        }

        if (networkHandler != null)
            networkHandler.tick();

        if (level == null)
            return;

        particleSystem.tickParticles(level.getBlockMap());

        for (RemotePlayer remotePlayer : remotePlayers.values()) {
            remotePlayer.tick();
        }

        Keyboard kb = getWindow().getKeyboard();
        float inputForward = 0, inputRight = 0;
        boolean jump = false;
        if (ui == null && capturedCameraPos == null) {
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
        localPlayer.tickMovement(level.getBlockMap(), inputForward, inputRight, jump);

        if (localPlayer.movedOrTurnedThisTick() || positionSendTimer-- <= 0) {
            localPlayer.clearTurned();

            Vector3f pos = localPlayer.getPosition();
            connection.sendPacket(new BidiPlayerPositionPacket(
                    (byte) -1,
                    pos.x, pos.y + NetworkHandler.POSITION_Y_OFFSET, pos.z,
                    localPlayer.getYaw(), localPlayer.getPitch()
            ));
            positionSendTimer = IDLE_POSITION_SEND_INTERVAL;
        }
    }

    private boolean isCameraInLiquid(Liquid liquid) {
        if (capturedCameraPos != null)
            return false;

        Vector3f pos = new Vector3f(camera.getTransform().position)
                .add(camera.getTransform().getForward().mul(0.01f));
        int blockX = (int) pos.x;
        int blockY = (int) pos.y;
        int blockZ = (int) pos.z;

        BlockMap blockMap = level.getBlockMap();

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

    private LevelRenderer.PreparedRender renderLevel(Renderer renderer, Vector2i windowSize) {
        if (localPlayer == null)
            throw new IllegalStateException("No player!");

        float subtick = ticker.getSubtickPercent();
        if (capturedCameraPos == null) {
            camera.getTransform().setPosition(localPlayer.getSmoothedPosition(subtick));
            camera.getTransform().position.y += CAMERA_HEIGHT;
            camera.getTransform().rotation.set(
                    localPlayer.getPitch(),
                    localPlayer.getYaw(),
                    0
            );
        }

        FogInfo fogInfo;
        if (isCameraInLiquid(Liquid.WATER))
            fogInfo = WATER_FOG;
        else if (isCameraInLiquid(Liquid.LAVA))
            fogInfo = LAVA_FOG;
        else
            fogInfo = new FogInfo(0, renderDistance.getFogDistance(), new Vector4f(225 / 255.0f, 240 / 255.0f, 255 / 255.0f, 1.0f), new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
        cameraProj.setFarPlane(fogInfo.maxDistance);

        Matrix4f proj = camera.getProjectionMatrix(windowSize.x, windowSize.y);
        Matrix4f view = camera.getViewMatrix();
        Matrix4f viewProj = new Matrix4f(proj).mul(view);

        boolean wireframe = getWindow().getKeyboard().isKeyPressed(Key.TAB);
        LevelRenderer.PreparedRender levelRender = levelRenderer.prepareRender(
                renderer,
                level.getBlockMap(),
                level.getLightMap(),
                level.getRenderData(),
                capturedViewProj != null ? capturedViewProj : viewProj,
                capturedCameraPos != null ? capturedCameraPos : camera.getTransform().position,
                wireframe,
                lineRenderer
        );

        if (raycastResult != null) {
            int col = Colors.RGBA.BLUE;
            int x = raycastResult.blockPos.x;
            int y = raycastResult.blockPos.y;
            int z = raycastResult.blockPos.z;

            AABB bb = Blocks.getBlock(level.getBlockMap().getBlockId(x, y, z)).getBoundingBox();

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

            BlockMap blockMap = level.getBlockMap();
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
            BlockMap blockMap = level.getBlockMap();
            LightMap lightMap = level.getLightMap();
            int col = Colors.RGBA.GREEN;
            for (int z = 0; z < blockMap.getBlocksZ(); z++) {
                for (int x = 0; x < blockMap.getBlocksX(); x++) {
                    int lightHeight = lightMap.getLightHeight(x, z);
                    lineRenderer.addLine(x, lightHeight, z, x + 1, lightHeight, z + 1, col);
                    lineRenderer.addLine(x + 1, lightHeight, z, x, lightHeight, z + 1, col);
                }
            }
        }

        if (capturedViewProj != null) {
            Vector3f nnn = capturedViewProj.frustumCorner(Matrix4fc.CORNER_NXNYNZ, new Vector3f());
            Vector3f nnp = capturedViewProj.frustumCorner(Matrix4fc.CORNER_NXNYPZ, new Vector3f());
            Vector3f npn = capturedViewProj.frustumCorner(Matrix4fc.CORNER_NXPYNZ, new Vector3f());
            Vector3f npp = capturedViewProj.frustumCorner(Matrix4fc.CORNER_NXPYPZ, new Vector3f());
            Vector3f pnn = capturedViewProj.frustumCorner(Matrix4fc.CORNER_PXNYNZ, new Vector3f());
            Vector3f pnp = capturedViewProj.frustumCorner(Matrix4fc.CORNER_PXNYPZ, new Vector3f());
            Vector3f ppn = capturedViewProj.frustumCorner(Matrix4fc.CORNER_PXPYNZ, new Vector3f());
            Vector3f ppp = capturedViewProj.frustumCorner(Matrix4fc.CORNER_PXPYPZ, new Vector3f());

            int col = Colors.RGBA.MAGENTA;
            lineRenderer.addLine(nnn, nnp, col);
            lineRenderer.addLine(nnn, npn, col);
            lineRenderer.addLine(nnn, pnn, col);
            lineRenderer.addLine(ppp, ppn, col);
            lineRenderer.addLine(ppp, pnp, col);
            lineRenderer.addLine(ppp, npp, col);
            lineRenderer.addLine(pnn, ppn, col);
            lineRenderer.addLine(pnn, pnp, col);
            lineRenderer.addLine(npn, ppn, col);
            lineRenderer.addLine(npn, npp, col);
            lineRenderer.addLine(nnp, pnp, col);
            lineRenderer.addLine(nnp, npp, col);
        }

        renderer.setClearColor(Colors.RGBA.fromFloats(fogInfo.color));
        renderer.clear(BufferType.COLOR);

        lineRenderer.flush(renderer, viewProj);

        if (drawLevelAsTranslucent) {
            float a = 0.5f;
            fogInfo = new FogInfo(fogInfo.minDistance, fogInfo.maxDistance, new Vector4f(fogInfo.color).setComponent(3, a), new Vector4f(fogInfo.tintColor).setComponent(3, a));
        }

        levelRender.renderOpaqueLayer(renderer, view, proj, fogInfo);
        environmentRenderer.renderOpaqueLayer(renderer, view, proj, fogInfo, subtick);

        remotePlayerRenderer.renderPlayers(renderer, level, remotePlayers.values(), view, proj, fogInfo, subtick);

        particleSystem.renderParticles(renderer, view, proj, fogInfo, subtick, level.getLightMap());

        environmentRenderer.renderTranslucentLayer(renderer, view, proj, fogInfo);
        levelRender.renderTranslucentLayer(renderer, view, proj, fogInfo);

        return levelRender;
    }

    private void renderInGameUI(UIDrawList uiDraw, LevelRenderer.PreparedRender levelRender) {
        int fps = (int) getFpsCounter().getFrameRate();
        float mspf = getFpsCounter().getFrameTime() * 1000.0f;
        uiDraw.drawText(1, 8, String.format("%d FPS, %.2f ms/frame", fps, mspf));
        uiDraw.drawText(1, 18, String.format("Render sections: %d opaque, %d translucent", levelRender.getOpaqueCount(), levelRender.getTranslucentCount()));

        uiDraw.drawSprite(uiDraw.getWidth() / 2 - 8, uiDraw.getHeight() / 2 - 8, uiSprites.getCrosshair());
        drawChatHistory(uiDraw);
        drawHotbar(uiDraw);
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

    private void drawChatHistory(UIDrawList draw) {
        boolean showAll = ui instanceof ChatInputUI;

        for (int i = 0; i < chatHistory.size(); i++) {
            ChatMessage msg = chatHistory.get(chatHistory.size() - i - 1);
            if (!showAll && msg.age >= CHAT_FADE_START + CHAT_FADE_TIME)
                break;

            int y = draw.getHeight() - 48 - 10 * i;
            float alpha = showAll ? 1 : Math.min(1, MathUtil.map(msg.age, CHAT_FADE_START, CHAT_FADE_START + CHAT_FADE_TIME, 1, 0));

            draw.drawTextAlpha(2, y, msg.message, alpha);
        }
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

        Vector2i windowSize = getWindow().getFramebufferSize();

        LevelRenderer.PreparedRender levelRender = null;
        if (level != null) {
            levelRender = renderLevel(renderer, windowSize);
        }

        int guiScaleX = windowSize.x / GUI_WIDTH;
        int guiScaleY = windowSize.y / GUI_HEIGHT;
        guiScale = Math.min(guiScaleX, guiScaleY);
        if (guiScale < 1)
            guiScale = 1;

        Vector2d mousePos = getWindow().getMouse().getCursorPos();
        Vector2i uiMousePos = new Vector2i((int) (mousePos.x / guiScale), (int) (mousePos.y / guiScale));

        int uiWidth = MathUtil.ceilDiv(windowSize.x, guiScale);
        int uiHeight = MathUtil.ceilDiv(windowSize.y, guiScale);
        try (UIDrawList uiDraw = new UIDrawList(uiWidth, uiHeight, atlasTexture, textRenderer)) {
            if (levelRender != null)
                renderInGameUI(uiDraw, levelRender);

            if (ui != null)
                ui.draw(uiDraw, uiSprites, uiMousePos);

            renderer2D.draw(uiDraw.getDrawList(), new Matrix4f().ortho(0, (float) windowSize.x / guiScale, (float) windowSize.y / guiScale, 0, -1, 1));
        }
    }

    public RenderDistance getRenderDistance() {
        return renderDistance;
    }

    public void setRenderDistance(RenderDistance renderDistance) {
        this.renderDistance = renderDistance;
    }

    @Override
    protected void cleanUp() {
        if (level != null)
            level.close();

        environmentRenderer.close();
        levelRenderer.close();
        particleSystem.close();
        textRenderer.close();
        remotePlayerRenderer.close();
        uiSprites.close();
        atlasTexture.close();
        renderer2D.close();
        nettyEventLoop.shutdownGracefully();
    }

    public static void main(String[] args) {
        String host = "localhost";
        int port = 25565;
        String username;
        if (args.length > 1) {
            host = args[0];
            port = Integer.parseInt(args[1]);
            username = args[2];
        } else {
            username = args[0];
        }

        VoxelGame game;
        try {
            game = new VoxelGame(host, port, username);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load resources", e);
        }

        game.run();
    }
}
