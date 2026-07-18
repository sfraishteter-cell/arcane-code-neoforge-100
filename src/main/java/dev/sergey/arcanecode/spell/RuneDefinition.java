package dev.sergey.arcanecode.spell;

import net.minecraft.network.chat.Component;

public record RuneDefinition(
    String id,
    String category,
    int mediaCost,
    String signature,
    Component name,
    Component description,
    int[] directions,
    String argument
) {}
