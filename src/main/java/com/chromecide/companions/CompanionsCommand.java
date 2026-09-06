package com.chromecide.companions;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

import javax.annotation.Nonnull;

/** /companions dismissall: release every companion so the mod can be removed without losing NPCs. */
public class CompanionsCommand extends AbstractCommandCollection {
    public static final String ADMIN = "companions.admin";

    public CompanionsCommand(@Nonnull Companions companions) {
        super("companions", "Companions administration");
        this.requirePermission(ADMIN);
        this.addSubCommand(new DismissAll(companions));
    }

    static class DismissAll extends CommandBase {
        private final Companions companions;

        DismissAll(Companions companions) {
            super("dismissall", "Release every companion in every world and restore their roles (run before removing the mod)");
            this.companions = companions;
            this.requirePermission(ADMIN);
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            companions.dismissAll(line -> context.sendMessage(Message.raw(line)));
        }
    }
}
