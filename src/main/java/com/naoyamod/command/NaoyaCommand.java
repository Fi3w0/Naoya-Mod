package com.naoyamod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.naoyamod.ability.AbilityLevel;
import com.naoyamod.ability.AbilityManager;
import com.naoyamod.network.NaoyaNetworking;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.UUID;

public class NaoyaCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                CommandRegistryAccess access,
                                CommandManager.RegistrationEnvironment env) {

        dispatcher.register(CommandManager.literal("naoya")
                .requires(src -> src.hasPermissionLevel(2))

                // /naoya whitelist add <player>
                .then(CommandManager.literal("whitelist")
                        .then(CommandManager.literal("add")
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .executes(ctx -> {
                                            ServerPlayerEntity p = EntityArgumentType.getPlayer(ctx, "player");
                                            if (AbilityManager.INSTANCE.addToWhitelist(p.getUuid())) {
                                                fb(ctx, "§a✦ " + p.getName().getString() + " can now use Naoya's power.", true);
                                                NaoyaNetworking.sendSyncToPlayer(p);
                                            } else {
                                                fb(ctx, "§e" + p.getName().getString() + " is already whitelisted.", false);
                                            }
                                            return 1;
                                        })))

                        // /naoya whitelist remove <player>
                        .then(CommandManager.literal("remove")
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .executes(ctx -> {
                                            ServerPlayerEntity p = EntityArgumentType.getPlayer(ctx, "player");
                                            if (AbilityManager.INSTANCE.removeFromWhitelist(p.getUuid())) {
                                                fb(ctx, "§c✦ Removed " + p.getName().getString() + " from whitelist.", true);
                                                NaoyaNetworking.sendSyncToPlayer(p);
                                            } else {
                                                fb(ctx, "§e" + p.getName().getString() + " is not whitelisted.", false);
                                            }
                                            return 1;
                                        })))

                        // /naoya whitelist list
                        .then(CommandManager.literal("list")
                                .executes(ctx -> {
                                    StringBuilder sb = new StringBuilder("§6Naoya Whitelist (")
                                            .append(AbilityManager.INSTANCE.getWhitelist().size()).append("):\n");
                                    for (UUID uuid : AbilityManager.INSTANCE.getWhitelist()) {
                                        ServerPlayerEntity online = ctx.getSource().getServer()
                                                .getPlayerManager().getPlayer(uuid);
                                        String name = online != null ? "§a" + online.getName().getString()
                                                : "§7" + uuid.toString().substring(0, 8) + "...";
                                        sb.append("  §e- ").append(name).append("\n");
                                    }
                                    if (AbilityManager.INSTANCE.getWhitelist().isEmpty())
                                        sb.append("  §7(empty)");
                                    fb(ctx, sb.toString(), false);
                                    return 1;
                                })))

                // /naoya level <player> <0-3>  — OP force-set
                .then(CommandManager.literal("level")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .then(CommandManager.argument("lvl", IntegerArgumentType.integer(0, 3))
                                        .executes(ctx -> {
                                            ServerPlayerEntity p = EntityArgumentType.getPlayer(ctx, "player");
                                            int l = IntegerArgumentType.getInteger(ctx, "lvl");
                                            AbilityManager.INSTANCE.setLevel(p, AbilityLevel.fromLevel(l));
                                            NaoyaNetworking.sendSyncToPlayer(p);
                                            fb(ctx, "§bSet " + p.getName().getString()
                                                    + "'s ability to level " + l, true);
                                            return 1;
                                        }))))
        );
    }

    private static void fb(com.mojang.brigadier.context.CommandContext<ServerCommandSource> ctx,
                            String msg, boolean broadcast) {
        ctx.getSource().sendFeedback(() -> Text.literal(msg), broadcast);
    }
}
