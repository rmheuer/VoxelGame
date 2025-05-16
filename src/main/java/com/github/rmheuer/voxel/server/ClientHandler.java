package com.github.rmheuer.voxel.server;

import com.github.rmheuer.azalea.math.MathUtil;
import com.github.rmheuer.voxel.block.Blocks;
import com.github.rmheuer.voxel.network.ClientPacketListener;
import com.github.rmheuer.voxel.network.packet.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;

public final class ClientHandler implements ClientPacketListener {
    private final ClientConnection conn;

    public ClientHandler(ClientConnection conn) {
        this.conn = conn;
    }

    @Override
    public void onPlayerId(ClientPlayerIdPacket packet) {
        System.out.println("Client username is " + packet.getUsername());

        if (packet.getProtocolVersion() != 7) {
            conn.kick("Protocol version mismatch");
            return;
        }

        conn.sendPacket(new ServerIdPacket((short) 7, "Test Server", "", true));
        conn.sendPacket(new ServerSpawnPlayerPacket((byte) -1, packet.getUsername(), 32, 32, 32, 0, 0));

        byte[] fakeMap = new byte[64*64*64];
        Arrays.fill(fakeMap, Blocks.ID_AIR);
        Arrays.fill(fakeMap, 0, 64 * 64, Blocks.ID_GRASS);

        ByteArrayOutputStream b = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(b)) {
            DataOutputStream out = new DataOutputStream(gzip);
            out.writeInt(fakeMap.length);
            out.write(fakeMap);
            gzip.finish();
        } catch (IOException e) {
            throw new RuntimeException("Failed to compress level data");
        }
        byte[] mapData = b.toByteArray();

        conn.sendPacket(ServerLevelInitPacket.INSTANCE);
        for (int i = 0; i < MathUtil.ceilDiv(mapData.length, 1024); i++) {
            int start = i * 1024;
            byte[] section = new byte[Math.min(1024, mapData.length - start)];
            System.arraycopy(mapData, start, section, 0, section.length);

            int percent = (100 * (start + section.length)) / mapData.length;
            conn.sendPacket(new ServerLevelDataChunkPacket(section, (short) percent));
        }
        conn.sendPacket(new ServerLevelFinalizePacket((short) 64, (short) 64, (short) 64));

        conn.sendPacket(new BidiPlayerPositionPacket((byte) -1, 32, 32, 32, 0, 0));
    }

    @Override
    public void onSetBlock(ClientSetBlockPacket packet) {

    }

    @Override
    public void onPlayerPosition(BidiPlayerPositionPacket packet) {

    }

    @Override
    public void onChatMessage(BidiChatMessagePacket packet) {

    }
}
