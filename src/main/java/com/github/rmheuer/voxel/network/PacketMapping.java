package com.github.rmheuer.voxel.network;

import com.github.rmheuer.voxel.network.packet.Packet;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class PacketMapping<I extends Packet, O extends Packet> {
    private final Map<Integer, Supplier<? extends I>> inSuppliers;
    private final Map<Class<? extends O>, Integer> outPacketIds;

    public PacketMapping() {
        inSuppliers = new HashMap<>();
        outPacketIds = new HashMap<>();
    }

    public void registerIn(int id, Supplier<? extends I> supplier) {
        inSuppliers.put(id, supplier);
    }

    public void registerOut(int id, Class<? extends O> packetClass) {
        outPacketIds.put(packetClass, id);
    }

    public I createInPacket(int id) {
        Supplier<? extends I> supplier = inSuppliers.get(id);
        if (supplier == null)
            return null;
        return supplier.get();
    }

    public int getIdForOutPacket(Class<?> packetClass) {
        return outPacketIds.get(packetClass);
    }
}
