plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
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
    publishing { singleVariant("release") { withSourcesJar() } }
}

dependencies {
    compileOnly(libs.okhttp)
    compileOnly(libs.androidx.activity.compose)
}

afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                from(components["release"])
                groupId    = "com.github.hamza863"
                artifactId = "netlens-no-op"
                version    = "1.1.0"
            }
        }
    }
}
