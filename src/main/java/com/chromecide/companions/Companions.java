package com.chromecide.companions;

import com.chromecide.lowtalk.api.DialogueContext;
import com.chromecide.lowtalk.api.DialogueListener;
import com.chromecide.lowtalk.api.LowTalkApi;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.flock.FlockMembership;
import com.hypixel.hytale.server.flock.FlockMembershipSystems;
import com.hypixel.hytale.server.flock.FlockPlugin;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.systems.RoleChangeSystem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * The companion rules. State lives in LowTalk's own per-NPC variables, in the companions scope, so it persists and
 * dialogues can read it: {@code owner} (player uuid), {@code state} ("follow" or "wait") and {@code role} (the role
 * to restore). The NPC's role is swapped to {@link #ROLE}, whose behaviour tree seeks its flock leader while it is a
 * flock member and waits when it is not; the player leads that flock, as the game's summoned allies do.
 */
public final class Companions implements DialogueListener {
    /** The follower role shipped in this mod's asset pack. */
    public static final String ROLE = "Companion_Follower";
    /** Variable scope for the companion state, shared by every dialogue. */
    public static final String SCOPE = "companions";
    private static final String STATE_FOLLOW = "Follow";

    private final CompanionsPlugin plugin;
    private final LowTalkApi api;

    public Companions(@Nonnull CompanionsPlugin plugin, @Nonnull LowTalkApi api) {
        this.plugin = plugin;
        this.api = api;
    }

    // ---- functions

    public Object following(DialogueContext ctx, List<Object> args) {
        return ownedBy(ctx) && "follow".equals(state(ctx.getNpcId()));
    }

    public Object waiting(DialogueContext ctx, List<Object> args) {
        return ownedBy(ctx) && "wait".equals(state(ctx.getNpcId()));
    }

    public Object isCompanion(DialogueContext ctx, List<Object> args) {
        return ownedBy(ctx);
    }

    // ---- commands

    /** Make this NPC the player's companion and start following. Returns the narration line. */
    public String follow(DialogueContext ctx, List<String> args) {
        Ref<EntityStore> npc = ctx.getNpcRef();
        Store<EntityStore> store = ctx.getEntityStore();
        PlayerRef player = ctx.getPlayer();
        Ref<EntityStore> playerRef = player.getReference();
        if (npc == null || store == null || playerRef == null) return "There is nobody here to follow you.";
        String owner = owner(ctx.getNpcId());
        if (owner != null && !owner.equals(player.getUuid().toString())) return ctx.getNpcName() + " already travels with someone else.";
        Player p = store.getComponent(playerRef, Player.getComponentType());
        if (p != null && p.getGameMode() != GameMode.Adventure) return ctx.getNpcName() + " can only follow you in Adventure mode.";
        NPCEntity entity = store.getComponent(npc, NPCEntity.getComponentType());
        if (entity == null || entity.getRole() == null) return ctx.getNpcName() + " cannot follow anyone.";

        if (!ROLE.equals(entity.getRoleName())) {
            int index = NPCPlugin.get().getIndex(ROLE);
            if (index < 0) {
                plugin.getLogger().at(Level.WARNING).log("Role %s is missing; is the Companions asset pack loaded?", ROLE);
                return ctx.getNpcName() + " does not know how to follow.";
            }
            if (entity.getRole().isRoleChangeRequested()) return ctx.getNpcName() + " is busy; try again in a moment.";
            api.setNpcVar(ctx.getNpcId(), SCOPE, "role", entity.getRoleName());
            // Keep the look, start in the follow state; the NPC plugin swaps the role on its next tick.
            RoleChangeSystem.requestRoleChange(npc, entity.getRole(), index, false, STATE_FOLLOW, null, store);
        }

        // The player leads a flock; the companion joins it. One flock per player, as the game allows.
        Ref<EntityStore> flock = FlockPlugin.getFlockReference(playerRef, store);
        if (flock == null || !flock.isValid()) {
            store.removeComponentIfExists(playerRef, FlockMembership.getComponentType());
            flock = FlockPlugin.createFlock(store, null, new String[] {ROLE});
            FlockMembershipSystems.join(playerRef, flock, store);
        }
        Ref<EntityStore> current = FlockPlugin.getFlockReference(npc, store);
        if (current == null || !current.equals(flock)) {
            store.removeComponentIfExists(npc, FlockMembership.getComponentType());
            FlockMembershipSystems.join(npc, flock, store);
        }

        api.setNpcVar(ctx.getNpcId(), SCOPE, "owner", player.getUuid().toString());
        api.setNpcVar(ctx.getNpcId(), SCOPE, "state", "follow");
        api.setNpcVar(ctx.getNpcId(), SCOPE, "dialogue", ctx.getDialogueId());
        api.bindNpc(ctx.getNpcId(), ctx.getDialogueId()); // the role changed, so keep this conversation reachable by NPC
        return ctx.getNpcName() + " will follow you.";
    }

