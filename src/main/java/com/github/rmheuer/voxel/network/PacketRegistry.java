package com.github.rmheuer.voxel.network;

import com.github.rmheuer.voxel.network.cpe.packet.*;
import com.github.rmheuer.voxel.network.packet.*;

import java.util.function.Supplier;

public final class PacketRegistry {
    private static final PacketMapping<ServerPacket, ClientPacket> clientMapping = new PacketMapping<>();
    private static final PacketMapping<ClientPacket, ServerPacket> serverMapping = new PacketMapping<>();

    static {
        registerClient(0x00, ClientPlayerIdPacket.class, ClientPlayerIdPacket::new);
        registerClient(0x05, ClientSetBlockPacket.class, ClientSetBlockPacket::new);
        registerClient(0x08, BidiPlayerPositionPacket.class, BidiPlayerPositionPacket::new);
        registerClient(0x0d, BidiChatMessagePacket.class, BidiChatMessagePacket::new);

        registerServer(0x00, ServerIdPacket.class, ServerIdPacket::new);
        registerServer(0x01, ServerPingPacket.class, () -> ServerPingPacket.INSTANCE);
        registerServer(0x02, ServerLevelInitPacket.class, () -> ServerLevelInitPacket.INSTANCE);
        registerServer(0x03, ServerLevelDataChunkPacket.class, ServerLevelDataChunkPacket::new);
        registerServer(0x04, ServerLevelFinalizePacket.class, ServerLevelFinalizePacket::new);
        registerServer(0x06, ServerSetBlockPacket.class, ServerSetBlockPacket::new);
        registerServer(0x07, ServerSpawnPlayerPacket.class, ServerSpawnPlayerPacket::new);
        registerServer(0x08, BidiPlayerPositionPacket.class, BidiPlayerPositionPacket::new);
        registerServer(0x09, ServerRelativeMoveAndLookPacket.class, ServerRelativeMoveAndLookPacket::new);
        registerServer(0x0a, ServerRelativeMovePacket.class, ServerRelativeMovePacket::new);
        registerServer(0x0b, ServerLookPacket.class, ServerLookPacket::new);
        registerServer(0x0c, ServerDespawnPlayerPacket.class, ServerDespawnPlayerPacket::new);
        registerServer(0x0d, BidiChatMessagePacket.class, BidiChatMessagePacket::new);
        registerServer(0x0e, ServerDisconnectPacket.class, ServerDisconnectPacket::new);
        registerServer(0x0f, ServerUpdateOpPacket.class, ServerUpdateOpPacket::new);

        // CPE

        registerClient(0x10, BidiExtInfoPacket.class, BidiExtInfoPacket::new);
        registerClient(0x11, BidiExtEntryPacket.class, BidiExtEntryPacket::new);

        registerServer(0x10, BidiExtInfoPacket.class, BidiExtInfoPacket::new);
        registerServer(0x11, BidiExtEntryPacket.class, BidiExtEntryPacket::new);
        registerServer(0x12, ServerSetClickDistancePacket.class, ServerSetClickDistancePacket::new);
        registerServer(0x26, ServerBulkBlockUpdatePacket.class, ServerBulkBlockUpdatePacket::new);
        registerServer(0x27, ServerSetTextColorPacket.class, ServerSetTextColorPacket::new);
        registerServer(0x36, ServerExtEntityTeleportPacket.class, ServerExtEntityTeleportPacket::new);
    }

    private static <P extends ClientPacket> void registerClient(int id, Class<P> packetClass, Supplier<P> constructor) {
        clientMapping.registerOut(id, packetClass);
        serverMapping.registerIn(id, constructor);
    }

    private static <P extends ServerPacket> void registerServer(int id, Class<P> packetClass, Supplier<P> constructor) {
        clientMapping.registerIn(id, constructor);
        serverMapping.registerOut(id, packetClass);
    }

    public static PacketMapping<ServerPacket, ClientPacket> getClientMapping() {
        return clientMapping;
    }

    public static PacketMapping<ClientPacket, ServerPacket> getServerMapping() {
        return serverMapping;
    }
}
