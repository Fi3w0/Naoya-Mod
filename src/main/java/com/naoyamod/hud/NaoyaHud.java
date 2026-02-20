package com.naoyamod.hud;

import com.naoyamod.ability.AbilityLevel;
import com.naoyamod.ability.ClientAbilityState;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * HUD for Naoya's ability — 1.20.1 compatible.
 * Fabric API 1.20.1 HudRenderCallback signature: (DrawContext, float tickDelta)
 */
public class NaoyaHud {

    private static int animTick = 0;

    public static void register() {
        // 1.20.1 signature: DrawContext + float tickDelta
        HudRenderCallback.EVENT.register((context, tickDelta) -> render(context));
    }

    private static void render(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;

        ClientAbilityState state = ClientAbilityState.INSTANCE;
        if (!state.isWhitelisted()) return;

        AbilityLevel level = state.getLevel();
        animTick++;

        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();
        TextRenderer tr = mc.textRenderer;

        // ── Panel layout (bottom-left) ──────────────────────────────────────
        // Heights: title(10) + gap(2) + dots(12) + status(10) + barrier(8) + padding
        // Total panel height depends on level
        boolean hasBarrier = level.hasSoundBarrier();
        boolean isBurst    = state.isBurstActive();

        int panelX = 8;
        int panelW = 170;

        // Build rows from bottom up so they don't push into hotbar
        int bottomPad = 55; // above hotbar
        int rowH      = 11;

        // Rows (from bottom): [charge bar?] [status] [dots] [title]
        int rowCount  = 3 + (hasBarrier ? 1 : 0);
        int panelH    = rowCount * rowH + 6;
        int panelY    = sh - bottomPad - panelH;

        // Background
        ctx.fill(panelX - 2, panelY - 2, panelX + panelW, panelY + panelH, 0xAA000000);
        // Border — color by level
        int borderCol = levelColor(level, isBurst);
        drawBorder(ctx, panelX - 2, panelY - 2, panelX + panelW, panelY + panelH, borderCol);

        // ── Row 0: Title ────────────────────────────────────────────────────
        int ry = panelY + 2;
        String title = isBurst ? "§4⚡ NAOYA — BURST ⚡" : "§f⚡ NAOYA";
        ctx.drawTextWithShadow(tr, title, panelX, ry, borderCol);
        ry += rowH;

        // ── Row 1: Level dots ───────────────────────────────────────────────
        for (int i = 1; i <= 3; i++) {
            int dotX     = panelX + (i - 1) * 55;
            boolean active = level.level >= i;
            boolean exact  = level.level == i;
            int dotColor   = active ? dotColor(i) : 0xFF333333;

            ctx.fill(dotX, ry, dotX + 48, ry + 9, dotColor);

            // Pulsing glow for the currently selected dot
            if (exact) {
                float pulse     = (float)(Math.sin(animTick * 0.18) * 0.5 + 0.5);
                int   glowAlpha = (int)(pulse * 100);
                ctx.fill(dotX - 1, ry - 1, dotX + 49, ry + 10,
                        (glowAlpha << 24) | (borderCol & 0xFFFFFF));
            }

            String lbl = "LV" + i;
            ctx.drawTextWithShadow(tr, lbl, dotX + 17, ry + 1, active ? 0xFFFFFF : 0xFF555555);
        }
        ry += rowH;

        // ── Row 2: Status line (replaces barrier text — no overlap) ─────────
        String status = getStatus(level, state);
        ctx.drawTextWithShadow(tr, status, panelX, ry, 0xFFDDDDDD);
        ry += rowH;

        // ── Row 3 (optional): Charge bar for sound barrier ──────────────────
        if (hasBarrier) {
            float fill = Math.min(1f, state.getRunningTicks() / 40f);
            // Bar background
            ctx.fill(panelX, ry, panelX + 160, ry + 5, 0xFF111111);
            // Bar fill
            int fillCol = fill >= 1f ? 0xFF00FFFF : 0xFF0055AA;
            if (fill >= 1f && (animTick / 5) % 2 == 0) fillCol = 0xFF88FFFF; // flicker
            ctx.fill(panelX, ry, panelX + (int)(160 * fill), ry + 5, fillCol);

            // Label inside bar area (right side) — only when NOT broken to avoid double text
            if (fill >= 1f) {
                // Replaces status line with barrier info (already shown in status, bar is enough)
            }
        }

        // ── Screen-edge glow (level 2+) ──────────────────────────────────────
        if (level == AbilityLevel.LEVEL_3 || isBurst) {
            float p = pulse(0.20f);
            int a = (int)(p * (isBurst ? 110 : 75));
            int c = isBurst ? 0xFF4400 : 0xFF2200;
            edgeGlow(ctx, sw, sh, a, c, isBurst ? 8 : 5);
        } else if (level == AbilityLevel.LEVEL_2) {
            float p = pulse(0.14f);
            int a = (int)(p * 55);
            edgeGlow(ctx, sw, sh, a, 0x0077FF, 4);
        }

        // ── Sound barrier break flash overlay ────────────────────────────────
        int flash = state.getSoundBarrierFlash();
        if (flash > 0) {
            int fAlpha = (int)((flash / 25f) * 50);
            ctx.fill(0, 0, sw, sh, (fAlpha << 24) | 0x00CCFF);
        }

        // ── Burst screen shake (simulate via edge + vignette) ────────────────
        if (isBurst) {
            float p = pulse(0.5f);
            int a = (int)(p * 30);
            ctx.fill(0, 0, sw, sh, (a << 24) | 0xFF0000);
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private static String getStatus(AbilityLevel level, ClientAbilityState state) {
        if (!level.isActive()) return "§7[INACTIVE]";
        boolean barrier = state.isSoundBarrierActive();
        boolean burst   = state.isBurstActive();
        return switch (level) {
            case LEVEL_1 -> "§eFast Strike — §7stop on hit";
            case LEVEL_2 -> barrier ? "§b◆ BARRIER ACTIVE · Water Walk ◆" : "§9Building speed...";
            case LEVEL_3 -> burst   ? "§4⚡⚡ BURST ACTIVE — PRESS §cR ⚡⚡"
                                    : "§cGodspeed · Hold §4R§c for BURST";
            default       -> "";
        };
    }

    private static int levelColor(AbilityLevel level, boolean burst) {
        if (burst) return 0xFFFF3300;
        return switch (level) {
            case NONE    -> 0xFF444444;
            case LEVEL_1 -> 0xFFFFAA00;
            case LEVEL_2 -> 0xFF00AAFF;
            case LEVEL_3 -> 0xFFFF3333;
        };
    }

    private static int dotColor(int i) {
        return switch (i) {
            case 1 -> 0xFF7A4400;
            case 2 -> 0xFF004488;
            case 3 -> 0xFF880000;
            default -> 0xFF333333;
        };
    }

    private static float pulse(float speed) {
        return (float)(Math.sin(animTick * speed) * 0.5 + 0.5);
    }

    private static void edgeGlow(DrawContext ctx, int sw, int sh, int alpha, int rgb, int thickness) {
        int col = (alpha << 24) | rgb;
        ctx.fill(0,           0,           sw,              thickness, col);
        ctx.fill(0,           sh-thickness,sw,              sh,        col);
        ctx.fill(0,           0,           thickness,       sh,        col);
        ctx.fill(sw-thickness,0,           sw,              sh,        col);
    }

    private static void drawBorder(DrawContext ctx, int x1, int y1, int x2, int y2, int col) {
        ctx.fill(x1, y1, x2,   y1+1, col);
        ctx.fill(x1, y2-1, x2, y2,   col);
        ctx.fill(x1, y1, x1+1, y2,   col);
        ctx.fill(x2-1, y1, x2, y2,   col);
    }
}
