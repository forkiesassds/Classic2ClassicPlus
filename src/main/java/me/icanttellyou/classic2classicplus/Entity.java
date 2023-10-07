package me.icanttellyou.classic2classicplus;

import java.lang.reflect.Field;

public class Entity {
    public float x, y, z;
    public float yaw, pitch;
    public float mx, my, mz;
    public int health = 20;
    public int attackTime, hurtTime, deathTime;
    public int airSupply = 300;

    public Entity() {}

    public static Entity fromWrongObject(Object obj) throws NoSuchFieldException, IllegalAccessException {
        Class<?> clazz = obj.getClass();

        Entity entity = new Entity();

        entity.x = clazz.getField("x").getFloat(obj);
        entity.y = clazz.getField("y").getFloat(obj);
        entity.z = clazz.getField("z").getFloat(obj);

        entity.yaw = clazz.getField("yRot").getFloat(obj);
        entity.pitch = clazz.getField("xRot").getFloat(obj);

        entity.mx = clazz.getField("xd").getFloat(obj);
        entity.my = clazz.getField("yd").getFloat(obj);
        entity.mz = clazz.getField("zd").getFloat(obj);

        //forgive me
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getName().equals("health"))
                entity.health = clazz.getField("health").getInt(obj);
            if (field.getName().equals("attackTime"))
                entity.attackTime = clazz.getField("attackTime").getInt(obj);
            if (field.getName().equals("hurtTime"))
                entity.hurtTime = clazz.getField("hurtTime").getInt(obj);
            if (field.getName().equals("deathTime"))
                entity.deathTime = clazz.getField("deathTime").getInt(obj);
            if (field.getName().equals("airSupply"))
                entity.airSupply = clazz.getField("airSupply").getInt(obj);
        }

        return entity;
    }
}
