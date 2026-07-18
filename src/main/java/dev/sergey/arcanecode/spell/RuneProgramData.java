package dev.sergey.arcanecode.spell;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.Arrays;
import java.util.List;

public final class RuneProgramData {
    private static final String PROGRAM = "ArcaneProgram";
    private static final String MEDIA = "ArcaneMedia";

    private RuneProgramData() {}

    public static List<String> getProgram(ItemStack stack) {
        String encoded = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
            .copyTag().getString(PROGRAM);
        if (encoded.isBlank()) return List.of();
        return Arrays.stream(encoded.split(","))
            .filter(id -> !id.isBlank())
            .limit(512)
            .toList();
    }

    public static void setProgram(ItemStack stack, List<String> program) {
        String encoded = String.join(",", program.stream().limit(512).toList());
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(PROGRAM, encoded));
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, !program.isEmpty());
    }

    public static int getMedia(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return Math.max(0, tag.getInt(MEDIA));
    }

    public static void setMedia(ItemStack stack, int media) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(MEDIA, Math.max(0, media)));
    }
}
