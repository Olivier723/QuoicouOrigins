package com.olivier.qcorigins.pickpocket;

import com.olivier.qcorigins.QuoicouOrigins;
import com.olivier.qcorigins.screens.ModScreens;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/**
 *  Le rendu grisé des cases de l'inventaire se fait côté client, le serveur doit syncroniser les inventaires
 */
public class PickpocketScreenHandler extends ScreenHandler {

    public static final int TARGET_SLOTS = 37;
    public static final int OFFHAND_INDEX = 36;
    private static final int MAIN_SLOTS = 36;
    private static final int HOTBAR_SIZE = 9;
    private static final int PROPERTIES_COUNT = 6;
    private static final int OFFHAND_BIT = 9; // On utilise la première propriété (num 0) pour stocker la main secondaire sur le 9e bit (0-8 → une barre de l'inventaire)


    private final Inventory targetInv;
    private final ServerPlayerEntity thief;
    private final ServerPlayerEntity target;
    private final Identifier stealSound;
    private final float stealSoundVolume;
    private final float stealSoundPitch;
    private final int hotbarRevealTimer;
    private final int inventoryRevealTimer;
    /**
     * Selon claude :
     * Une propriété = 16 bits (short) => une propriété par rangée de l'inventaire + une pour la case en cours de fouille + une pour le progrès de la fouille <br>
     *
     * Index	Contenu
     * 0 à 3	masque de bits des cases révélées, une rangée par propriété
     * 4	    case en cours de fouille (-1 si aucune)
     * 5	    progression de la fouille, de 0 à 100
     */
    private final PropertyDelegate props; // Propriété utilisée par le serveur pour syncroniser l'état de l'inventaire avec le côté client

    private long pendingStart;
    private long pendingDuration;

    // Constructeur client (appelé par le type enregistré)
    public PickpocketScreenHandler(int syncId, PlayerInventory playerInv) {
        this(syncId, playerInv, new SimpleInventory(TARGET_SLOTS),
            new ArrayPropertyDelegate(PROPERTIES_COUNT), null, null, 0, 0, null, 0f, 0f);
    }

    // Constructeur serveur
    public PickpocketScreenHandler(int syncId, PlayerInventory playerInv, Inventory targetInv,
                                   ServerPlayerEntity thief, ServerPlayerEntity target, int hotbarRevealTimer, int inventoryRevealTimer,
                                   Identifier stealSound, float stealSoundVolume, float stealSoundPitch) {
        this(syncId, playerInv, targetInv, new ArrayPropertyDelegate(PROPERTIES_COUNT), thief, target, hotbarRevealTimer, inventoryRevealTimer, stealSound, stealSoundVolume, stealSoundPitch);
    }


    private PickpocketScreenHandler(int syncId, PlayerInventory playerInv,
                                    Inventory targetInv, PropertyDelegate props,
                                    ServerPlayerEntity thief, ServerPlayerEntity target,
                                    int hotbarRevealTimer, int inventoryRevealTimer,
                                    Identifier stealSound, float stealSoundVolume, float stealSoundPitch) {
        super(ModScreens.PICKPOCKET, syncId);

        this.hotbarRevealTimer = hotbarRevealTimer;
        this.inventoryRevealTimer = inventoryRevealTimer;
        this.targetInv = targetInv;
        this.thief = thief;
        this.target = target;
        this.stealSound = stealSound;
        this.stealSoundVolume = stealSoundVolume;
        this.stealSoundPitch = stealSoundPitch;
        this.props = props;
        this.props.set(4, -1);

        // Grille de la victime : la rangée 3 (hotbar) est décalée de 4 px
        for (int row = 0; row < 4; row++) {
            int y = 18 + row * 18 + (row == 3 ? 4 : 0);
            for (int col = 0; col < 9; col++) {
                addSlot(new HiddenSlot(targetInv, col + row * 9, 8 + col * 18, y));
            }
        }

        // Main secondaire : alignée sur la hotbar de la victime
        addSlot(new HiddenSlot(targetInv, OFFHAND_INDEX, 174, 76));

        // Inventaire du voleur (décalé de 4 px)
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 108 + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(playerInv, col, 8 + col * 18, 166));

