package com.olivier.qcorigins.screens;

import com.olivier.qcorigins.QuoicouOrigins;
import com.olivier.qcorigins.pickpocket.PickpocketScreenHandler;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;

public class ModScreens {
    public static final ScreenHandlerType<PickpocketScreenHandler> PICKPOCKET =
        Registry.register(Registries.SCREEN_HANDLER, QuoicouOrigins.id("pickpocket"),
            new ScreenHandlerType<>(PickpocketScreenHandler::new, FeatureFlags.VANILLA_FEATURES));

    public static void initialize() { }  // force le chargement de la classe
}
