package net.pixeldreamstudios.spellhudaddon;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ModInitializer;

import net.pixeldreamstudios.spellhudaddon.config.AddonHudConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpellHudAddon implements ModInitializer {
	public static final String MOD_ID = "spellhud-addon";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		AutoConfig.register(AddonHudConfig.class, JanksonConfigSerializer::new);
		LOGGER.info("SpellHudAddon loaded with config");

		LOGGER.info("Hello Fabric world!");
	}
}