package mods.cybercat.gigeresque.common;

import mods.cybercat.gigeresque.common.block.GIgBlocks;
import mods.cybercat.gigeresque.common.config.CustomMidnightConfig;
import mods.cybercat.gigeresque.common.config.GigeresqueConfig;
import mods.cybercat.gigeresque.common.data.handler.TrackedDataHandlers;
import mods.cybercat.gigeresque.common.entity.Entities;
import mods.cybercat.gigeresque.common.fluid.GigFluids;
import mods.cybercat.gigeresque.common.item.GigItems;
import mods.cybercat.gigeresque.common.item.group.GigItemGroups;
import mods.cybercat.gigeresque.common.sound.GigSounds;
import mods.cybercat.gigeresque.common.status.effect.GigStatusEffects;
import mods.cybercat.gigeresque.common.structures.GigStructures;
import net.fabricmc.api.ModInitializer;

public class Gigeresque implements ModInitializer {
	public static GigeresqueConfig config;
	public static final String MOD_ID = "gigeresque";

	@Override
	public void onInitialize() {
		CustomMidnightConfig.init(MOD_ID, GigeresqueConfig.class);

		new GigItems();
		GIgBlocks.getInstance().initialize();
		GigFluids.getInstance().initialize();
		GigSounds.getInstance().initialize();
		GigStatusEffects.getInstance().initialize();
		TrackedDataHandlers.getInstance().initialize();
		Entities.getInstance().initialize();
		GigStructures.registerStructureFeatures();
		new GigItemGroups();
	}
}
