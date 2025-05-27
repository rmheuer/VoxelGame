package com.github.rmheuer.voxel.client;

import com.github.rmheuer.azalea.render.Colors;
import com.github.rmheuer.voxel.client.ui.DownloadingTerrainUI;
import com.github.rmheuer.voxel.level.BlockMap;
import com.github.rmheuer.voxel.network.ServerPacketListener;
import com.github.rmheuer.voxel.network.cpe.packet.*;
import com.github.rmheuer.voxel.network.packet.*;
import com.github.rmheuer.voxel.network.cpe.CPEExtensions;
import org.joml.Vector3i;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

public final class NetworkHandler implements ServerPacketListener {
    public static final float POSITION_Y_OFFSET = 1.6f;
    private static final int TIMEOUT_TICKS = 10 * 20;

    private final VoxelGame client;
    private final ServerConnection conn;

    private final CPEExtensions.ExtensionSet receivedServerExtensions;
    private int extEntryPacketsRemaining;
    private CPEExtensions extensions;

    private DownloadingTerrainUI downloadingTerrainUI;
    private final List<byte[]> receivedLevelChunks;

    private final AtomicInteger timeoutTimer;

    public NetworkHandler(VoxelGame client, ServerConnection conn, String username) {
        this.client = client;
        this.conn = conn;

        receivedServerExtensions = new CPEExtensions.ExtensionSet();
        receivedLevelChunks = new ArrayList<>();

        conn.setPacketListener(this);
        conn.sendPacket(new ClientPlayerIdPacket((short) 7, username, "", CPEExtensions.HANDSHAKE_MAGIC_VALUE));

        timeoutTimer = new AtomicInteger(0);
    }

    public void tick() {
        if (timeoutTimer.incrementAndGet() >= TIMEOUT_TICKS) {
            System.err.println("Server connection timed out");
            conn.close();
        }
    }

    private void resetTimeout() {
        timeoutTimer.set(0);
    }

    @Override
    public void onExtInfo(BidiExtInfoPacket packet) {
        System.out.println("Server started CPE negotiation");
        System.out.println("Server software: " + packet.getAppName());

        extEntryPacketsRemaining = packet.getExtensionCount();
        if (extEntryPacketsRemaining == 0)
            sendCPEReply();
    }

    @Override
    public void onExtEntry(BidiExtEntryPacket packet) {
        System.out.println("Server supports extension: " + packet.getExtName() + " version " + packet.getVersion());

        receivedServerExtensions.add(packet.getExtName(), packet.getVersion());

        extEntryPacketsRemaining--;
        if (extEntryPacketsRemaining == 0)
            sendCPEReply();
    }

    private void sendCPEReply() {
        List<CPEExtensions.ExtensionInfo> extensions = CPEExtensions.ALL_SUPPORTED;

        conn.sendPacket(new BidiExtInfoPacket(
                "VoxelGame",
                (short) extensions.size()
        ));
        for (CPEExtensions.ExtensionInfo info : extensions) {
            conn.sendPacket(new BidiExtEntryPacket(info.name, info.version));
        }

        this.extensions = new CPEExtensions(receivedServerExtensions);
        System.out.println("CPE negotiation finished");
    }

    @Override
    public void onServerId(ServerIdPacket packet) {
        System.out.println("Server name: " + packet.getServerName());
        System.out.println("Server MOTD: " + packet.getServerMotd());

        // If no CPE negotiation happened, no extensions are supported
        if (extensions == null)
            extensions = new CPEExtensions(new CPEExtensions.ExtensionSet());
    }

    @Override
    public void onPing() {
        resetTimeout();
    }

    @Override
    public void onLevelInit() {
        resetTimeout();
        downloadingTerrainUI = new DownloadingTerrainUI();
        receivedLevelChunks.clear();
        client.runOnMainThread(() -> {
            client.setUI(downloadingTerrainUI);
            client.clearLevel();
        });
    }

