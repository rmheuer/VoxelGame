package com.github.rmheuer.voxel.server;

import com.github.rmheuer.azalea.math.CubeFace;
import com.github.rmheuer.voxel.block.Block;
import com.github.rmheuer.voxel.block.Blocks;
import com.github.rmheuer.voxel.level.BlockMap;
import com.github.rmheuer.voxel.level.LevelAccess;
import com.github.rmheuer.voxel.level.MapSection;
import com.github.rmheuer.voxel.network.PacketDecoder;
import com.github.rmheuer.voxel.network.PacketEncoder;
import com.github.rmheuer.voxel.network.PacketMapping;
import com.github.rmheuer.voxel.network.PacketRegistry;
import com.github.rmheuer.voxel.network.packet.BidiChatMessagePacket;
import com.github.rmheuer.voxel.network.packet.ClientPacket;
import com.github.rmheuer.voxel.network.packet.ServerPacket;
import com.github.rmheuer.voxel.network.packet.ServerSetBlockPacket;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class GameServer implements LevelAccess {
    private static final String LEVEL_FILE = "server_level.cw";
    private static final int AUTOSAVE_INTERVAL_TICKS = 60 * 20;

    private static CubeFace[] FACES = CubeFace.values();

    private final Queue<String> consoleInputQueue;
    private final ConsoleInputThread consoleThread;

    private NetworkConnectionHandler networkConnectionHandler;
    private LocalConnectionHandler localConnectionHandler;

    private final Map<Byte, ClientConnection> clients;
    private final ClassicWorldFile levelFile;
    private final BlockMap map;

    private volatile boolean running;

    private CompletableFuture<Void> autosaveFuture;
    private int autosaveTimer;

    public GameServer() throws Exception {
        consoleInputQueue = new ConcurrentLinkedQueue<>();
        consoleThread = new ConsoleInputThread(consoleInputQueue);

        clients = new ConcurrentHashMap<>();

        ClassicWorldFile levelFile;
        BlockMap map;
        try {
            levelFile = ClassicWorldFile.loadFromFile(LEVEL_FILE);
            map = new BlockMap(
                    levelFile.getSizeX() / MapSection.SIZE,
                    levelFile.getSizeY() / MapSection.SIZE,
                    levelFile.getSizeZ() / MapSection.SIZE,
                    levelFile.getBlockData()
            );
        } catch (FileNotFoundException e) {
            System.err.println("Level file " + LEVEL_FILE + " not found, generating new level");

            map = LevelGenerator.generateLevel(128 / 16);

            levelFile = new ClassicWorldFile(
                    "Server Level",
                    UUID.randomUUID(),
                    (short) map.getBlocksX(),
                    (short) map.getBlocksY(),
                    (short) map.getBlocksZ(),
                    null,
                    new ClassicWorldFile.GeneratorInfo("VoxelGame", "default"),
                    new ClassicWorldFile.SpawnInfo(map.getBlocksX() / 2f, 64, map.getBlocksZ() / 2f, 0, 0)
            );
        }
        this.levelFile = levelFile;
        this.map = map;
    }

    public void openToNetwork(int port) throws Exception {
        networkConnectionHandler = new NetworkConnectionHandler(this, port);
    }

    public void openLocally(LocalAddress addr) throws Exception {
        localConnectionHandler = new LocalConnectionHandler(this, addr);
    }

    public byte addClient(ClientConnection client) {
        // Try each player ID sequentially until we find an available one
        // There's probably a better way to do this
        byte id = 0;
        while (true) {
            if (clients.putIfAbsent(id, client) == null) {
                return id;
            }

            if (id == 127)
                id = 0;
            else
                id++;
        }
    }

    public void removeClient(byte playerId) {
        clients.remove(playerId);
        levelFile.markAccessed();
    }

    public Collection<ClientConnection> getAllClients() {
        return clients.values();
    }

    public void broadcastPacketToAll(ServerPacket packet) {
        for (ClientConnection client : clients.values()) {
            client.sendPacket(packet);
        }
    }

    public void broadcastPacketToOthers(ServerPacket packet, ClientConnection excluded) {
        for (ClientConnection client : clients.values()) {
            if (client == excluded)
                continue;

            client.sendPacket(packet);
        }
    }

    public void broadcastSystemMessage(String message) {
        broadcastPacketToAll(new BidiChatMessagePacket((byte) -1, message));
    }

    public ClassicWorldFile.SpawnInfo getSpawnInfo() {
        return levelFile.getSpawnInfo();
    }

    public BlockMap getCopyOfMap() {
        synchronized (map) {
            return new BlockMap(map);
        }
    }

    @Override
    public byte getBlockId(int x, int y, int z) {
        synchronized (map) {
            return map.getBlockId(x, y, z);
        }
    }

    @Override
    public void setBlockId(int x, int y, int z, byte blockId) {
        synchronized (map) {
            map.setBlockId(x, y, z, blockId);
            broadcastPacketToAll(new ServerSetBlockPacket((short) x, (short) y, (short) z, blockId));

            Block placed = Blocks.getBlock(blockId);
            if (placed.getNeighborUpdateBehavior() != null)
                placed.getNeighborUpdateBehavior().doAction(this, x, y, z, blockId);

            updateNeighbors(x, y, z);
        }
    }

    @Override
    public void setBlockIdNoNeighborUpdates(int x, int y, int z, byte blockId) {
        synchronized (map) {
            map.setBlockId(x, y, z, blockId);
            broadcastPacketToAll(new ServerSetBlockPacket((short) x, (short) y, (short) z, blockId));
        }
    }

    @Override
    public void updateNeighbors(int x, int y, int z) {
        synchronized (map) {
            for (CubeFace face : FACES) {
                int nx = x + face.x;
                int ny = y + face.y;
                int nz = z + face.z;
                if (map.isBlockInBounds(nx, ny, nz)) {
                    byte neighborId = map.getBlockId(nx, ny, nz);
                    Block neighbor = Blocks.getBlock(neighborId);
                    if (neighbor.getNeighborUpdateBehavior() != null)
                        neighbor.getNeighborUpdateBehavior().doAction(this, nx, ny, nz, neighborId);
                }
            }
        }
    }

    public void placeBlock(int x, int y, int z, byte blockId) {
        Block block = Blocks.getBlock(blockId);
        block.getPlacementBehavior().doAction(this, x, y, z, blockId);
    }

    public void breakBlock(int x, int y, int z) {
        if (y >= 29 && y < 32 && (x == 0 || x == map.getBlocksX() - 1 || z == 0 || z == map.getBlocksZ() - 1)) {
            setBlockId(x, y, z, Blocks.ID_STILL_WATER);
        } else {
            setBlockId(x, y, z, Blocks.ID_AIR);
        }
    }

    private void handleConsoleCommand(String command) {
        if (command.equals("stop")) {
            running = false;
        }
    }

    private void tick() {
        for (ClientConnection client : clients.values()) {
            client.tick();
        }

        if (autosaveTimer++ >= AUTOSAVE_INTERVAL_TICKS) {
            autosaveTimer = 0;
            startAutosave();
        }
    }

    public void run() {
        consoleThread.start();

        long prevTime = System.nanoTime();
        double unprocessedTime = 0;
        double tickInterval = 1 / 20.0;

        running = true;
        while (running) {
            String consoleCommand;
            while ((consoleCommand = consoleInputQueue.poll()) != null) {
                handleConsoleCommand(consoleCommand);
            }

            long time = System.nanoTime();
            unprocessedTime += (time - prevTime) / 1_000_000_000.0;
            prevTime = time;
            unprocessedTime = Math.min(unprocessedTime, 1.0); // Don't get too far behind
            while (unprocessedTime > tickInterval) {
                unprocessedTime -= tickInterval;
                tick();
            }

            // Nothing to do...
            try {
                Thread.sleep(5);
            } catch (InterruptedException ignored) {}
        }

        System.out.println("Server shutting down");
        if (networkConnectionHandler != null)
            networkConnectionHandler.close();
        if (localConnectionHandler != null)
            localConnectionHandler.close();

        System.out.println("Disconnecting clients");
        for (ClientConnection client : clients.values()) {
            client.kick("Server closed");
        }

        levelFile.markAccessed();
        if (autosaveFuture != null)
            autosaveFuture.join();
        saveLevel(map);

        consoleThread.close();
    }

    private void startAutosave() {
        // If previous one hasn't finished, don't start another
        if (autosaveFuture != null && !autosaveFuture.isDone())
            return;

        if (!clients.isEmpty())
            levelFile.markAccessed();
        BlockMap mapCopy = getCopyOfMap();

        autosaveFuture = new CompletableFuture<>();
        new Thread(() -> {
            saveLevel(mapCopy);
            autosaveFuture.complete(null);
        }).start();
    }

    private void saveLevel(BlockMap map) {
        levelFile.setBlockData(map.packBlockData());

        try {
            System.out.println("Saving level");
            levelFile.saveToFile(LEVEL_FILE);
        } catch (IOException e) {
            System.err.println("Failed to save level!");
            e.printStackTrace();
        }
    }

    public void stop() {
        running = false;
    }

    public static void main(String[] args) throws Exception {
        int port = 25565;
        if (args.length > 0)
            port = Integer.parseInt(args[0]);

        GameServer server = new GameServer();
        server.openToNetwork(port);
        server.run();
    }
}
