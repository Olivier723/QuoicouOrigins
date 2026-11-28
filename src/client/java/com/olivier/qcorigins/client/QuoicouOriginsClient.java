package com.olivier.qcorigins.client;

import com.olivier.qcorigins.screens.ModScreens;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class QuoicouOriginsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		HandledScreens.register(ModScreens.PICKPOCKET, PickpocketScreen::new);
	}
}