package com.github.rmheuer.voxel.network;

import com.github.rmheuer.voxel.network.packet.*;
import com.github.rmheuer.voxel.network.cpe.packet.ServerBulkBlockUpdatePacket;

public interface ServerPacketListener extends BidiPacketListener {
    void onServerId(ServerIdPacket packet);

    void onPing();

    void onLevelInit();

    void onLevelDataChunk(ServerLevelDataChunkPacket packet);

    void onLevelFinalize(ServerLevelFinalizePacket packet);

    void onSetBlock(ServerSetBlockPacket packet);

    void onBulkBlockUpdate(ServerBulkBlockUpdatePacket packet);

    void onSpawnPlayer(ServerSpawnPlayerPacket packet);

    void onRelativeMoveAndLook(ServerRelativeMoveAndLookPacket packet);

    void onRelativeMove(ServerRelativeMovePacket packet);

    void onLook(ServerLookPacket packet);

    void onDespawnPlayer(ServerDespawnPlayerPacket packet);

    void onDisconnect(ServerDisconnectPacket packet);

    void onUpdateOp(ServerUpdateOpPacket packet);
}
