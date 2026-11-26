package com.olivier.qcorigins.power;

import io.github.apace100.apoli.power.factory.PowerFactory;
import io.github.apace100.apoli.registry.ApoliRegistries;
import net.minecraft.registry.Registry;

public class ModPowers {
    public static void register() {
        PowerFactory<?> factory = PickpocketPower.createFactory();
        Registry.register(ApoliRegistries.POWER_FACTORY, factory.getSerializerId(), factory);
    }
}