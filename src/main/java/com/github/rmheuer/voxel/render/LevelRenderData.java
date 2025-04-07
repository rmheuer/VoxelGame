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

        if (sectionX > 0 && relX == 0)
            markSectionOutdated(sectionX - 1, sectionY, sectionZ);
        if (sectionY > 0 && relY == 0)
            markSectionOutdated(sectionX, sectionY - 1, sectionZ);
        if (sectionZ > 0 && relZ == 0)
            markSectionOutdated(sectionX, sectionY, sectionZ - 1);
        if (sectionX < sectionsX - 1 && relX == LevelSection.SIZE - 1)
            markSectionOutdated(sectionX + 1, sectionY, sectionZ);
        if (sectionY < sectionsY - 1 && relY == LevelSection.SIZE - 1)
            markSectionOutdated(sectionX, sectionY + 1, sectionZ);
        if (sectionZ < sectionsZ - 1 && relZ == LevelSection.SIZE - 1)
            markSectionOutdated(sectionX, sectionY, sectionZ + 1);
    }

    @Override
    public void close() {
        for (SectionRenderData section : sections) {
            section.close();
        }
    }
}
