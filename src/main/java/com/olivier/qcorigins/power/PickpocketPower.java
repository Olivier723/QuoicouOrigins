package com.olivier.qcorigins.power;

import com.olivier.qcorigins.QuoicouOrigins;
import io.github.apace100.apoli.power.Power;
import io.github.apace100.apoli.power.PowerType;
import io.github.apace100.apoli.power.factory.PowerFactory;
import io.github.apace100.calio.data.SerializableData;
import io.github.apace100.calio.data.SerializableDataTypes;
import net.minecraft.entity.LivingEntity;

public class PickpocketPower extends Power {

    private final int cooldown;
    private final float range;
    private final boolean mustBeBehind;
    private final float detectionChance;
    public final int hotbarRevealTimer;
    public final int inventoryRevealTimer;
    private long lastUse = -1_000_000L;

    public PickpocketPower(PowerType<?> type, LivingEntity entity, int cooldown,
                           float range, boolean mustBeBehind, float detectionChance,
                           int hotbarRevealTimer, int inventoryRevealTimer) {
        super(type, entity);
        this.cooldown = cooldown;
        this.range = range;
        this.mustBeBehind = mustBeBehind;
        this.detectionChance = detectionChance;
        this.inventoryRevealTimer = inventoryRevealTimer;
        this.hotbarRevealTimer = hotbarRevealTimer;
    }

    public boolean isReady(long worldTime) { return worldTime - lastUse >= cooldown; }
    public int remainingTicks(long worldTime) { return (int) Math.max(0, cooldown - (worldTime - lastUse)); }
    public void markUsed(long worldTime) { this.lastUse = worldTime; }

    public float getRange() { return range; }
    public boolean mustBeBehind() { return mustBeBehind; }
    public float getDetectionChance() { return detectionChance; }

    public static PowerFactory<?> createFactory() {
        return new PowerFactory<>(QuoicouOrigins.id("pickpocket"),
                new SerializableData()
                    .add("cooldown", SerializableDataTypes.INT, 200)
                    .add("range", SerializableDataTypes.FLOAT, 3.0F)
                    .add("must_be_behind", SerializableDataTypes.BOOLEAN, true)
                    .add("detection_chance", SerializableDataTypes.FLOAT, 0.25F)
                    .add("hotbar_reveal_timer", SerializableDataTypes.INT, 7 * 20)
                    .add("inventory_reveal_timer", SerializableDataTypes.INT, 2 * 20),
                data -> (
                    (type, entity) -> new PickpocketPower(
                        type, entity,
                        data.getInt("cooldown"),
                        data.getFloat("range"),
                        data.getBoolean("must_be_behind"),
                        data.getFloat("detection_chance"),
                        data.getInt("hotbar_reveal_timer"),
                        data.getInt("inventory_reveal_timer")
                    )
                )
        ).allowCondition();
    }
}