    public String stay(DialogueContext ctx, List<String> args) {
        Ref<EntityStore> npc = ctx.getNpcRef();
        Store<EntityStore> store = ctx.getEntityStore();
        if (npc == null || store == null) return null;
        if (!ownedBy(ctx)) return ctx.getNpcName() + " is not travelling with you.";
        // Waiting means staying out of the player's flock, so the leader walking away does not pull the NPC along.
        store.removeComponentIfExists(npc, FlockMembership.getComponentType()); // the role sees this and switches to Wait
        api.setNpcVar(ctx.getNpcId(), SCOPE, "state", "wait");
        return ctx.getNpcName() + " will wait here.";
    }

    public String dismiss(DialogueContext ctx, List<String> args) {
        Ref<EntityStore> npc = ctx.getNpcRef();
        Store<EntityStore> store = ctx.getEntityStore();
        if (npc == null || store == null) return null;
        if (!ownedBy(ctx)) return ctx.getNpcName() + " is not travelling with you.";
        release(ctx.getNpcId(), npc, store);
        return ctx.getNpcName() + " goes back to what they were doing.";
    }

    /** Undo everything follow() did to an NPC: leave the flock, restore the role, forget the state, unbind the dialogue. */
    public void release(@Nonnull UUID npcId, @Nonnull Ref<EntityStore> npc, @Nonnull Store<EntityStore> store) {
        store.removeComponentIfExists(npc, FlockMembership.getComponentType());
        NPCEntity entity = store.getComponent(npc, NPCEntity.getComponentType());
        Object original = api.getNpcVar(npcId, SCOPE, "role");
        if (entity != null && entity.getRole() != null && original instanceof String role && !role.equals(entity.getRoleName())) {
            int index = NPCPlugin.get().getIndex(role);
            if (index >= 0 && !entity.getRole().isRoleChangeRequested()) {
                RoleChangeSystem.requestRoleChange(npc, entity.getRole(), index, false, store);
            } else if (index < 0) {
                plugin.getLogger().at(Level.WARNING).log("Companion %s: original role %s no longer exists; it keeps %s", npcId, role, ROLE);
            }
        }
        Object dialogue = api.getNpcVar(npcId, SCOPE, "dialogue");
        if (dialogue instanceof String id) api.unbindNpc(npcId, id);
        api.setNpcVar(npcId, SCOPE, "owner", null);
        api.setNpcVar(npcId, SCOPE, "state", null);
        api.setNpcVar(npcId, SCOPE, "role", null);
        api.setNpcVar(npcId, SCOPE, "dialogue", null);
    }

    /**
     * Release every companion in every loaded world (the step to run before removing this mod: an NPC left in the
     * follower role would be lost when the role is gone). Reports through {@code out} on each world's thread.
     */
    public void dismissAll(@Nonnull java.util.function.Consumer<String> out) {
        for (World world : Universe.get().getWorlds().values()) {
            world.execute(() -> {
                Store<EntityStore> store = world.getEntityStore().getStore();
                List<Ref<EntityStore>> found = new java.util.ArrayList<>();
                store.forEachChunk(NPCEntity.getComponentType(), (chunk, cb) -> {
                    for (int i = 0; i < chunk.size(); i++) {
                        NPCEntity e = chunk.getComponent(i, NPCEntity.getComponentType());
                        if (e != null && ROLE.equals(e.getRoleName())) found.add(chunk.getReferenceTo(i));
                    }
                });
                int released = 0;
                for (Ref<EntityStore> ref : found) {
                    if (!ref.isValid()) continue;
                    UUIDComponent uuid = store.getComponent(ref, UUIDComponent.getComponentType());
                    if (uuid == null) continue;
                    release(uuid.getUuid(), ref, store);
                    released++;
                }
                out.accept("Companions: released " + released + " companion(s) in world " + world.getName() + ".");
            });
        }
    }

    // ---- listener: nothing yet; here to prove the hook works and to log companion conversations

    @Override
    public void onEnd(@Nonnull DialogueContext ctx) {
        if (ownedBy(ctx)) plugin.getLogger().at(Level.FINE).log("%s finished talking to their companion %s", ctx.getPlayer().getUsername(), ctx.getNpcName());
    }

    // ---- helpers

    private boolean ownedBy(DialogueContext ctx) {
        String owner = owner(ctx.getNpcId());
        return owner != null && owner.equals(ctx.getPlayer().getUuid().toString());
    }

    @Nullable
    private String owner(UUID npcId) {
        Object o = api.getNpcVar(npcId, SCOPE, "owner");
        return o instanceof String s ? s : null;
    }

    @Nullable
    private String state(UUID npcId) {
        Object o = api.getNpcVar(npcId, SCOPE, "state");
        return o instanceof String s ? s : null;
    }

}
