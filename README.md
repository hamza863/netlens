<div align="center">

# NetLens 🔍

**A lightweight, zero-overhead network inspector for Android.**

Shake your device to inspect every API call in a clean, Compose-native bottom sheet — then copy it as cURL or export the whole session as HAR.

[![JitPack](https://jitpack.io/v/hamza863/netlens.svg)](https://jitpack.io/#hamza863/netlens)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg)](https://android-arsenal.com/api?level=21)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](#license)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-100%25-4285F4.svg)](https://developer.android.com/jetpack/compose)

</div>

---

## Why NetLens?

NetLens captures requests **in memory** and renders them with **Jetpack Compose** — no database, no notification spam, and **no measurable impact on your API latency** (response bodies are *peeked*, never consumed).

| | Chucker | **NetLens** |
|---|---|---|
| Storage | Room database (disk writes) | **In-memory only** |
| Trigger | Notification tray | **Shake gesture / floating bubble** |
| Response body | Consumes & re-wraps the stream | **Peeks only — stream untouched** |
| Body formatting | JSON | **JSON · XML · form-urlencoded · image preview** |
| Export | — | **cURL · HAR (DevTools-compatible)** |
| Extra dependencies | Room, Notifications | **None** |
| Release build | No-op variant | **No-op variant** |
| UI | Views / Fragments | **100% Compose, no AppCompat** |

---

## Features

- 🤳 **Shake to open** — or enable a draggable **floating bubble** (great for emulators).
- ⚡ **Zero latency cost** — bodies are peeked via `Buffer.clone()`; Retrofit/Gson/Moshi parse as normal.
- 🧠 **In-memory ring buffer** — keeps the last *N* calls, drops the oldest, never touches disk.
- 🔎 **Search & filter** — free-text search plus quick chips (All / 2xx / 3xx / 4xx / 5xx / Failed).
- 📊 **Session stats** — total calls, failures, and average duration at a glance.
- 🎨 **Smart formatting** — pretty-prints JSON, XML and form-urlencoded; previews images; labels binary payloads instead of dumping mojibake.
- 📦 **Large-body handling** — long payloads are truncated with a one-tap **“View full body.”**
- 📋 **Copy as cURL** — reproduce any request in your terminal or Postman instantly.
- 📤 **Export as HAR** — share a full session as a `.har` file that opens in Chrome DevTools, Charles or Proxyman.
- 🚫 **Safe for release** — drop in the `no-op` variant and NetLens compiles to nothing.

---

## Install

**1. Add JitPack** to `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

**2. Add the dependency** in your app's `build.gradle.kts` — the real library for debug, the no-op for release:

```kotlin
dependencies {
    debugImplementation("com.github.hamza863:netlens:1.1.1")
    releaseImplementation("com.github.hamza863:netlens-no-op:1.1.1")
}
```

> The `no-op` artifact mirrors the public API but does nothing, so calls to NetLens are safe to leave in production code.

---

## Quick start

**1. Add the interceptor to your `OkHttpClient`:**

```kotlin
val client = OkHttpClient.Builder()
    .addInterceptor(NetLens.interceptor())
    .build()
```

**2. Install shake-to-open in your `Application`:**

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NetLens.install(this)
    }
}
```

That's it — **shake your device** on any screen to open the viewer.

> Works seamlessly with Retrofit: pass the same `OkHttpClient` to `Retrofit.Builder().client(...)`.

---

## Configuration

```kotlin
NetLens.install(
    app = this,
    config = NetLensConfig(
        shakeToOpen    = true,    // open on shake
        showBubble     = false,   // show a draggable floating button
        maxEntries     = 200,     // calls kept in memory
        maxBodyBytes   = 64 * 1024, // max body captured per call (64 KB)
        shakeThreshold = 12f      // shake sensitivity (lower = more sensitive)
    )
)
```

| Option | Default | Description |
|---|---|---|
| `shakeToOpen` | `true` | Open the viewer when the device is shaken. |
| `showBubble` | `false` | Show a draggable floating button to open the viewer. |
| `maxEntries` | `200` | Number of calls retained in the in-memory buffer. |
| `maxBodyBytes` | `65536` | Maximum request/response body size captured. |
| `shakeThreshold` | `12f` | G-force above gravity that counts as a shake. |

---

## Public API

```kotlin
NetLens.interceptor()        // the OkHttp Interceptor to add to your client
NetLens.install(app, config) // enable shake / bubble triggers
NetLens.show(activity)       // open the viewer manually (e.g. from a debug menu)
NetLens.clear()              // drop all captured logs
NetLens.logCount()           // number of calls currently stored
```

**Open manually** from a debug button:

```kotlin
NetLens.show(this) // pass your ComponentActivity
```

---

## What you see

**List view** — each row shows a color-coded **method** badge, the **URL**, a status-colored **code** (green 2xx · amber 3xx · red 4xx · purple 5xx), **duration**, **response size**, and **time**.

**Detail view** — tap any row for a full-screen breakdown:

- Request URL, headers and body
- Response status, headers and body
- **Pretty-printed** JSON / XML / form bodies, **image previews**, and binary labels
- **cURL**, **Share** and **Copy** actions in the toolbar

Use the **search bar** and **filter chips** to narrow things down, and **Export** in the header to share the session as HAR.

---

## Requirements

- **minSdk 21+**
- **Jetpack Compose** (the host app must already use Compose)
- **OkHttp 4 or 5**

No ProGuard/R8 rules are required; the library ships its own consumer rules.

---

## License

```
Copyright 2024 Hamza

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
```
