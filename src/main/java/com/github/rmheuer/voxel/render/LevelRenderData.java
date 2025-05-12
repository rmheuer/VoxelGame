package com.github.rmheuer.voxel.render;

import com.github.rmheuer.azalea.utils.SafeCloseable;
import com.github.rmheuer.voxel.level.MapSection;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class LevelRenderData implements SafeCloseable {
    private final int sectionsX, sectionsY, sectionsZ;
    private final SectionRenderData[] sections;
    private final Vector3f prevCameraPos;

    public LevelRenderData(int sectionsX, int sectionsY, int sectionsZ) {
        this.sectionsX = sectionsX;
        this.sectionsY = sectionsY;
        this.sectionsZ = sectionsZ;

        prevCameraPos = new Vector3f();

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
        int sectionX = blockX / MapSection.SIZE;
        int sectionY = blockY / MapSection.SIZE;
        int sectionZ = blockZ / MapSection.SIZE;
        int relX = blockX % MapSection.SIZE;
        int relY = blockY % MapSection.SIZE;
        int relZ = blockZ % MapSection.SIZE;

        SectionRenderData section = getSection(sectionX, sectionY, sectionZ);
        section.markOutdated();
        section.markVisibilityOutdated();

        boolean faceNX = sectionX > 0 && relX == 0;
        boolean faceNY = sectionY > 0 && relY == 0;
        boolean faceNZ = sectionZ > 0 && relZ == 0;
        boolean facePX = sectionX < sectionsX - 1 && relX == MapSection.SIZE - 1;
        boolean facePY = sectionY < sectionsY - 1 && relY == MapSection.SIZE - 1;
        boolean facePZ = sectionZ < sectionsZ - 1 && relZ == MapSection.SIZE - 1;

        // If touching face, mark adjacent section also
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

        // If touching a bottom edge, mark diagonal section also
        if (faceNX && faceNY)
            markSectionOutdated(sectionX - 1, sectionY - 1, sectionZ);
        if (facePX && faceNY)
            markSectionOutdated(sectionX + 1, sectionY - 1, sectionZ);
        if (faceNY && faceNZ)
            markSectionOutdated(sectionX, sectionY - 1, sectionZ - 1);
        if (faceNY && facePZ)
            markSectionOutdated(sectionX, sectionY - 1, sectionZ + 1);

        // If touching a bottom corner, mark diagonal section also
        if (faceNX && faceNY && faceNZ)
            markSectionOutdated(sectionX - 1, sectionY - 1, sectionZ - 1);
        if (facePX && faceNY && faceNZ)
            markSectionOutdated(sectionX + 1, sectionY - 1, sectionZ - 1);
        if (faceNX && faceNY && facePZ)
            markSectionOutdated(sectionX - 1, sectionY - 1, sectionZ + 1);
        if (facePX && faceNY && facePZ)
            markSectionOutdated(sectionX + 1, sectionY - 1, sectionZ + 1);
    }

    public void lightChanged(int blockX, int blockZ, int prevHeight, int newHeight) {
        if (prevHeight == newHeight)
            throw new IllegalArgumentException("Height did not change");

        int minHeight = Math.min(prevHeight, newHeight);
        int maxHeight = Math.max(prevHeight, newHeight);
        int minSection = minHeight / MapSection.SIZE;
        int maxSection = (maxHeight - 1) / MapSection.SIZE;

        int sectionX = blockX / MapSection.SIZE;
        int sectionZ = blockZ / MapSection.SIZE;
        int relX = blockX % MapSection.SIZE;
        int relZ = blockZ % MapSection.SIZE;
        boolean edgeNX = sectionX > 0 && relX == 0;
        boolean edgeNZ = sectionZ > 0 && relZ == 0;
        boolean edgePX = sectionX < sectionsX - 1 && relX == MapSection.SIZE - 1;
        boolean edgePZ = sectionZ < sectionsZ - 1 && relZ == MapSection.SIZE - 1;

        for (int sectionY = minSection; sectionY <= maxSection; sectionY++) {
            markSectionOutdated(sectionX, sectionY, sectionZ);
            if (edgeNX)
                markSectionOutdated(sectionX - 1, sectionY, sectionZ);
            if (edgeNZ)
                markSectionOutdated(sectionX, sectionY, sectionZ - 1);
            if (edgePX)
                markSectionOutdated(sectionX + 1, sectionY, sectionZ);
            if (edgePZ)
                markSectionOutdated(sectionX, sectionY, sectionZ + 1);
        }

        if (minSection > 0 && minHeight % MapSection.SIZE == 0)
            markSectionOutdated(sectionX, minSection - 1, sectionZ);
        if (maxSection < sectionsY - 1 && maxHeight % MapSection.SIZE == MapSection.SIZE - 1)
            markSectionOutdated(sectionX, maxSection + 1, sectionZ);
    }

    public Vector3f getPrevCameraPos() {
        return prevCameraPos;
    }

    public void setPrevCameraPos(Vector3fc pos) {
        prevCameraPos.set(pos);
    }

    @Override
    public void close() {
        for (SectionRenderData section : sections) {
            section.close();
        }
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
}
