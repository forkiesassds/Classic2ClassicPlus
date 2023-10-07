package me.icanttellyou.classic2classicplus;

import dev.dewy.nbt.tags.array.ByteArrayTag;
import dev.dewy.nbt.tags.collection.CompoundTag;
import dev.dewy.nbt.tags.collection.ListTag;
import dev.dewy.nbt.tags.primitive.FloatTag;
import dev.dewy.nbt.tags.primitive.LongTag;
import dev.dewy.nbt.tags.primitive.ShortTag;
import dev.dewy.nbt.tags.primitive.StringTag;

import java.io.*;
import java.lang.reflect.Field;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class Main {
    public static final int LEVEL_SAVE_MAGIC = 0x271BB788;

    public static void main(String... args) throws NoSuchFieldException, IllegalAccessException {
        String path = String.join(" ", args);
        File file = new File(path);
        Level level;

        //read the level
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(new FileInputStream(file)))) {
            if (in.readInt() == LEVEL_SAVE_MAGIC) {
                byte saveVer = in.readByte();
                if (saveVer > 2) {
                    System.out.println("Save version " + saveVer + " is unsupported!");
                    return;
                } else if (saveVer <= 1) {
                    System.out.println("Converting save version 1");
                    String name = in.readUTF();
                    String creator = in.readUTF();
                    long timestamp = in.readLong();
                    short width = in.readShort();
                    short height = in.readShort();
                    short depth = in.readShort();
                    byte[] blocks = new byte[width * height * depth];
                    in.readFully(blocks);
                    level = new Level(blocks, width, height, depth);
                    level.name = name;
                    level.creator = creator;
                    level.createTime = timestamp;
                } else {
                    System.out.println("Attempting to deserialize level");
                    try (ObjectInputStream ois = new ObjectInputStreamHack(in, Level.class)) {
                        level = (Level) (ois.readObject());
                    }
                }
            } else {
                System.out.println("File is not level!");
                return;
            }
        } catch (Exception e) {
            System.out.println("Failed to load level!");
            e.printStackTrace();
            return;
        }

        //create root tag for level
        CompoundTag levelRoot = new CompoundTag("MinecraftLevel");

        //create level information tag
        CompoundTag aboutTag = new CompoundTag("About");
        aboutTag.put(new LongTag("CreatedOn", level.createTime));
        aboutTag.put(new StringTag("Name", level.name));
        aboutTag.put(new StringTag("Author", level.creator));

        //put to root tag
        levelRoot.put(aboutTag);

        //create level environment tag
        CompoundTag envTag = new CompoundTag("Environment");
        envTag.put(new ShortTag("SkyColor", level.skyColor));
        envTag.put(new ShortTag("FogColor", level.fogColor));
        envTag.put(new ShortTag("CloudColor", level.cloudColor));
        envTag.put(new ShortTag("SurroundingWaterHeight", level.waterLevel));

        //put to root tag
        levelRoot.put(envTag);

        //create level data tag
        CompoundTag mapTag = new CompoundTag("Map");

        mapTag.put(new ShortTag("Width", level.width));
        mapTag.put(new ShortTag("Length", level.depth));
        mapTag.put(new ShortTag("Height", level.height));

        //create level spawn coordinate list and populate it
        ListTag<ShortTag> spawnCoordinateList = new ListTag<>("Spawn");
        spawnCoordinateList.add(new ShortTag("X", level.xSpawn));
        spawnCoordinateList.add(new ShortTag("Y", level.ySpawn));
        spawnCoordinateList.add(new ShortTag("Z", level.zSpawn));

        //add level spawn coordinate list to level data tag
        mapTag.put(spawnCoordinateList);
        mapTag.put(new ByteArrayTag("Blocks", level.blocks));
        //Data tag does not exist in classic(+).

        //put to root tag
        levelRoot.put(mapTag);

        //entities, abandon all hope.
        ListTag<CompoundTag> entityList = new ListTag<>("Entities");

        if (level.blockMap != null) {
            Class<?> levelBlockMapClass = level.blockMap.getClass();
            List<Object>[] entityGrid = (List<Object>[]) levelBlockMapClass.getField("entityGrid").get(level.blockMap);

            int blockMapWidth = levelBlockMapClass.getField("width").getInt(level.blockMap);
            int blockMapHeight = levelBlockMapClass.getField("height").getInt(level.blockMap);
            int blockMapDepth = levelBlockMapClass.getField("depth").getInt(level.blockMap);

            for (int x = 0; x < blockMapWidth; x++) {
                for (int y = 0; y < blockMapHeight; y++) {
                    for (int z = 0; z < blockMapDepth; z++) {
                        List<Object> list = entityGrid[(y * blockMapDepth + z) * blockMapWidth + x];

                        for (Object o : list) {
                            String name = o.getClass().getSimpleName();
                            Entity entity = Entity.fromWrongObject(o);

                            CompoundTag entityTag = new CompoundTag();

                            entityTag.put(new StringTag("id", name));

                            ListTag<FloatTag> posTag = new ListTag<>("Pos");
                            posTag.add(new FloatTag("X", entity.x));
                            posTag.add(new FloatTag("Y", entity.y));
                            posTag.add(new FloatTag("Z", entity.z));

                            entityTag.put(posTag);

                            ListTag<FloatTag> rotationTag = new ListTag<>("Rotation");
                            rotationTag.add(new FloatTag("Yaw", entity.yaw));
                            rotationTag.add(new FloatTag("Pitch", entity.pitch));

                            entityTag.put(rotationTag);

                            ListTag<FloatTag> motionTag = new ListTag<>("Motion");
                            motionTag.add(new FloatTag("X", entity.mx));
                            motionTag.add(new FloatTag("Y", entity.my));
                            motionTag.add(new FloatTag("Z", entity.mz));

                            entityTag.put(motionTag);

                            entityTag.put(new ShortTag("Health", entity.health));
                            entityTag.put(new ShortTag("AttackTime", entity.attackTime));
                            entityTag.put(new ShortTag("HurtTime", entity.hurtTime));
                            entityTag.put(new ShortTag("DeathTime", entity.deathTime));
                            entityTag.put(new ShortTag("Air", entity.airSupply));

                            entityList.add(entityTag);
                        }
                    }
                }
            }
        }

        //TODO: PLAYER
        /*if (level.player != null) {
            CompoundTag playerTag = new CompoundTag();


        }*/

        levelRoot.put(entityList);
    }

    static class ObjectInputStreamHack extends ObjectInputStream {
        String className;

        public ObjectInputStreamHack(InputStream in, Class<?> clazz) throws  IOException {
            super(in);
            this.className = clazz.getName();
        }

        @Override
        protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
            ObjectStreamClass streamClass = super.readClassDescriptor();

            Class<?> localClass;
            try {
                localClass = Class.forName(className);
            } catch (ClassNotFoundException e) {
                System.out.println("No local class for " + streamClass.getName());
                e.printStackTrace();
                return streamClass;
            }

            ObjectStreamClass localStreamClass = ObjectStreamClass.lookup(localClass);
            try {
                long localSUID = localStreamClass.getSerialVersionUID();
                long streamSUID = streamClass.getSerialVersionUID();
                if (streamSUID != localSUID) {
                    streamClass = localStreamClass;
                }

                Field field = streamClass.getClass().getDeclaredField("name");
                field.setAccessible(true);
                field.set(streamClass, className);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            return streamClass;
        }
    }
}