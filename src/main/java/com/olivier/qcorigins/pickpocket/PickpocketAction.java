package com.olivier.qcorigins.pickpocket;

import com.olivier.qcorigins.QuoicouOrigins;
import io.github.apace100.apoli.power.factory.action.ActionFactory;
import io.github.apace100.calio.data.SerializableData;
import io.github.apace100.calio.data.SerializableDataTypes;
import net.minecraft.entity.Entity;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;

public class PickpocketAction {

    public static ActionFactory<Pair<Entity, Entity>> createFactory() {
        return new ActionFactory<>(
            QuoicouOrigins.id("open_pickpocket"),
            new SerializableData()
                .add("hotbar_search_time", SerializableDataTypes.INT, 40)
                .add("inventory_search_time", SerializableDataTypes.INT, 80)
                .add("detection_chance", SerializableDataTypes.FLOAT, 0.25F)
                .add("steal_sound", SerializableDataTypes.IDENTIFIER, new Identifier("minecraft", "entity.item.pickup"))
                .add("steal_sound_volume", SerializableDataTypes.FLOAT, 1.0F)
                .add("steal_sound_pitch", SerializableDataTypes.FLOAT, 0.8F),
            PickpocketAction::execute
        );
    }

    private static void execute(SerializableData.Instance data, Pair<Entity, Entity> pair) {
        if (!(pair.getLeft() instanceof ServerPlayerEntity thief)) return;
        if (!(pair.getRight() instanceof ServerPlayerEntity target)) return;

        thief.openHandledScreen(new SimpleNamedScreenHandlerFactory(
            (syncId, inv, p) -> new PickpocketScreenHandler(syncId, inv,
                new TargetInventoryWrapper(target), thief, target,
                data.getInt("hotbar_search_time"), data.getInt("inventory_search_time"),
                data.getId("steal_sound"),
                data.getFloat("steal_sound_volume"), data.getFloat("steal_sound_pitch")),
            Text.translatable("power.quoicouorigins.pickpocket.victim_inventory", target.getName().getString())
        ));
    }
}