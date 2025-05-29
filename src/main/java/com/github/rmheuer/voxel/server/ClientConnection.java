package com.github.rmheuer.voxel.server;

import com.github.rmheuer.azalea.math.MathUtil;
import com.github.rmheuer.azalea.render.Colors;
import com.github.rmheuer.voxel.level.BlockMap;
import com.github.rmheuer.voxel.network.ClientPacketListener;
import com.github.rmheuer.voxel.network.Connection;
import com.github.rmheuer.voxel.network.PacketDataBuf;
import com.github.rmheuer.voxel.network.cpe.CPEExtensions;
import com.github.rmheuer.voxel.network.cpe.packet.ServerSetTextColorPacket;
import com.github.rmheuer.voxel.network.packet.*;
import com.github.rmheuer.voxel.network.cpe.packet.BidiExtEntryPacket;
import com.github.rmheuer.voxel.network.cpe.packet.BidiExtInfoPacket;
import com.github.rmheuer.voxel.network.cpe.packet.ServerSetClickDistancePacket;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.joml.Vector3f;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public final class ClientConnection extends Connection<ClientPacket, ServerPacket> implements ClientPacketListener {
    private static final int PING_INTERVAL = 20;

    private final GameServer server;
    private byte playerId;
    private String username;

    private final CPEExtensions.ExtensionSet receivedClientExtensions;
    private int extEntryPacketsRemaining;
    private CPEExtensions extensions;

    private final StringBuilder partialChatMessage;
    
    private Vector3f position;
    private float pitch, yaw;

    private int pingTimer;

    public ClientConnection(GameServer server, Channel channel) {
        super(channel);

        this.server = server;
        playerId = -1;
        username = null;

        receivedClientExtensions = new CPEExtensions.ExtensionSet();

        partialChatMessage = new StringBuilder();

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

    @Override
    public void onPlayerId(ClientPlayerIdPacket packet) {
        System.out.println("Client username is " + packet.getUsername());
        username = packet.getUsername();

        if (packet.getProtocolVersion() != 7) {
            kick("Protocol version mismatch");
            return;
        }

        if (packet.getPadding() == CPEExtensions.HANDSHAKE_MAGIC_VALUE) {
            // Start CPE
            System.out.println("Client started CPE negotiation");

            List<CPEExtensions.ExtensionInfo> extensions = CPEExtensions.ALL_SUPPORTED;
            sendPacket(new BidiExtInfoPacket("VoxelGame", (short) extensions.size()));
            for (CPEExtensions.ExtensionInfo info : extensions) {
                sendPacket(new BidiExtEntryPacket(info.name, info.version));
            }
        } else {
            initPlayer();
        }
    }

    @Override
    public void onExtInfo(BidiExtInfoPacket packet) {
        System.out.println("Client CPE response");
        System.out.println("Client software: " + packet.getAppName());

        extEntryPacketsRemaining = packet.getExtensionCount();
        if (extEntryPacketsRemaining == 0)
            initPlayer();
    }

    @Override
    public void onExtEntry(BidiExtEntryPacket packet) {
        System.out.println("Client supports extension: " + packet.getExtName() + " version " + packet.getVersion());
        receivedClientExtensions.add(packet.getExtName(), packet.getVersion());

        extEntryPacketsRemaining--;
        if (extEntryPacketsRemaining == 0)
            initPlayer();
    }

    private ServerSpawnPlayerPacket makeSpawnPlayerPacket() {
        return new ServerSpawnPlayerPacket(
                playerId, username,
                position.x, position.y, position.z,
                yaw, pitch
        );
    }

    private void initPlayer() {
        extensions = new CPEExtensions(receivedClientExtensions);
        sendPacket(new ServerIdPacket((short) 7, "Test Server", "", true));

        if (extensions.clickDistance)
            sendPacket(new ServerSetClickDistancePacket(5.0f));
        
        ClassicWorldFile.SpawnInfo spawn = server.getSpawnInfo();

        position = new Vector3f(spawn.x, spawn.y, spawn.z);
        yaw = spawn.yaw;
        pitch = spawn.pitch;

        // Tell client about itself
        sendPacket(new ServerSpawnPlayerPacket((byte) -1, username, spawn.x, spawn.y, spawn.z, spawn.yaw, spawn.pitch));

        // Tell client about the other players on the server
        for (ClientConnection client : server.getAllClients()) {
            sendPacket(client.makeSpawnPlayerPacket());
        }

        // Tell others that we joined
        playerId = server.addClient(this);
        server.broadcastPacketToOthers(makeSpawnPlayerPacket(), this);
        server.broadcastSystemMessage(username + " joined the game");

        long prev = System.nanoTime();
        BlockMap map = server.getCopyOfMap();
        System.out.println(System.nanoTime() - prev);
        byte[] rawMapData = map.packBlockData();

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
        sendPacket(new ServerLevelFinalizePacket((short) map.getBlocksX(), (short) map.getBlocksY(), (short) map.getBlocksZ()));

        sendPacket(new BidiPlayerPositionPacket((byte) -1, spawn.x, spawn.y, spawn.z, spawn.yaw, spawn.pitch));

        if (extensions.textColors) {
            String message = "TextColors ext test";

            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < message.length(); i++) {
                float hue = MathUtil.map(i, 0, message.length(), 0, 360);
                int col = Colors.RGBA.fromHSV(hue, 1, 1);

                sendPacket(new ServerSetTextColorPacket(
                        Colors.RGBA.getRed(col),
                        Colors.RGBA.getGreen(col),
                        Colors.RGBA.getBlue(col),
                        Colors.RGBA.getAlpha(col),
                        (char) ('A' + i)
                ));

                builder.append('&');
                builder.append((char) ('A' + i));
                builder.append(message.charAt(i));
            }

            sendPacket(new BidiChatMessagePacket(playerId, builder.toString()));
        }
    }

    @Override
    public void onSetBlock(ClientSetBlockPacket packet) {
        if (packet.getMode() == ClientSetBlockPacket.Mode.PLACED) {
            server.placeBlock(packet.getX(), packet.getY(), packet.getZ(), packet.getBlockId());
        } else {
            server.breakBlock(packet.getX(), packet.getY(), packet.getZ());
        }
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

    private void dispatchChatMessage(String message) {
        String msg = username + ": " + message;
        System.out.println("[CHAT] " + msg);

        int partLen = PacketDataBuf.MAX_STRING_LEN;
        
        int partIndex = 0;
        while (partIndex + partLen < msg.length()) {
            String part = msg.substring(partIndex, partIndex + partLen);
            server.broadcastPacketToAll(new BidiChatMessagePacket(playerId, part));
            partIndex += partLen;
        }

        if (partIndex < msg.length()) {
            server.broadcastPacketToAll(new BidiChatMessagePacket(playerId, msg.substring(partIndex)));
        }
    }

    @Override
    public void onChatMessage(BidiChatMessagePacket packet) {
        if (extensions.longerMessages) {
            partialChatMessage.append(packet.getMessage());
            if (packet.getPlayerId() == 1)
                return;

            dispatchChatMessage(partialChatMessage.toString());
            partialChatMessage.setLength(0);
        } else {
            dispatchChatMessage(packet.getMessage());
        }
    }

    private void printNamed(String message) {
        String name = username != null ? username : "{unknown}";
        System.out.println(name + ": " + message);
    }

    @Override
    public void sendPacket(ServerPacket packet) {
        super.sendPacket(packet);
//        printNamed("Sent " + packet);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
//        printNamed("Received " + msg);
        super.channelRead(ctx, msg);
    }

    public void kick(String reason) {
        printNamed("Kicking for: " + reason);
        sendPacket(new ServerDisconnectPacket(reason));
        close();
    }
}
