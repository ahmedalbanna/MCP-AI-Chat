plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.hilt)
  alias(libs.plugins.detekt)
  alias(libs.plugins.spotless)
  alias(libs.plugins.kover)
}

// Roborazzi snapshot tests boot Robolectric's native graphics stack per test
// and dominate unit-test runtime. Opt in for local runs / CI verification
// with -PscreenshotTests=true; skipped otherwise.
val screenshotTestsEnabled =
  providers.gradleProperty("screenshotTests").getOrElse("false").toBoolean()

android {
  namespace = "com.aistudio.mcpchat"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.mcpchat"
    minSdk = 24
    targetSdk = 36
    versionCode = 2
    versionName = "1.1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      val debugKeystorePath = System.getProperty("user.home") + "/.android/debug.keystore"
      storeFile = file(debugKeystorePath)
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug { signingConfig = signingConfigs.getByName("debugConfig") }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions {
    unitTests {
      isIncludeAndroidResources = true
      // Return defaults for un-mocked android.jar methods (android.util.Log,
      // etc.) instead of throwing in plain JVM unit tests.
      isReturnDefaultValues = true
      all { test ->
        test.maxParallelForks =
          (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
        if (!screenshotTestsEnabled) {
          test.exclude("**/SnapshotTests*")
        }
      }
    }
  }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Export Room schemas into the unit-test assets so MigrationTestHelper can
// validate migrations on the JVM (see DatabaseMigrationTest).
ksp {
  arg("room.schemaLocation", "$projectDir/src/test/assets")
}

dependencies {
    implementation(project(":shared"))
    implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material3.windowsizeclass)
  implementation(libs.androidx.window)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.coil.compose)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.okhttp)
  implementation(libs.androidx.security.crypto)
  implementation(libs.androidx.datastore.preferences)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  testImplementation(libs.androidx.room.testing)
  testImplementation(libs.mockk)
  testImplementation(libs.mockwebserver)
  testImplementation(libs.turbine)
  testImplementation(libs.hilt.android.testing)
  // Real org.json implementation for plain JVM unit tests (the android.jar
  // version is stubbed and would otherwise return nulls under
  // isReturnDefaultValues).
  testImplementation("org.json:json:20240303")
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.hilt.compiler)
  implementation(libs.hilt.android)
  implementation(libs.hilt.navigation.compose)
}

detekt {
  buildUponDefaultConfig = true
  allRules = false
  config.setFrom(file("$rootDir/detekt-config.yml"))
}

// detekt 1.23.x caps jvm-target at 22; without this the Detekt tasks infer
// the daemon's JDK (e.g. 24) and fail. 11 matches compileOptions.
tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
  jvmTarget = "11"
}

kover {
  reports {
    filters {
      excludes {
        classes("*.BuildConfig")
        classes("*.Hilt_*")
        classes("hilt_aggregated_deps.*")
      }
    }
     verify {
        rule {
            // 60% gate enforced (see ROADMAP Phase 12.4).
            minBound(60)
        }
    }
    total {
      verify {
        rule {
            minBound(0)
        }
      }
    }
  }
}

spotless {
  kotlin {
    target("**/*.kt")
    targetExclude("**/build/**")
    ktlint().editorConfigOverride(
      mapOf(
        "max_line_length" to "150",
        "ktlint_function_naming_ignore_when_annotated_with" to "Composable"
      )
    )
  }
}
