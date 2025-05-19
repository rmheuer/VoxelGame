package com.github.rmheuer.voxel.server;

import com.github.rmheuer.azalea.math.MathUtil;
import com.github.rmheuer.nbtlib.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

// https://web.archive.org/web/20240507001809/https://wiki.vg/ClassicWorld_file_format
public final class ClassicWorldFile {
    public static final class CreatorInfo {
        public final String service;
        public final String username;

        public CreatorInfo(String service, String username) {
            this.service = service;
            this.username = username;
        }
    }

    public static final class GeneratorInfo {
        public final String software;
        public final String generatorName;

        public GeneratorInfo(String software, String generatorName) {
            this.software = software;
            this.generatorName = generatorName;
        }
    }

    public static final class SpawnInfo {
        public final float x, y, z;
        public final float yaw, pitch;

        public SpawnInfo(float x, float y, float z, float yaw, float pitch) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }

    public static ClassicWorldFile loadFromFile(String filename) throws IOException {
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(new FileInputStream(filename)))) {
            NbtTagCompound root = NbtIO.readTagFile(in);
            return new ClassicWorldFile(root);
        }
    }

    private final String name;
    private final UUID uuid;
    private final short sizeX, sizeY, sizeZ;
    private SpawnInfo spawnInfo;
    private byte[] blockData;

    private final CreatorInfo creatorInfo;
    private final GeneratorInfo generatorInfo;
    private Long timeCreated;
    private Long timeLastAccessed;
    private Long timeLastModified;

    private final NbtTagCompound metadata;

    public ClassicWorldFile(String name, UUID uuid, short sizeX, short sizeY, short sizeZ, CreatorInfo creatorInfo, GeneratorInfo generatorInfo, SpawnInfo spawnInfo) {
        this.name = name;
        this.uuid = uuid;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.creatorInfo = creatorInfo;
        this.generatorInfo = generatorInfo;
        this.spawnInfo = spawnInfo;

        long timestamp = Instant.now().getEpochSecond();
        this.timeCreated = timestamp;
        this.timeLastModified = timestamp;

        this.metadata = new NbtTagCompound();
    }

    public ClassicWorldFile(NbtTagCompound root) {
        NbtTagCompound world = (NbtTagCompound) root.get("ClassicWorld");

        name = ((NbtTagString) world.get("Name")).getValue();
        uuid = uuidFromBytes(((NbtTagByteArray) world.get("UUID")).getValue());
        sizeX = ((NbtTagShort) world.get("X")).getValue();
        sizeY = ((NbtTagShort) world.get("Y")).getValue();
        sizeZ = ((NbtTagShort) world.get("Z")).getValue();

        if (world.containsKey("CreatedBy")) {
            NbtTagCompound createdBy = (NbtTagCompound) world.get("CreatedBy");
            creatorInfo = new CreatorInfo(
                    ((NbtTagString) createdBy.get("Username")).getValue(),
                    ((NbtTagString) createdBy.get("Service")).getValue()
            );
        } else {
            creatorInfo = null;
        }

        if (world.containsKey("MapGenerator")) {
            NbtTagCompound mapGenerator = (NbtTagCompound) world.get("MapGenerator");
            generatorInfo = new GeneratorInfo(
                    ((NbtTagString) mapGenerator.get("Software")).getValue(),
                    ((NbtTagString) mapGenerator.get("MapGeneratorName")).getValue()
            );
        } else {
            generatorInfo = null;
        }

        if (world.containsKey("TimeCreated"))
            timeCreated = ((NbtTagLong) world.get("TimeCreated")).getValue();
        if (world.containsKey("LastAccessed"))
            timeLastAccessed = ((NbtTagLong) world.get("LastAccessed")).getValue();
        if (world.containsKey("LastModified"))
            timeLastModified = ((NbtTagLong) world.get("LastModified")).getValue();

        NbtTagCompound spawn = (NbtTagCompound) world.get("Spawn");
        spawnInfo = new SpawnInfo(
                ((NbtTagShort) spawn.get("X")).getValue() / 32.0f,
                ((NbtTagShort) spawn.get("Y")).getValue() / 32.0f,
                ((NbtTagShort) spawn.get("Z")).getValue() / 32.0f,
                parseAngle(((NbtTagByte) spawn.get("H")).getValue()),
                parseAngle(((NbtTagByte) spawn.get("P")).getValue())
        );

        blockData = ((NbtTagByteArray) world.get("BlockArray")).getValue();
        metadata = (NbtTagCompound) world.get("Metadata");
    }

    public NbtTagCompound save() {
        NbtTagCompound world = new NbtTagCompound();
        world.add("FormatVersion", new NbtTagByte((byte) 1));
        world.add("Name", new NbtTagString(name));
        world.add("UUID", new NbtTagByteArray(uuidToBytes(uuid)));
        world.add("X", new NbtTagShort(sizeX));
        world.add("Y", new NbtTagShort(sizeY));
        world.add("Z", new NbtTagShort(sizeZ));

        if (creatorInfo != null) {
            NbtTagCompound createdBy = new NbtTagCompound();
            createdBy.add("Service", new NbtTagString(creatorInfo.service));
            createdBy.add("Username", new NbtTagString(creatorInfo.service));
            world.add("CreatedBy", createdBy);
        }
        if (generatorInfo != null) {
            NbtTagCompound mapGenerator = new NbtTagCompound();
            mapGenerator.add("Software", new NbtTagString(generatorInfo.software));
            mapGenerator.add("MapGeneratorName", new NbtTagString(generatorInfo.generatorName));
            world.add("MapGenerator", mapGenerator);
        }

        if (timeCreated != null)
            world.add("TimeCreated", new NbtTagLong(timeCreated));
        if (timeLastAccessed != null)
            world.add("LastAccessed", new NbtTagLong(timeLastAccessed));
        if (timeLastModified != null)
            world.add("LastModified", new NbtTagLong(timeLastModified));

        NbtTagCompound spawn = new NbtTagCompound();
        spawn.add("X", new NbtTagShort((short) (spawnInfo.x * 32)));
        spawn.add("Y", new NbtTagShort((short) (spawnInfo.y * 32)));
        spawn.add("Z", new NbtTagShort((short) (spawnInfo.z * 32)));
        spawn.add("H", new NbtTagByte(serializeAngle(spawnInfo.yaw)));
        spawn.add("P", new NbtTagByte(serializeAngle(spawnInfo.pitch)));
        world.add("Spawn", spawn);

        world.add("BlockArray", new NbtTagByteArray(blockData));
        world.add("Metadata", metadata);

        NbtTagCompound root = new NbtTagCompound();
        root.add("ClassicWorld", world);
        return root;
    }

    public void saveToFile(String filename) throws IOException {
        try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(filename)))) {
            NbtIO.writeTagFile(save(), out);
        }
    }

    private UUID uuidFromBytes(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long msb = bb.getLong();
        long lsb = bb.getLong();
        return new UUID(msb, lsb);
    }

    private byte[] uuidToBytes(UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    private float parseAngle(byte value) {
        int unsigned = value & 0xFF;
        return -unsigned / 256.0f * (float) Math.PI * 2;
    }

    private byte serializeAngle(float angle) {
        float scaled = -angle / ((float) Math.PI * 2) * 256.0f;
        int wrapped = (int) MathUtil.wrap(scaled, 0, 256);
        return (byte) wrapped;
    }

    public void setBlockData(byte[] blockData) {
        this.blockData = blockData;
        timeLastModified = Instant.now().getEpochSecond();
    }

    public void setSpawnInfo(SpawnInfo spawnInfo) {
        this.spawnInfo = spawnInfo;
    }

    public void markAccessed() {
        timeLastAccessed = Instant.now().getEpochSecond();
    }

    public String getName() {
        return name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public short getSizeX() {
        return sizeX;
    }

    public short getSizeY() {
        return sizeY;
    }

    public short getSizeZ() {
        return sizeZ;
    }

    public byte[] getBlockData() {
        return blockData;
    }

    public SpawnInfo getSpawnInfo() {
        return spawnInfo;
    }

    public CreatorInfo getCreatorInfo() {
        return creatorInfo;
    }

    public GeneratorInfo getGeneratorInfo() {
        return generatorInfo;
    }

    public Long getTimeCreated() {
        return timeCreated;
    }

    public Long getTimeLastAccessed() {
        return timeLastAccessed;
    }

    public Long getTimeLastModified() {
        return timeLastModified;
    }

    public NbtTagCompound getMetadata() {
        return metadata;
    }
}
