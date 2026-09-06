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

## Try it

1. Build LowTalk (`../lowtalk`, `./gradlew build`), then here `./gradlew runServer`.
2. In game: `/npc spawn Kweebec_Merchant`, look at it, `/lowtalk tag companion`, `/gamemode adventure`.
3. Talk to it and choose "Come with me".

## Why this exists

Companions is the first mod written against LowTalk's public API (`com.chromecide.lowtalk.api`). Everything it
needed that the API lacked was added to LowTalk rather than worked around; see the LowTalk changelog.

MIT licensed, like LowTalk.
