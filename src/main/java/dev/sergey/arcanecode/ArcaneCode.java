package dev.sergey.arcanecode;

import com.mojang.logging.LogUtils;
import dev.sergey.arcanecode.guide.StarterGuideEvents;
import dev.sergey.arcanecode.network.ArcaneNetwork;
import dev.sergey.arcanecode.registry.ModBlocks;
import dev.sergey.arcanecode.registry.ModCreativeTabs;
import dev.sergey.arcanecode.registry.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(ArcaneCode.MOD_ID)
public final class ArcaneCode {
    public static final String MOD_ID = "arcanecode";
    public static final String VERSION = "1.0.0-dev";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ArcaneCode(IEventBus modBus) {
        LOGGER.info("[Arcane Code] Initializing {}", VERSION);
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        modBus.addListener(ArcaneNetwork::register);
        NeoForge.EVENT_BUS.addListener(StarterGuideEvents::onPlayerLogin);
    }
}
