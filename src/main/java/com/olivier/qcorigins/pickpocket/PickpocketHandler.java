package com.olivier.qcorigins.pickpocket;

import com.olivier.qcorigins.power.PickpocketPower;
import com.olivier.qcorigins.QuoicouOrigins;
import io.github.apace100.apoli.component.PowerHolderComponent;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PickpocketHandler {

    public static void register() {
        UseEntityCallback.EVENT.register(PickpocketHandler::onUse);
    }

    private static ActionResult onUse(PlayerEntity player, World world, Hand hand, Entity entity, @Nullable EntityHitResult hit) {

        if (world.isClient() || hand != Hand.MAIN_HAND) return ActionResult.PASS;
        if (!(player instanceof ServerPlayerEntity thief)
                || !(entity instanceof ServerPlayerEntity target)) return ActionResult.PASS;
        if (!thief.isSneaking() || thief.isSpectator()) return ActionResult.PASS;

        List<PickpocketPower> powers = PowerHolderComponent.getPowers(thief, PickpocketPower.class);
        if (powers.isEmpty()) return ActionResult.PASS;
        PickpocketPower power = powers.get(0);

        long now = world.getTime();


        if (thief.distanceTo(target) > power.getRange()) return ActionResult.PASS;

        QuoicouOrigins.LOGGER.info("[Pickpocket] Player {} tried to rob {}", thief.getName().getString(), target.getName().getString());

        if (!power.isReady(now)) {
            int seconds = power.remainingTicks(now) / 20 + 1;
            thief.sendMessage(Text.literal("Vos doigts sont encore engourdis (" + seconds + " s)"), true);
            return ActionResult.FAIL;
        }

        if (power.mustBeBehind() && !isBehind(thief, target)) {
            thief.sendMessage(Text.literal("Vous devez vous placer dans son dos"), true);
            return ActionResult.FAIL;
        }

        power.markUsed(now);

        // Risque de détection
        if (world.random.nextFloat() < power.getDetectionChance()) {
            target.sendMessage(Text.literal("Vous sentez une main dans votre poche..."), true);
            target.getServerWorld().playSound(null, target.getBlockPos(),
                    SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.PLAYERS, 0.8f, 1.0f);
        }

        thief.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> new PickpocketScreenHandler(
                        syncId, inv, new TargetInventoryWrapper(target), thief, power.hotbarRevealTimer, power.inventoryRevealTimer),
                Text.literal("Poches de " + target.getName().getString())
        ));
        return ActionResult.SUCCESS;
    }

    /** Vrai si le voleur est dans un cône d'environ 120° derrière la cible. */
    private static boolean isBehind(ServerPlayerEntity thief, ServerPlayerEntity target) {
        Vec3d targetLook = target.getRotationVec(1.0F);
        Vec3d look = new Vec3d(targetLook.x, 0, targetLook.z).normalize();
        Vec3d toThief = new Vec3d(thief.getX() - target.getX(), 0, thief.getZ() - target.getZ()).normalize();
        return look.dotProduct(toThief) < -0.5;
    }
}