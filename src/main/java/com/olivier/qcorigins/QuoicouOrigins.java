package com.olivier.qcorigins;

import com.olivier.qcorigins.pickpocket.PickpocketAction;
import com.olivier.qcorigins.screens.ModScreens;
import io.github.apace100.apoli.power.factory.action.ActionFactory;
import io.github.apace100.apoli.registry.ApoliRegistries;
import net.fabricmc.api.ModInitializer;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuoicouOrigins implements ModInitializer {
	public static final String MOD_ID = "quoicouorigins";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("QuoicouOrigins initializing!");

		ActionFactory<Pair<Entity, Entity>> factory = PickpocketAction.createFactory();
		Registry.register(ApoliRegistries.BIENTITY_ACTION, factory.getSerializerId(), factory);
		ModScreens.initialize();

	}

	public static Identifier id(String path) {
		return new Identifier(MOD_ID, path);
	}
}
