package net.pixeldreamstudios.spellhudaddon;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ModInitializer;

import net.pixeldreamstudios.spellhudaddon.config.AddonHudConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpellHudAddon implements ModInitializer {
	public static final String MOD_ID = "spellhud-addon";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		AutoConfig.register(AddonHudConfig.class, JanksonConfigSerializer::new);
		LOGGER.info("SpellHudAddon loaded with config");
	}
}