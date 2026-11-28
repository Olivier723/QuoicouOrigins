package com.olivier.qcorigins.client;

import com.olivier.qcorigins.QuoicouOrigins;
import com.olivier.qcorigins.pickpocket.PickpocketScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class PickpocketScreen extends HandledScreen<PickpocketScreenHandler> {

    private static final Identifier THIEF_TEXTURE =
        new Identifier(QuoicouOrigins.MOD_ID, "textures/gui/thief_inventory.png");

    private static final int BASE_WIDTH = 176;  // partie centrée par HandledScreen
    private static final int GUI_W = 198;       // dessin complet, panneau compris
    private static final int GUI_H = 190;
    private static final int TEX_W = 256;       // taille du fichier PNG
    private static final int TEX_H = 256;

    public PickpocketScreen(PickpocketScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);
        // backgroundWidth reste à 176 : la partie coffre reste centrée
        this.backgroundHeight = GUI_H;
        this.playerInventoryTitleY = GUI_H - 94;
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        ctx.drawTexture(THIEF_TEXTURE, this.x, this.y, 0, 0, GUI_W, GUI_H, TEX_W, TEX_H);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx);
        super.render(ctx, mouseX, mouseY, delta);
        drawHiddenOverlays(ctx);
        drawMouseoverTooltip(ctx, mouseX, mouseY);
    }

    private void drawHiddenOverlays(DrawContext ctx) {
        for (int i = 0; i < PickpocketScreenHandler.TARGET_SLOTS; i++) {
            if (handler.isRevealed(i)) continue;

            Slot slot = handler.slots.get(i);
            int sx = this.x + slot.x;
            int sy = this.y + slot.y;
            boolean searching = handler.getPendingSlot() == i;

            // Grisage (jaune si en cours de fouille)
            ctx.fill(sx, sy, sx + 16, sy + 16, searching ? 0xCC8A7A20 : 0xCC555555);

            if (searching) {
                // Barre de progression en bas de la case
                int w = 16 * handler.getProgress() / 100;
                ctx.fill(sx, sy + 14, sx + w, sy + 16, 0xFFFFFF55);
            }
        }
    }

    @Override
    protected boolean isClickOutsideBounds(double mouseX, double mouseY, int left, int top, int button) {
        boolean outside = super.isClickOutsideBounds(mouseX, mouseY, left, top, button);
        // Le panneau de la main secondaire compte comme intérieur
        boolean inPanel = mouseX >= left + BASE_WIDTH && mouseX < left + GUI_W
            && mouseY >= top && mouseY < top + GUI_H;
        return outside && !inPanel;
    }
}