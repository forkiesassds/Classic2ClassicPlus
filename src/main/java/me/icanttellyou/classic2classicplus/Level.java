package me.icanttellyou.classic2classicplus;

import java.io.Serial;
import java.io.Serializable;

public class Level implements Serializable {
    @Serial
    private static final long serialVersionUID = 0L;
    public int width, height, depth;
    public byte[] blocks;
    public String name, creator;
    public long createTime;
    public int xSpawn, ySpawn, zSpawn;
    public float rotSpawn;
    public Object blockMap;
    public boolean creativeMode;
    public int waterLevel, skyColor, fogColor, cloudColor;
    public Object player;
    public boolean networkMode;
    public int unprocessed, tickCount;

    public Level(byte[] blocks, short width, short height, short depth) {
        this.blocks = blocks;
        this.width = width;
        this.height = height;
        this.depth = depth;
    }

    public Level() {
        this.blocks = new byte[1 << 24];
        this.width = 256;
        this.height = 256;
        this.depth = 256;
    }
}
