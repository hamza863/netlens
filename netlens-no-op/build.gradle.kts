plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.maven.publish)
}

android {
    namespace  = "com.netlens"
    compileSdk = 36
    defaultConfig { minSdk = 21 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin { jvmToolchain(21) }
}

dependencies {
    compileOnly(libs.okhttp)
    compileOnly(libs.androidx.activity.compose)
}

mavenPublishing {
    coordinates("io.github.hamza863", "netlens-no-op", providers.gradleProperty("VERSION_NAME").get())

    pom {
        name.set("NetLens (no-op)")
        description.set("No-op variant of NetLens for release builds — same API, does nothing.")
        inceptionYear.set("2024")
        url.set("https://github.com/hamza863/netlens")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("hamza863")
                name.set("Hamza")
                url.set("https://github.com/hamza863")
            }
        }
        scm {
            url.set("https://github.com/hamza863/netlens")
            connection.set("scm:git:git://github.com/hamza863/netlens.git")
            developerConnection.set("scm:git:ssh://git@github.com/hamza863/netlens.git")
        }
    }

    publishToMavenCentral()

    if (providers.gradleProperty("signingInMemoryKey").isPresent ||
        providers.gradleProperty("signing.keyId").isPresent
    ) {
        signAllPublications()
    }
}
