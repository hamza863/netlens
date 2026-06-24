# Publishing NetLens

NetLens publishes to **Maven Central** via the
[Vanniktech Maven Publish](https://vanniktech.github.io/gradle-maven-publish-plugin/)
plugin. Group ID: **`io.github.hamza863`**.

```kotlin
// Consumers use:
debugImplementation("io.github.hamza863:netlens:<version>")
releaseImplementation("io.github.hamza863:netlens-no-op:<version>")
```

Publishing is already wired up in `netlens/build.gradle.kts` and
`netlens-no-op/build.gradle.kts`. You only need to do the **one-time setup**
below once; after that, releasing is a single command.

---

## One-time setup

### 1. Sonatype Central account + namespace

1. Sign in at <https://central.sonatype.com> with your **GitHub** account.
2. Go to **Namespaces** → **Add Namespace** → enter `io.github.hamza863`.
   Because it's an `io.github.<user>` namespace owned by your GitHub login, it's
   verified automatically (Central may ask you to create a temporary public repo
   it names — just create it, click **Verify**, then delete it).
3. **Account → Generate User Token.** This gives a **username** and **password**
   used for uploads (not your login password).

### 2. GPG signing key

Central requires signed artifacts.

```bash
# Generate a key (pick RSA 4096, no expiry is fine)
gpg --full-generate-key

# Find the key id (the long hex after "sec   rsa4096/")
gpg --list-secret-keys --keyid-format long

# Publish the PUBLIC key so Central can verify signatures
gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>

# Export the PRIVATE key (armored) for the in-memory signing config
gpg --armor --export-secret-keys <KEY_ID> > netlens-signing-key.asc
```

> Keep `netlens-signing-key.asc` private — never commit it.

### 3. Credentials in `~/.gradle/gradle.properties`

Put these in your **global** Gradle properties (`~/.gradle/gradle.properties`),
**not** in the repo:

```properties
# Central Portal user token (from step 1.3)
mavenCentralUsername=<token-username>
mavenCentralPassword=<token-password>

# GPG signing (in-memory). Paste the full armored key on one line,
# replacing real newlines with \n. Or use the env var form below.
signingInMemoryKey=-----BEGIN PGP PRIVATE KEY BLOCK-----\n...\n-----END PGP PRIVATE KEY BLOCK-----
signingInMemoryKeyPassword=<your-gpg-key-passphrase>
```

Alternatively, pass the key via environment variables (handy for CI), e.g.:

```bash
export ORG_GRADLE_PROJECT_mavenCentralUsername=<token-username>
export ORG_GRADLE_PROJECT_mavenCentralPassword=<token-password>
export ORG_GRADLE_PROJECT_signingInMemoryKey="$(cat netlens-signing-key.asc)"
export ORG_GRADLE_PROJECT_signingInMemoryKeyPassword=<your-gpg-key-passphrase>
```

The build only calls `signAllPublications()` when a signing key is present, so
builds without these properties (local dev, JitPack) still work.

---

---

## Recommended: automated, secret-free releases via GitHub Actions

The most secure setup keeps **no secrets on any developer machine** — releases run
in CI with secrets stored in GitHub. The workflow is at
`.github/workflows/publish.yml` and triggers on a `v*` tag (or manually).

Add these four **repository secrets** (GitHub → Settings → Secrets and variables →
Actions → New repository secret):

| Secret | Value |
|--------|-------|
| `MAVEN_CENTRAL_USERNAME` | Central Portal user-token username |
| `MAVEN_CENTRAL_PASSWORD` | Central Portal user-token password |
| `SIGNING_IN_MEMORY_KEY` | ASCII-armored **private** key (see below) |
| `SIGNING_IN_MEMORY_KEY_PASSWORD` | your GPG passphrase |

Export the in-memory key (paste the whole output as the secret value — multi-line
is fine in GitHub secrets):

```bash
gpg --armor --export-secret-keys <KEY_ID>
```

Then release by pushing a tag:

```bash
git tag v1.2.0 && git push origin v1.2.0
```

CI builds, signs, and publishes automatically. This is the preferred path —
prefer it over publishing from a laptop.

---

## Releasing a new version (local fallback)

1. Bump the version in **both** `netlens/build.gradle.kts` and
   `netlens-no-op/build.gradle.kts` (`coordinates(..., "<new-version>")`),
   update `CHANGELOG.md`, and commit.
2. Publish and auto-release:

   ```bash
   ./gradlew publishAndReleaseToMavenCentral --no-configuration-cache
   ```

   Or, to inspect before releasing, upload to a staging deployment and release it
   manually from the Central Portal UI:

   ```bash
   ./gradlew publishToMavenCentral --no-configuration-cache
   ```

3. Artifacts usually appear on Central within ~15–30 minutes, then propagate to
   the `mavenCentral()` repo shortly after.

> Versions on Central are **immutable** — you can't overwrite a published
> version, so double-check before releasing.

---

## JitPack (fallback)

JitPack still works as a fallback via `jitpack.yml` (it builds from git tags).
Once on Maven Central, prefer the `io.github.hamza863` coordinates above.
