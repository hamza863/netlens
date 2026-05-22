# NetLens 🔍

[![](https://jitpack.io/v/hamza863/netlens.svg)](https://jitpack.io/#hamza863/netlens)
[![Build](https://github.com/hamza863/netlens/actions/workflows/build.yml/badge.svg)](https://github.com/hamza863/netlens/actions)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg)](https://android-arsenal.com/api?level=21)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

A lightweight Android network logger. **Shake your device** to see all API calls in a clean bottom sheet.

No notifications. No database. No impact on your API speed.

---

## Why NetLens?

| | Chucker | NetLens |
|---|---|---|
| Storage | Room database (disk writes) | In-memory only |
| Trigger | Notification tray | Shake gesture |
| Response body | Consumes stream | Peeks only — stream untouched |
| Extra dependencies | Room, Notifications | None |
| Release build | No-op variant | No-op variant |

---

## Install

**1. Add JitPack to `settings.gradle.kts`**

```kotlin
repositories {
    google()
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}
```

**2. Add the dependency**

```kotlin
// build.gradle.kts (app)
dependencies {
    debugImplementation("com.github.hamza863:netlens:1.0.0")
    releaseImplementation("com.github.hamza863:netlens-no-op:1.0.0")
}
```

> The `no-op` version does nothing in release — safe to keep in production code.

---

## Setup

**Step 1 — Add the interceptor to OkHttpClient**

```kotlin
val client = OkHttpClient.Builder()
    .addInterceptor(NetLensInterceptor())
    .build()
```

**Step 2 — Install in your Application class**

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NetLens.install(this)
    }
}
```

That's it. **Shake your device** on any screen to open the log viewer.

---

## Options

```kotlin
NetLens.install(
    app = this,
    config = NetLensConfig(
        shakeToOpen    = true,   // enable shake gesture
        maxEntries     = 200,    // how many calls to keep in memory
        maxBodyBytes   = 65536L, // max body size to capture (64 KB)
        shakeThreshold = 12f     // shake sensitivity (lower = more sensitive)
    )
)
```

**Open manually** (e.g. from a debug menu button):

```kotlin
NetLens.show(this) // pass your Activity
```

**Clear all logs:**

```kotlin
NetLens.clear()
```

---

## What you see

Each row in the bottom sheet shows:

- **Method** — color-coded badge (GET, POST, PUT, PATCH, DELETE)
- **URL** — truncated for readability
- **Status code** — green for 2xx, orange for 3xx, red for 4xx, purple for 5xx
- **Duration** — milliseconds
- **Time** — HH:mm:ss

Tap any row for the full detail view — headers, request body, response body — with a **Copy** button.

There's also a **search bar** to filter by URL, method, or status code.

---

## Works with Retrofit

```kotlin
val retrofit = Retrofit.Builder()
    .baseUrl("https://api.example.com/")
    .client(
        OkHttpClient.Builder()
            .addInterceptor(NetLensInterceptor())
            .build()
    )
    .build()
```

---

## License

```
Copyright 2024 Hamza

Licensed under the Apache License, Version 2.0
http://www.apache.org/licenses/LICENSE-2.0
```
