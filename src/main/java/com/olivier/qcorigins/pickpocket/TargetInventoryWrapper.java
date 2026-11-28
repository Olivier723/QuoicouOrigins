package com.olivier.qcorigins.pickpocket;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

public class TargetInventoryWrapper implements Inventory {

    private static final int OFFHAND_SLOT = 36;
    private static final int INVENTORY_SIZE = 37;

    private final ServerPlayerEntity target;

    public TargetInventoryWrapper(ServerPlayerEntity target) {
        this.target = target;
    }

    // Les slots 0-8 sont la barre d'action et 9-35 l'inventaire principal.
    // Ce décalage affiche la barre d'action en bas de la grille, comme en jeu.
    private int map(int slot) {
        if (slot == OFFHAND_SLOT) return 40; // Slot 40 => Main secondaire
        return (slot + 9) % 36;
    }

    @Override
    public int size() {
        return INVENTORY_SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < this.size(); i++) {
            if (!target.getInventory().getStack(map(i)).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        return target.getInventory().getStack(map(slot));
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        return target.getInventory().removeStack(map(slot), amount);
    }

    @Override
    public ItemStack removeStack(int slot) {
        return target.getInventory().removeStack(map(slot));
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        target.getInventory().setStack(map(slot), stack);
    }

    @Override
    public void markDirty() {
        target.getInventory().markDirty();
        target.currentScreenHandler.sendContentUpdates();
        target.playerScreenHandler.sendContentUpdates();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return target.isAlive()
                && !target.isRemoved()
                && player.squaredDistanceTo(target) < 16.0;
    }

    @Override
    public void clear() {
        // Volontairement vide : on ne veut pas qu'un clic vide l'inventaire de la cible.
    }
}