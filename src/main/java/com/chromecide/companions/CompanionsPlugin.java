package com.chromecide.companions;

import com.chromecide.lowtalk.api.LowTalkApi;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;
import java.util.logging.Level;

/**
 * Companions: NPCs that travel with a player because a dialogue asked them to. Adds the commands {@code <<follow>>},
 * {@code <<stay>>} and {@code <<dismiss>>} and the functions {@code following()}, {@code waiting()} and
 * {@code is_companion()} to LowTalk, all through its public API. Following uses the game's own flock system: the
 * player leads a flock and the companion is a member whose role seeks the leader.
 */
public class CompanionsPlugin extends JavaPlugin {
    private static CompanionsPlugin instance;

    public CompanionsPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static CompanionsPlugin get() {
        return instance;
    }

    @Override
    protected void setup() {
        LowTalkApi api = LowTalkApi.get();
        Companions companions = new Companions(this, api);
        api.registerCommand("follow", "<<follow>>", "This NPC becomes the player's companion and follows them (Adventure mode).", companions::follow);
        api.registerCommand("stay", "<<stay>>", "The companion waits where it stands until told to follow again.", companions::stay);
        api.registerCommand("dismiss", "<<dismiss>>", "The companion goes back to its old life; its original role returns.", companions::dismiss);
        api.registerFunction("following", "following()", "True while this NPC is following the player.", companions::following);
        api.registerFunction("waiting", "waiting()", "True while this NPC is the player's companion but waiting.", companions::waiting);
        api.registerFunction("is_companion", "is_companion()", "True if this NPC is the player's companion, following or waiting.", companions::isCompanion);
        api.addListener(companions);
        getLogger().at(Level.INFO).log("Companions ready: <<follow>>, <<stay>>, <<dismiss>>, following(), waiting(), is_companion()");
    }
}
