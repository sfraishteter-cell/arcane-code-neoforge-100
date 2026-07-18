package dev.sergey.arcanecode.client;

import dev.sergey.arcanecode.spell.RuneProgramData;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

public final class ClientHooks {
    private ClientHooks() {}

    public static void openCodex() {
        Minecraft.getInstance().setScreen(new ArcaneCodexScreen());
    }

    public static void openCaster(ItemStack stack) {
        Minecraft.getInstance().setScreen(new RuneCastingScreen(RuneProgramData.getProgram(stack)));
    }
}
