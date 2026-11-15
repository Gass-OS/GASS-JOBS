# 🏎️ Kasserine Kart Rally

### A 3D kart racer through Kasserine

[![Godot 4.3](https://img.shields.io/badge/Godot-4.3-478CBF?style=for-the-badge&logo=godot-engine&logoColor=white)](https://godotengine.org)
[![Android](https://img.shields.io/badge/Android-arm64-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/Gass-OS/Frankenstein/actions)
[![Version 1.0](https://img.shields.io/badge/version-1.0-blue?style=for-the-badge)](https://github.com/Gass-OS/Frankenstein)
[![Build](https://github.com/Gass-OS/Frankenstein/actions/workflows/android.yml/badge.svg)](https://github.com/Gass-OS/Frankenstein/actions)

**Kasserine Kart Rally** (this repo is called `Frankenstein`) is a 3D kart racing game built with **Godot 4.3** for **Android**. Drive a kart around the track with touch controls — gas, brake and steering — with real kart physics: acceleration, friction, gravity and turning that only bites while you're moving.

---

## 📖 What it is

- 🏎️ **Kart physics** — a `CharacterBody3D` kart with acceleration, top speed, friction and steering (28 m/s top speed)
- 🛣️ **A track to race** — `track.tscn`, built for mobile touch play
- 📱 **Touch controls** — on-screen gas, brake and steering buttons
- 🤖 **GitHub Actions build** — every push to `main` builds an APK (`Kasserine-Rally-APK`)

> **Status: early prototype.** The game drives and the CI pipeline is set up, but the latest CI runs have failed — the build needs attention before the APK is downloadable. See the [Actions page](https://github.com/Gass-OS/Frankenstein/actions).

---

## 📦 Get the game

When CI is green, download **`Kasserine-Rally-APK`** from the latest successful run's artifacts ([Actions page](https://github.com/Gass-OS/Frankenstein/actions)).

> The game is not on Google Play — the CI artifact is the way to install it.

---

## 🛠️ Build it yourself

```bash
git clone https://github.com/Gass-OS/Frankenstein.git
cd Frankenstein
# Open project.godot with Godot 4.3 — project files live at the repo root
# Android export: arm64-v8a, package com.kasserine.rally
```

Pushing to `main` triggers the same build in CI — no local Android SDK needed.

### Project details

| | |
|:---|---|
| **Engine** | Godot 4.3, GL Compatibility renderer |
| **Language** | GDScript |
| **Genre** | 3D kart racing |
| **Android** | arm64-v8a — package `com.kasserine.rally` |

### Project layout

```
├── main.tscn / track.tscn   # menu + race track
├── kart.gd                  # kart physics + touch controls
├── project.godot
└── .github/workflows/android.yml   # CI: Godot 4.3 + Android SDK, builds the APK
```

### A note for developers

`export_presets.cfg` in the repo currently holds workflow text by mistake — CI regenerates the real export presets before building. Don't edit that file by hand; fix it by letting CI write it, or replace it with real presets.

---

<div align="center">

**Made with Godot in Kasserine, Tunisia 🇹🇳**

*A Gass-OS game — small, fast, and fun.*

</div>
