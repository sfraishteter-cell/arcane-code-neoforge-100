package dev.sergey.arcanecode.item;

import dev.sergey.arcanecode.ArcaneCode;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;

final class ClientBridge {
    private ClientBridge() {}

    static void openCodex() {
        invoke("openCodex", new Class<?>[0], new Object[0]);
    }

    static void openCaster(ItemStack stack) {
        invoke("openCaster", new Class<?>[]{ItemStack.class}, new Object[]{stack.copy()});
    }

    private static void invoke(String name, Class<?>[] parameterTypes, Object[] args) {
        try {
            Class<?> hooks = Class.forName("dev.sergey.arcanecode.client.ClientHooks");
            Method method = hooks.getMethod(name, parameterTypes);
            method.invoke(null, args);
        } catch (ReflectiveOperationException exception) {
            ArcaneCode.LOGGER.error("Failed to invoke client hook {}", name, exception);
        }
    }
}
