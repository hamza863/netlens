plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.maven.publish)
}

android {
    namespace  = "com.netlens"
    compileSdk = 36

    defaultConfig {
        minSdk = 21
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        compose = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(21)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.okhttp)
    implementation(libs.androidx.lifecycle.common)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)

    testImplementation(libs.junit)
    testImplementation(libs.mockwebserver)
}

mavenPublishing {
    coordinates("io.github.hamza863", "netlens", providers.gradleProperty("VERSION_NAME").get())

    pom {
        name.set("NetLens")
        description.set("Lightweight Android network logger — shake to inspect, zero DB overhead.")
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

    // Upload to the Central Portal (https://central.sonatype.com).
    publishToMavenCentral()

    // Only sign when a key is configured, so local builds without a key
    // (which only need publishToMavenLocal) don't fail.
    if (providers.gradleProperty("signingInMemoryKey").isPresent ||
        providers.gradleProperty("signing.keyId").isPresent
    ) {
        signAllPublications()
    }
}