    @Override
    public void onLevelDataChunk(ServerLevelDataChunkPacket packet) {
        resetTimeout();
        receivedLevelChunks.add(packet.getChunkData());

        // Local copy so it doesn't get replaced with null in onLevelFinalize()
        final DownloadingTerrainUI ui = downloadingTerrainUI;
        client.runOnMainThread(() -> ui.setPercentReceived(packet.getPercentComplete()));
    }

    @Override
    public void onLevelFinalize(ServerLevelFinalizePacket packet) {
        resetTimeout();

        int totalSize = 0;
        for (byte[] chunk : receivedLevelChunks) {
            totalSize += chunk.length;
        }

        byte[] gzipLevelData = new byte[totalSize];
        int offset = 0;
        for (byte[] chunk : receivedLevelChunks) {
            System.arraycopy(chunk, 0, gzipLevelData, offset, chunk.length);
            offset += chunk.length;
        }

        int expectedLen = packet.getSizeX() * packet.getSizeY() * packet.getSizeZ();

        byte[] levelData;
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(gzipLevelData))) {
            DataInputStream in = new DataInputStream(gzip);
            int length = in.readInt();
            if (length != expectedLen)
                throw new RuntimeException("Incorrect level data array size");

            levelData = new byte[length];
            in.readFully(levelData);
        } catch (IOException e) {
            throw new RuntimeException("Failed to decompress level data", e);
        }

        downloadingTerrainUI = null;
        receivedLevelChunks.clear();

        client.runOnMainThread(() -> {
            client.initLevel(packet.getSizeX(), packet.getSizeY(), packet.getSizeZ(), levelData);
            client.setUI(null);
        });
    }

    @Override
    public void onSetBlock(ServerSetBlockPacket packet) {
        client.runOnMainThread(() -> {
            client.setBlock(new Vector3i(packet.getX(), packet.getY(), packet.getZ()), packet.getBlockId());
        });
    }

    @Override
    public void onBulkBlockUpdate(ServerBulkBlockUpdatePacket packet) {
        int[] indices = packet.getIndices();
        byte[] blocks = packet.getBlocks();
        client.runOnMainThread(() -> {
            BlockMap map = client.getLevel().getBlockMap();
            int blocksX = map.getBlocksX();
            int blocksZ = map.getBlocksZ();

            Vector3i pos = new Vector3i();
            for (int i = 0; i < indices.length; i++) {
                int idx = indices[i];
                
                int x = idx % blocksX;
                int z = (idx / blocksX) % blocksZ;
                int y = idx / blocksX / blocksZ;
                pos.set(x, y, z);

                client.setBlock(pos, blocks[i]);
            }
        });
    }

    @Override
    public void onSpawnPlayer(ServerSpawnPlayerPacket packet) {
        if (packet.getPlayerId() == -1) {
            client.runOnMainThread(() -> client.resetLocalPlayer(
                    packet.getX(), packet.getY() - POSITION_Y_OFFSET + 1, packet.getZ(),
                    packet.getPitch(), packet.getYaw()
            ));
        } else {
            client.runOnMainThread(() -> client.addRemotePlayer(
                    packet.getPlayerId(),
                    new RemotePlayer(
                            packet.getX(), packet.getY() - POSITION_Y_OFFSET, packet.getZ(),
                            packet.getPitch(), packet.getYaw()
                    )
            ));
        }
    }

    @Override
    public void onPlayerPosition(BidiPlayerPositionPacket packet) {
        client.runOnMainThread(() -> {
            Player player = client.getPlayer(packet.getPlayerId());
            if (player == null) {
                System.err.println("Received position for unknown player " + packet.getPlayerId());
                return;
            }

            player.teleport(packet.getX(), packet.getY() - POSITION_Y_OFFSET, packet.getZ());
            player.setDirection(packet.getPitch(), packet.getYaw());
        });
    }

    @Override
    public void onExtEntityTeleport(ServerExtEntityTeleportPacket packet) {
        client.runOnMainThread(() -> {
            Player player = client.getPlayer(packet.getEntityId());
            if (player == null) {
                System.err.println("Received teleport for unknown player " + packet.getEntityId());
                return;
            }

            switch (packet.getMoveMode()) {
                case ABSOLUTE_INSTANT:
                    player.teleportInstantly(packet.getX(), packet.getY(), packet.getZ());
                    break;
                case ABSOLUTE_SMOOTH:
                    player.teleport(packet.getX(), packet.getY(), packet.getZ());
                    break;
                case RELATIVE_INSTANT:
                    player.moveInstantly(packet.getX(), packet.getY(), packet.getZ());
                    break;
                case RELATIVE_SMOOTH:
                    player.move(packet.getX(), packet.getY(), packet.getZ());
                    break;
            }

            switch (packet.getLookMode()) {
                case INSTANT:
                    player.setDirectionInstantly(packet.getPitch(), packet.getYaw());
                    break;
                case SMOOTH:
                    player.setDirection(packet.getPitch(), packet.getYaw());
                    break;
            }
        });
    }

    @Override
    public void onRelativeMoveAndLook(ServerRelativeMoveAndLookPacket packet) {
        client.runOnMainThread(() -> {
            Player player = client.getPlayer(packet.getPlayerId());
            if (player == null) {
                System.err.println("Received movement for unknown player " + packet.getPlayerId());
                return;
            }

            player.move(packet.getDeltaX(), packet.getDeltaY(), packet.getDeltaZ());
            player.setDirection(packet.getPitch(), packet.getYaw());
        });
    }

    @Override
    public void onRelativeMove(ServerRelativeMovePacket packet) {
        client.runOnMainThread(() -> {
            Player player = client.getPlayer(packet.getPlayerId());
            if (player == null) {
                System.err.println("Received movement for unknown player " + packet.getPlayerId());
                return;
            }

            player.move(packet.getDeltaX(), packet.getDeltaY(), packet.getDeltaZ());
        });
    }

    @Override
    public void onLook(ServerLookPacket packet) {
        client.runOnMainThread(() -> {
            Player player = client.getPlayer(packet.getPlayerId());
            if (player == null) {
                System.err.println("Received movement for unknown player " + packet.getPlayerId());
                return;
            }

            player.setDirection(packet.getPitch(), packet.getYaw());
        });
    }

    @Override
    public void onDespawnPlayer(ServerDespawnPlayerPacket packet) {
        client.runOnMainThread(() -> {
            client.removeRemotePlayer(packet.getPlayerId());
        });
    }

    @Override
    public void onChatMessage(BidiChatMessagePacket packet) {
        client.runOnMainThread(() -> {
            client.addChatMessage(packet.getMessage());
        });
    }

    @Override
    public void onSetTextColor(ServerSetTextColorPacket packet) {
        client.runOnMainThread(() -> {
            if (packet.getA() > 0) {
                System.out.println("Defined new color for &" + packet.getCode());
                client.getChatColors().defineCustomColor(
                        packet.getCode(),
                        Colors.RGBA.fromInts(packet.getR(), packet.getG(), packet.getB(), packet.getA())
                );
            } else {
                System.out.println("Removed color definition for &" + packet.getCode());
                client.getChatColors().removeCustomColor(packet.getCode());
            }
        });
    }

    @Override
    public void onDisconnect(ServerDisconnectPacket packet) {
        System.out.println("Kicked: " + packet.getReason());
        conn.close();
    }

    @Override
    public void onUpdateOp(ServerUpdateOpPacket packet) {

    }

    @Override
    public void onSetClickDistance(ServerSetClickDistancePacket packet) {
        System.out.println("Reach distance updated to " + packet.getDistance());
        client.runOnMainThread(() -> {
            client.setReachDistance(packet.getDistance());
        });
    }

    public CPEExtensions getExtensions() {
        return extensions;
    }
}
