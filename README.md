# Companions

NPC companions for [LowTalk](../lowtalk) dialogues. A dialogue can ask an NPC to come along, wait, or go home:

```
-> Come with me.
    <<follow>>
-> Wait here.
    <<stay>>
-> You can go now.
    <<dismiss>>
```

and read the state with `following()`, `waiting()` and `is_companion()`.

Following is the game's own flock mechanism: the player leads a flock and the companion is a member whose role seeks
the leader, the same way summoned allies follow their summoner. The NPC's role is swapped to `Companion_Follower`
(keeping its look) while it travels, and swapped back on dismiss. The dialogue that recruited it stays bound to that
NPC through LowTalk's run-time bindings, so talking to the companion keeps working whatever its role.

Players can only lead flocks in Adventure mode; in Creative the NPC accepts but does not move.

## Hytale versions

| Companions | LowTalk | Hytale release line | Hytale pre-release line |
|------------|---------|---------------------|-------------------------|
| 0.1.0      | 0.1.0   | 0.6.3, 0.6.4        | 0.7.0-pre.1             |

`./gradlew buildAll` writes one jar per line into `build/dist/`; LowTalk is built from the sibling checkout through
a composite build, so the two always match at source level. Branching follows LowTalk's CONTRIBUTING notes.

## Try it

1. Build LowTalk (`../lowtalk`, `./gradlew build`), then here `./gradlew runServer`.
2. In game: `/npc spawn Kweebec_Merchant`, look at it, `/lowtalk tag companion`, `/gamemode adventure`.
3. Talk to it and choose "Come with me".

## Removing the mod

Run `/companions dismissall` first (permission `companions.admin`). It releases every companion in every loaded
world: leaves the flock, restores the original role, forgets the companion state. An NPC still in the
`Companion_Follower` role when the mod is gone is not a crash, but the NPC plugin cannot rebuild a role it has never
heard of and the NPC is lost. Everything else Companions touches is the game's own (flock membership, roles you
already had) or lives in LowTalk's data files.

## Why this exists

Companions is the first mod written against LowTalk's public API (`com.chromecide.lowtalk.api`). Everything it
needed that the API lacked was added to LowTalk rather than worked around; see the LowTalk changelog.

MIT licensed, like LowTalk.

## AI Use Disclosure

Like LowTalk, Companions was designed, directed and play-tested by one person and mostly written by an AI coding
agent, Claude Code, under that direction. It contains no AI itself and makes no network calls. LowTalk's README
says more under "AI Use Disclosure".
