package com.github.rmheuer.voxel.server;

import com.github.rmheuer.azalea.math.MathUtil;
import com.github.rmheuer.voxel.block.Blocks;
import com.github.rmheuer.voxel.level.BlockMap;
import com.github.rmheuer.voxel.network.ClientPacketListener;
import com.github.rmheuer.voxel.network.Connection;
import com.github.rmheuer.voxel.network.packet.*;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.joml.Vector3f;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;

public final class ClientConnection extends Connection<ClientPacket, ServerPacket> implements ClientPacketListener {
    private static final int PING_INTERVAL = 20;

    private final GameServer server;
    private byte playerId;
    private String username;

    private Vector3f position;
    private float pitch, yaw;

    private int pingTimer;

    public ClientConnection(GameServer server, Channel channel) {
        super(channel);

        this.server = server;
        playerId = -1;
        username = null;

        pingTimer = PING_INTERVAL;
    }

    public void tick() {
        if (!isConnected()) {
            printNamed("Client disconnected");
            close();

            server.broadcastPacketToOthers(new ServerDespawnPlayerPacket(playerId), this);
            server.removeClient(playerId);

            server.broadcastSystemMessage(username + " left the game");

            return;
        }

        if (pingTimer-- <= 0) {
            pingTimer = PING_INTERVAL;

            sendPacket(ServerPingPacket.INSTANCE);

            // Refresh absolute position to prevent desync
            server.broadcastPacketToOthers(new BidiPlayerPositionPacket(
                    playerId,
                    position.x, position.y, position.z,
                    yaw, pitch
            ), this);
        }
    }

    @Override
    protected void dispatchPacket(ClientPacket packet) {
        packet.handleClient(this);
    }

    private ServerSpawnPlayerPacket makeSpawnPlayerPacket() {
        return new ServerSpawnPlayerPacket(
                playerId, username,
                position.x, position.y, position.z,
                yaw, pitch
        );
    }

    @Override
    public void onPlayerId(ClientPlayerIdPacket packet) {
        System.out.println("Client username is " + packet.getUsername());
        username = packet.getUsername();

        if (packet.getProtocolVersion() != 7) {
            kick("Protocol version mismatch");
            return;
        }

        playerId = server.addClient(this);
        sendPacket(new ServerIdPacket((short) 7, "Test Server", "", true));

        position = new Vector3f(32, 32, 32);
        yaw = 0;
        pitch = 0;

        // Tell client about itself
        sendPacket(new ServerSpawnPlayerPacket((byte) -1, packet.getUsername(), 32, 32, 32, 0, 0));

        // Tell client about the other players on the server
        for (ClientConnection client : server.getAllClients()) {
            if (client == this)
                continue;

            sendPacket(client.makeSpawnPlayerPacket());
        }

        // Tell others that we joined
        server.broadcastPacketToOthers(makeSpawnPlayerPacket(), this);

        server.broadcastSystemMessage(username + " joined the game");

        long prev = System.nanoTime();
        BlockMap map = server.getCopyOfMap();
        System.out.println(System.nanoTime() - prev);
        byte[] rawMapData = map.packBlockDataForNetwork();

        ByteArrayOutputStream b = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(b)) {
            DataOutputStream out = new DataOutputStream(gzip);
            out.writeInt(rawMapData.length);
            out.write(rawMapData);
            gzip.finish();
        } catch (IOException e) {
            throw new RuntimeException("Failed to compress level data");
        }
        byte[] mapData = b.toByteArray();

        sendPacket(ServerLevelInitPacket.INSTANCE);
        for (int i = 0; i < MathUtil.ceilDiv(mapData.length, 1024); i++) {
            int start = i * 1024;
            byte[] section = new byte[Math.min(1024, mapData.length - start)];
            System.arraycopy(mapData, start, section, 0, section.length);

            int percent = (100 * (start + section.length)) / mapData.length;
            sendPacket(new ServerLevelDataChunkPacket(section, (short) percent));
        }
        sendPacket(new ServerLevelFinalizePacket((short) 64, (short) 64, (short) 64));

        sendPacket(new BidiPlayerPositionPacket((byte) -1, 32, 32, 32, 0, 0));
    }

    @Override
    public void onSetBlock(ClientSetBlockPacket packet) {
        byte newId = packet.getMode() == ClientSetBlockPacket.Mode.PLACED
                ? packet.getBlockId()
                : Blocks.ID_AIR;
        server.setBlock(packet.getX(), packet.getY(), packet.getZ(), newId);
    }

    @Override
    public void onPlayerPosition(BidiPlayerPositionPacket packet) {
        float deltaX = packet.getX() - position.x;
        float deltaY = packet.getY() - position.y;
        float deltaZ = packet.getZ() - position.z;
        float maxDelta = Math.max(Math.max(Math.abs(deltaX), Math.abs(deltaY)), Math.abs(deltaZ));

        boolean movedXYZ = maxDelta > 0;
        boolean turned = packet.getPitch() != pitch || packet.getYaw() != yaw;

        if (maxDelta > 3) {
            server.broadcastPacketToOthers(new BidiPlayerPositionPacket(
                    playerId,
                    packet.getX(), packet.getY(), packet.getZ(),
                    packet.getYaw(), packet.getPitch()
            ), this);
        } else if (movedXYZ) {
            if (turned) {
                server.broadcastPacketToOthers(new ServerRelativeMoveAndLookPacket(
                        playerId,
                        deltaX, deltaY, deltaZ,
                        packet.getYaw(), packet.getPitch()
                ), this);
            } else {
                server.broadcastPacketToOthers(new ServerRelativeMovePacket(
                        playerId,
                        deltaX, deltaY, deltaZ
                ), this);
            }
        } else if (turned) {
            server.broadcastPacketToOthers(new ServerLookPacket(
                    playerId,
                    packet.getYaw(), packet.getPitch()
            ), this);
        }

        position.set(packet.getX(), packet.getY(), packet.getZ());
        pitch = packet.getPitch();
        yaw = packet.getYaw();
    }

    @Override
    public void onChatMessage(BidiChatMessagePacket packet) {
        String msg = username + ": " + packet.getMessage();
        BidiChatMessagePacket broadcast = new BidiChatMessagePacket(playerId, msg);
        server.broadcastPacketToAll(broadcast);

        System.out.println("[CHAT] " + msg);
    }

    private void printNamed(String message) {
        String name = username != null ? username : "{unknown}";
        System.out.println(name + ": " + message);
    }

    @Override
    public void sendPacket(ServerPacket packet) {
        super.sendPacket(packet);
        printNamed("Sent " + packet);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        printNamed("Received " + msg);
        super.channelRead(ctx, msg);
    }

    public void kick(String reason) {
        printNamed("Kicking for: " + reason);
        sendPacket(new ServerDisconnectPacket(reason));
        close();
    }
}
