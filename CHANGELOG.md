# Changelog

## 0.2.0 (2026-09-20)

A compatibility release. Nothing Companions does has changed; what it is built against has.

### Built against LowTalk 0.4.0, Hytale 0.6.8 and 0.7.0-pre.3.1

- **LowTalk 0.4.0 is now the minimum**, and it fixes something that affected dialogues written with these
  commands: a condition could not see what a command above it had just done. `<<follow>>` followed by
  `<<if following()>>` in the same passage took the false branch, because the runtime evaluated every
  condition before running any of the passage's commands. Commands now run where they are written, so that
  pair behaves the way it reads. The sample dialogue never relied on it — it asks at the start of the next
  conversation — but anyone who wrote one that did was quietly getting the wrong branch.
- Played in game on 0.6.8 and on 0.7.0-pre.3.1, the newest build on each line. To be exact about it: the
  jars walked in game were built before the version was bumped, and every class and pack file in them is
  byte-identical to the ones tagged here — the version string in `manifest.json` is the only difference.
  The 0.2.0 jars themselves were booted from the jar on a plain server on both lines.
- Companions itself needed no source change for any of it. It touches 21 Hytale classes, none of them the
  chunk, heightmap or spawn-point APIs that 0.7.0-pre.3 reworked, and it compiles with no deprecation
  warnings on either line.

### Fixed

- `./gradlew buildPreRelease` left a LowTalk jar in `build/dist` carrying *this* mod's version string. The
  `-P` overrides propagate into the composite build, so LowTalk's jar task honoured `-PjarDir` and
  `-Pversion` as well and wrote a copy of itself labelled `LowTalk-0.1.0+hytale...`. Nobody should ship that
  jar, so it no longer sits next to the ones people do.

## 0.1.0 (2026-09-06)

- `<<follow>>`, `<<stay>>`, `<<dismiss>>`; `following()`, `waiting()`, `is_companion()`.
- `/companions dismissall` to release every companion before removing the mod.
- `Companion_Follower` role and the `companion` sample dialogue (bind with `/lowtalk tag companion`).