        addProperties(this.props);
    }

    // ---- État partagé (lisible des deux côtés) ----
    public boolean isRevealed(int idx) {
        if (idx == OFFHAND_INDEX) return ((props.get(0) >> OFFHAND_BIT) & 1) == 1;
        return ((props.get(idx / 9) >> (idx % 9)) & 1) == 1;
    }

    private void setRevealed(int idx) {
        if (idx == OFFHAND_INDEX) {
            props.set(0, props.get(0) | (1 << OFFHAND_BIT));
        } else {
            props.set(idx / 9, props.get(idx / 9) | (1 << (idx % 9)));
        }
    }

    public int getPendingSlot() { return props.get(4); }
    public int getProgress() { return props.get(5); }

    /** Slot qui paraît vide et inutilisable tant qu'il n'est pas révélé. */
    private class HiddenSlot extends Slot {
        private final int idx;

        HiddenSlot(Inventory inv, int index, int x, int y) {
            super(inv, index, x, y);
            this.idx = index;
        }

        @Override
        public ItemStack getStack() {
            if (isRevealed(idx)) return super.getStack();
            return ItemStack.EMPTY;
        }

        @Override
        public boolean canTakeItems(PlayerEntity player) {
            return isRevealed(idx);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return isRevealed(idx) && super.getStack().isEmpty();
        }

        @Override
        public void onTakeItem(PlayerEntity player, ItemStack stack) {
            onItemStolen(player, stack);
            super.onTakeItem(player, stack);
        }

        @Override
        public void setStack(ItemStack stack) {
            super.setStack(stack);
        }
    }

    private void onItemStolen(PlayerEntity player, ItemStack stack) {
        if (player == null || target == null) return;
        if (stack.isEmpty()) return;

        QuoicouOrigins.LOGGER.info("[Pickpocket] {} stole {}x {} from {} {}",
            player.getName().getString(), stack.getCount(),
            Registries.ITEM.getId(stack.getItem()), target.getName().getString(),
            stack.hasNbt() ? "with NBT=" + stack.getNbt() : ""
        );

        playToBoth();
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (slotIndex >= 0 && slotIndex < TARGET_SLOTS && !isRevealed(slotIndex)) {

            // Si on cherche un autre slot que celui qui est en recherche
            // Reset le cooldown
            // Si thief est null alors on est côté client
            if (thief == null || getPendingSlot() == slotIndex) {
                return;
            }
            props.set(4, slotIndex);
            props.set(5, 0);
            pendingStart = thief.getServerWorld().getTime();
            // recherche dans la hotbar
            if (slotIndex >= MAIN_SLOTS - HOTBAR_SIZE && slotIndex < MAIN_SLOTS || slotIndex == OFFHAND_INDEX) {
                pendingDuration = this.hotbarRevealTimer;
                return;
            }
            pendingDuration = this.inventoryRevealTimer;
            return; // aucune interaction tant que la case est cachée
        }
        super.onSlotClick(slotIndex, button, actionType, player);
    }




    /** Appelée à chaque tick serveur pour le joueur qui a ce menu ouvert. */
    @Override
    public void sendContentUpdates() {
        if (thief != null && getPendingSlot() != -1) {
            long elapsed = thief.getServerWorld().getTime() - pendingStart;
            if (elapsed >= pendingDuration) {
                setRevealed(getPendingSlot());
                props.set(4, -1); // Reset de la case en recherche
                props.set(5, 0); // Remise du progrès à 0
            }else {
                props.set(5, (int) (elapsed * 100 / Math.max(1, pendingDuration)));
            }
        }
        super.sendContentUpdates();
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        // Shift-clic uniquement depuis une case révélée de la cible vers le voleur
        if (slot < 0 || slot >= TARGET_SLOTS || !isRevealed(slot)) return ItemStack.EMPTY;

        Slot s = slots.get(slot);
        if (!s.hasStack()) return ItemStack.EMPTY;

        ItemStack stack = s.getStack();
        ItemStack copy = stack.copy();
        if (!this.insertItem(stack, TARGET_SLOTS, slots.size(), true)) {
            return ItemStack.EMPTY;
        }

        // Quantité réellement transférée = avant - après
        ItemStack moved = copy.copy();
        moved.setCount(copy.getCount() - stack.getCount());
        onItemStolen(player, moved);

        if (stack.isEmpty()) s.setStack(ItemStack.EMPTY);
        else s.markDirty();
        return copy;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return targetInv.canPlayerUse(player);
    }

    private void playToBoth() {
        if (this.stealSound == null) return; // Crash si le son est null (côté client)
        RegistryEntry<SoundEvent> entry = RegistryEntry.of(SoundEvent.of(stealSound));
        for (ServerPlayerEntity p : new ServerPlayerEntity[]{thief, target}) {
            p.networkHandler.sendPacket(new PlaySoundS2CPacket(
                entry, SoundCategory.PLAYERS,
                p.getX(), p.getY(), p.getZ(),
                stealSoundVolume, stealSoundPitch, p.getWorld().getRandom().nextLong()));
        }
    }
}