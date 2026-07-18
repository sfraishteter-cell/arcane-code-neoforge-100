package dev.sergey.arcanecode.spell;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** A strongly typed value carried by the Arcane Code stack machine. */
public record Iota(Kind kind, Object value) {
    public enum Kind {
        NUMBER, BOOLEAN, STRING, VECTOR, ENTITY,
        EFFECT, PARTICLE, BLOCK, ITEM, SOUND, ENTITY_TYPE,
        LIST, PROGRAM, NULL
    }

    public static Iota number(double value) { return new Iota(Kind.NUMBER, value); }
    public static Iota bool(boolean value) { return new Iota(Kind.BOOLEAN, value); }
    public static Iota string(String value) { return new Iota(Kind.STRING, value); }
    public static Iota vector(Vec3 value) { return new Iota(Kind.VECTOR, value); }
    public static Iota entity(Entity value) { return new Iota(Kind.ENTITY, value); }
    public static Iota effect(ResourceLocation value) { return new Iota(Kind.EFFECT, value); }
    public static Iota particle(ResourceLocation value) { return new Iota(Kind.PARTICLE, value); }
    public static Iota block(ResourceLocation value) { return new Iota(Kind.BLOCK, value); }
    public static Iota item(ResourceLocation value) { return new Iota(Kind.ITEM, value); }
    public static Iota sound(ResourceLocation value) { return new Iota(Kind.SOUND, value); }
    public static Iota entityType(ResourceLocation value) { return new Iota(Kind.ENTITY_TYPE, value); }
    public static Iota list(List<Iota> value) { return new Iota(Kind.LIST, List.copyOf(value)); }
    public static Iota program(List<String> value) { return new Iota(Kind.PROGRAM, List.copyOf(value)); }
    public static Iota nil() { return new Iota(Kind.NULL, null); }

    public String shortDisplay() {
        return switch (kind) {
            case NUMBER -> String.format("%.3f", (double)value);
            case BOOLEAN, STRING, EFFECT, PARTICLE, BLOCK, ITEM, SOUND, ENTITY_TYPE -> String.valueOf(value);
            case VECTOR -> {
                Vec3 vec = (Vec3)value;
                yield String.format("(%.2f, %.2f, %.2f)", vec.x, vec.y, vec.z);
            }
            case ENTITY -> ((Entity)value).getName().getString();
            case LIST -> "list[" + ((List<?>)value).size() + "]";
            case PROGRAM -> "program[" + ((List<?>)value).size() + "]";
            case NULL -> "null";
        };
    }
}
