package com.github.rmheuer.voxel.client.multiplayer_test;

import com.github.rmheuer.voxel.client.ServerConnection;
import com.github.rmheuer.voxel.client.ui.DownloadingTerrainUI;
import com.github.rmheuer.voxel.network.ServerPacketListener;
import com.github.rmheuer.voxel.network.packet.*;
import org.joml.Vector3i;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

public final class NetworkHandler implements ServerPacketListener {
    private final MultiplayerClientTest client;
    private final ServerConnection conn;

    private DownloadingTerrainUI downloadingTerrainUI;
    private final List<byte[]> receivedLevelChunks;

    public NetworkHandler(MultiplayerClientTest client, ServerConnection conn) {
        this.client = client;
        this.conn = conn;

        receivedLevelChunks = new ArrayList<>();

        conn.setPacketListener(this);
        conn.sendPacket(new ClientPlayerIdPacket(
                (short) 7,
                "TestPlayer",
                ""
        ));
    }

    @Override
    public void onServerId(ServerIdPacket packet) {
        System.out.println("Server name: " + packet.getServerName());
        System.out.println("Server MOTD: " + packet.getServerMotd());
    }

    @Override
    public void onPing() {

    }

    @Override
    public void onLevelInit() {
        downloadingTerrainUI = new DownloadingTerrainUI();
        receivedLevelChunks.clear();
        client.runOnMainThread(() -> {
            client.setUI(downloadingTerrainUI);
            client.clearLevel();
        });
    }

    @Override
    public void onLevelDataChunk(ServerLevelDataChunkPacket packet) {
        receivedLevelChunks.add(packet.getChunkData());
        client.runOnMainThread(() -> downloadingTerrainUI.setPercentReceived(packet.getPercentComplete()));
    }

    @Override
    public void onLevelFinalize(ServerLevelFinalizePacket packet) {
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
    public void onSpawnPlayer(ServerSpawnPlayerPacket packet) {
        if (packet.getPlayerId() == -1) {
            client.runOnMainThread(() -> client.resetPlayer(
                    packet.getX(), packet.getY(), packet.getZ(),
                    packet.getPitch(), packet.getYaw()
            ));
        }
    }

    @Override
    public void onPlayerPosition(BidiPlayerPositionPacket packet) {

    }

    @Override
    public void onRelativeMoveAndLook(ServerRelativeMoveAndLookPacket packet) {

    }

    @Override
    public void onRelativeMove(ServerRelativeMovePacket packet) {

    }

    @Override
    public void onLook(ServerLookPacket packet) {

    }

    @Override
    public void onDespawnPlayer(ServerDespawnPlayerPacket packet) {

    }

    @Override
    public void onChatMessage(BidiChatMessagePacket packet) {

    }

    @Override
    public void onDisconnect(ServerDisconnectPacket packet) {
        System.out.println("Kicked: " + packet.getReason());
        conn.disconnect();
    }

    @Override
    public void onUpdateOp(ServerUpdateOpPacket packet) {

    }
}
