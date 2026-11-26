package com.olivier.qcorigins.pickpocket;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.item.Items;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;

public class PickpocketScreenHandler extends ScreenHandler {

    private static final int TARGET_SLOTS = 36;
    private static final int HOTBAR_SIZE = 9;

    private final Inventory targetInv;
    private final ServerPlayerEntity thief;
    private final boolean[] revealed = new boolean[TARGET_SLOTS];
    private final int hotbarRevealTimer;
    private final int inventoryRevealTimer;

    private int pendingSlot = -1;
    private long pendingStart;
    private long timer;

    // Technique pour indiquer les cases non recherchées
    private ItemStack hiddenBarrier() {
        return makePane(Items.BARRIER, "Hidden");
    }

    private ItemStack searchingBarrier() {
        long elapsed = thief.getServerWorld().getTime() - pendingStart;
        long remaining = Math.max(0, (this.timer - elapsed + 19) / 20); // arrondi au-dessus
        return makePane(Items.YELLOW_STAINED_GLASS_PANE, "Searching... " + remaining + " s");
    }

    private static ItemStack makePane(net.minecraft.item.Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.setCustomName(Text.literal(name).setStyle(Style.EMPTY.withColor(Formatting.WHITE).withItalic(false)));
        return stack;
    }

    public PickpocketScreenHandler(int syncId, PlayerInventory playerInv,
                                   Inventory targetInv, ServerPlayerEntity thief,
                                   int hotbarRevealTimer, int inventoryRevealTimer) {
        super(ScreenHandlerType.GENERIC_9X4, syncId);

        this.hotbarRevealTimer = hotbarRevealTimer;
        this.inventoryRevealTimer = inventoryRevealTimer;

        this.targetInv = targetInv;
        this.thief = thief;

        // Slots de la cible : indices 0 à 35
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new HiddenSlot(targetInv, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }
        // Inventaire du voleur : indices 36 à 62, puis barre d'action 63 à 71
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 103 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 161));
        }
    }

    /** Slot qui paraît vide et inutilisable tant qu'il n'est pas révélé. */
    private class HiddenSlot extends Slot {
        private final int idx;

        HiddenSlot(Inventory inv, int index, int x, int y) {
            super(inv, index, x, y);
            this.idx = index;
        }

        @Override
        public ItemStack getStack() {
            if (revealed[idx]) return super.getStack();
            if (idx == pendingSlot) return searchingBarrier();
            return hiddenBarrier();
        }

        @Override
        public boolean canTakeItems(PlayerEntity player) {
            return revealed[idx];
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return revealed[idx] && super.getStack().isEmpty();
        }
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (slotIndex >= 0 && slotIndex < TARGET_SLOTS && !revealed[slotIndex]) {

            // Si on cherche un autre slot que celui qui est en recherche
            // Reset le cooldown
            if (pendingSlot != slotIndex) {
                pendingSlot = slotIndex;
                pendingStart = thief.getServerWorld().getTime();
                // recherche dans la hotbar
                if (slotIndex >= TARGET_SLOTS - HOTBAR_SIZE) {
                    timer = this.hotbarRevealTimer;
                    return;
                }
                timer = this.inventoryRevealTimer;
            }
            return; // aucune interaction tant que la case est cachée
        }
        super.onSlotClick(slotIndex, button, actionType, player);
    }

    /** Appelée à chaque tick serveur pour le joueur qui a ce menu ouvert. */
    @Override
    public void sendContentUpdates() {
        if (pendingSlot != -1) {
            long elapsed = thief.getServerWorld().getTime() - pendingStart;
            if (elapsed >= this.timer) {
                revealed[pendingSlot] = true;
                pendingSlot = -1;
            }
        }
        super.sendContentUpdates();
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        // Shift-clic uniquement depuis une case révélée de la cible vers le voleur
        if (slot < 0 || slot >= TARGET_SLOTS || !revealed[slot]) return ItemStack.EMPTY;

        Slot s = this.slots.get(slot);
        if (!s.hasStack()) return ItemStack.EMPTY;

        ItemStack stack = s.getStack();
        ItemStack copy = stack.copy();
        if (!this.insertItem(stack, TARGET_SLOTS, this.slots.size(), true)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) s.setStack(ItemStack.EMPTY);
        else s.markDirty();
        return copy;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return targetInv.canPlayerUse(player);
    }
}