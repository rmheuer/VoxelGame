package com.github.rmheuer.voxel.level;

public final class Level {
    private final int sectionsX, sectionsY, sectionsZ;
    private final int blocksX, blocksY, blocksZ;
    private final LevelSection[] sections;

    public Level(int sectionsX, int sectionsY, int sectionsZ) {
        this.sectionsX = sectionsX;
        this.sectionsY = sectionsY;
        this.sectionsZ = sectionsZ;
        blocksX = sectionsX * LevelSection.SIZE;
        blocksY = sectionsY * LevelSection.SIZE;
        blocksZ = sectionsZ * LevelSection.SIZE;

        int sectionCount = sectionsX * sectionsY * sectionsZ;

        sections = new LevelSection[sectionCount];
        for (int i = 0; i < sectionCount; i++) {
            sections[i] = new LevelSection();
        }
    }

    private int sectionIndex(int sectionX, int sectionY, int sectionZ) {
        return sectionX + sectionZ * sectionsX + sectionY * sectionsX * sectionsZ;
    }

    public LevelSection getSection(int sectionX, int sectionY, int sectionZ) {
        return sections[sectionIndex(sectionX, sectionY, sectionZ)];
    }

    public LevelSection getSectionContainingBlock(int blockX, int blockY, int blockZ) {
        int sectionX = blockX / LevelSection.SIZE;
        int sectionY = blockY / LevelSection.SIZE;
        int sectionZ = blockZ / LevelSection.SIZE;

        return getSection(sectionX, sectionY, sectionZ);
    }

    public byte getBlockId(int blockX, int blockY, int blockZ) {
        LevelSection section = getSectionContainingBlock(blockX, blockY, blockZ);
        int relX = blockX % LevelSection.SIZE;
        int relY = blockY % LevelSection.SIZE;
        int relZ = blockZ % LevelSection.SIZE;

        return section.getBlockId(relX, relY, relZ);
    }

    public byte setBlockId(int blockX, int blockY, int blockZ, byte newBlockId) {
        LevelSection section = getSectionContainingBlock(blockX, blockY, blockZ);
        int relX = blockX % LevelSection.SIZE;
        int relY = blockY % LevelSection.SIZE;
        int relZ = blockZ % LevelSection.SIZE;

        return section.setBlockId(relX, relY, relZ, newBlockId);
    }

    public boolean isBlockInBounds(int blockX, int blockY, int blockZ) {
        return blockX >= 0 && blockX < blocksX
                && blockY >= 0 && blockY < blocksY
                && blockZ >= 0 && blockZ < blocksZ;
    }

    public int getSectionsX() {
        return sectionsX;
    }

    public int getSectionsY() {
        return sectionsY;
    }

    public int getSectionsZ() {
        return sectionsZ;
    }

    public int getBlocksX() {
        return blocksX;
    }

    public int getBlocksY() {
        return blocksY;
    }

    public int getBlocksZ() {
        return blocksZ;
    }
}
