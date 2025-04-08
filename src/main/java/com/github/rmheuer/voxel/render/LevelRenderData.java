package com.github.rmheuer.voxel.render;

import com.github.rmheuer.azalea.utils.SafeCloseable;
import com.github.rmheuer.voxel.level.LevelSection;

public final class LevelRenderData implements SafeCloseable {
    private final int sectionsX, sectionsY, sectionsZ;
    private final SectionRenderData[] sections;

    public LevelRenderData(int sectionsX, int sectionsY, int sectionsZ) {
        this.sectionsX = sectionsX;
        this.sectionsY = sectionsY;
        this.sectionsZ = sectionsZ;

        int sectionCount = sectionsX * sectionsY * sectionsZ;
        sections = new SectionRenderData[sectionCount];
        for (int i = 0; i < sectionCount; i++) {
            sections[i] = new SectionRenderData();
        }
    }

    public SectionRenderData getSection(int sectionX, int sectionY, int sectionZ) {
        int index = sectionX + sectionZ * sectionsX + sectionY * sectionsX * sectionsZ;
        return sections[index];
    }

    private void markSectionOutdated(int sectionX, int sectionY, int sectionZ) {
        getSection(sectionX, sectionY, sectionZ).markOutdated();
    }

    public void blockChanged(int blockX, int blockY, int blockZ) {
        int sectionX = blockX / LevelSection.SIZE;
        int sectionY = blockY / LevelSection.SIZE;
        int sectionZ = blockZ / LevelSection.SIZE;
        int relX = blockX % LevelSection.SIZE;
        int relY = blockY % LevelSection.SIZE;
        int relZ = blockZ % LevelSection.SIZE;

        markSectionOutdated(sectionX, sectionY, sectionZ);

        boolean faceNX = sectionX > 0 && relX == 0;
        boolean faceNY = sectionY > 0 && relY == 0;
        boolean faceNZ = sectionZ > 0 && relZ == 0;
        boolean facePX = sectionX < sectionsX - 1 && relX == LevelSection.SIZE - 1;
        boolean facePY = sectionY < sectionsY - 1 && relY == LevelSection.SIZE - 1;
        boolean facePZ = sectionZ < sectionsZ - 1 && relZ == LevelSection.SIZE - 1;

        // Check neighbors along faces
        if (faceNX)
            markSectionOutdated(sectionX - 1, sectionY, sectionZ);
        if (faceNY)
            markSectionOutdated(sectionX, sectionY - 1, sectionZ);
        if (faceNZ)
            markSectionOutdated(sectionX, sectionY, sectionZ - 1);
        if (facePX)
            markSectionOutdated(sectionX + 1, sectionY, sectionZ);
        if (facePY)
            markSectionOutdated(sectionX, sectionY + 1, sectionZ);
        if (facePZ)
            markSectionOutdated(sectionX, sectionY, sectionZ + 1);

        // Check neighbors along edges
        // TODO

        // Check neighbors along vertices
        if (faceNX && faceNY && faceNZ)
            markSectionOutdated(sectionX - 1, sectionY - 1, sectionZ - 1);
        if (facePX && faceNY && faceNZ)
            markSectionOutdated(sectionX + 1, sectionY - 1, sectionZ - 1);
        if (faceNX && facePY && faceNZ)
            markSectionOutdated(sectionX - 1, sectionY + 1, sectionZ - 1);
        if (facePX && facePY && faceNZ)
            markSectionOutdated(sectionX + 1, sectionY + 1, sectionZ - 1);
        if (faceNX && faceNY && facePZ)
            markSectionOutdated(sectionX - 1, sectionY - 1, sectionZ + 1);
        if (facePX && faceNY && facePZ)
            markSectionOutdated(sectionX + 1, sectionY - 1, sectionZ + 1);
        if (faceNX && facePY && facePZ)
            markSectionOutdated(sectionX - 1, sectionY + 1, sectionZ + 1);
        if (facePX && facePY && facePZ)
            markSectionOutdated(sectionX + 1, sectionY + 1, sectionZ + 1);
    }

    @Override
    public void close() {
        for (SectionRenderData section : sections) {
            section.close();
        }
    }
}
