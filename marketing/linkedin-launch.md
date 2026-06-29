# NetLens — LinkedIn launch drafts

Three angles for the launch post. Pick one. Posting tips at the bottom.

---

## Variant 1 — Feature announcement

🔍 I just open-sourced **NetLens** — a lightweight network inspector for Android that won't touch your API latency.

Every Android dev knows the pain of debugging API calls: digging through Logcat, or reaching for a tool that writes every response to a database and re-wraps your network stream.

I wanted something lighter. So I built NetLens.

Shake your device (or tap a floating bubble) and a clean, Compose-native sheet shows every call — method, status, timing, headers, and body.

What makes it different:

⚡ **Zero latency cost** — response bodies are *peeked*, never consumed. Retrofit/Gson/Moshi parse exactly as before.
🧠 **In-memory only** — a ring buffer of your last N calls. No database, no disk writes, no notification spam.
🌳 **Collapsible JSON tree** — explore responses as a tap-to-expand tree, plus XML, form, and image previews.
🔒 **Safe sharing** — `Authorization` and `Cookie` headers are redacted in cURL and HAR exports.
📤 **Copy as cURL · Export as HAR** — reproduce any request in seconds, or open a full session in Chrome DevTools.
🚫 **Safe for release** — drop in the `no-op` variant and NetLens compiles to nothing.

100% Jetpack Compose. No AppCompat. Zero extra dependencies.

Three lines to integrate:

```
debugImplementation("io.github.hamza863:netlens:1.2.0")
// add NetLens.interceptor() to your OkHttpClient
// call NetLens.install(this) in your Application
```

It's on Maven Central and Apache-2.0 licensed. I'd love your feedback — and a ⭐ goes a long way:

👉 github.com/hamza863/netlens

#AndroidDev #Kotlin #JetpackCompose #OpenSource #MobileDevelopment #Android #SoftwareEngineering

---

## Variant 2 — Personal / story-driven (short)

I almost shipped an auth token to a logging tool last month.

I was debugging an API call the usual way — piping response bodies to Logcat — when I realized I'd just dumped a user's `Authorization` header into a log I was about to share. That was the moment I decided to build my own inspector.

Meet **NetLens** 🔍

Shake your phone. Every API call shows up in a clean sheet — status, timing, headers, body. Tap one to explore the JSON as a collapsible tree, or copy it as cURL.

The three things I cared about most:

⚡ It never touches your latency — bodies are *peeked*, not consumed.
🔒 Auth and cookie headers are redacted before you share.
🧠 It's all in memory — no database, no disk, gone when you close the app.

100% Compose. Zero extra dependencies. And a `no-op` variant so it vanishes in release builds.

It's free, open-source, and on Maven Central today.

⭐ github.com/hamza863/netlens

What's the closest call you've had with leaking something in a log? 👇

#AndroidDev #Kotlin #JetpackCompose #OpenSource

> Note: tweak the auth-token anecdote to match something that actually happened to you, so it rings true if anyone asks.

---

## Variant 3 — Chucker comparison

For years, my Android debugging setup was the same: add Chucker, shake, inspect. It's a great tool.

But every time I added it, the same thing nagged me — it spins up a **Room database** to store calls, posts notifications, and wraps my OkHttp response stream to read the body.

For a debug tool, that's a lot of machinery just to *look* at a request.

So I built a lighter one: **NetLens** 🔍

Same shake-to-inspect feel. None of the weight:

🧠 **In-memory only** — a ring buffer of your last N calls. No database, no disk writes.
⚡ **Zero latency cost** — bodies are *peeked*, never consumed. Your parsing runs untouched.
🌳 **Collapsible JSON tree** — explore responses tap-by-tap, not as a wall of text.
🔒 **Redacted sharing** — auth & cookie headers masked in cURL and HAR exports.
📦 **Zero extra dependencies** — 100% Compose, and a `no-op` variant so it disappears in release.

Three lines to wire it in. Free, open-source, on Maven Central.

If you've ever wanted Chucker without the database, this one's for you.

⭐ github.com/hamza863/netlens

What's in your Android debugging toolkit right now? 👇

#AndroidDev #Kotlin #JetpackCompose #OpenSource

---

## Posting tips

- **Attach the demo natively.** Upload `assets/netlens-demo.mp4` directly to the post — native video gets far more reach than a link.
- **Link in the first comment.** LinkedIn suppresses posts with outbound links in the body. Put `github.com/hamza863/netlens` in the first comment and change the CTA to "Link in the comments 👇".
- **Timing.** Tue–Thu mornings (your timezone) tend to perform best for dev audiences.
- **End on a question.** The closing question is deliberate engagement-bait — relevant questions drive comments, which drive reach.
