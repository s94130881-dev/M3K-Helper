@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
    id("kotlin-parcelize")
}

android {
    namespace = "com.remtrik.m3khelper"

    // Android 16
    compileSdk = 37

    defaultConfig {
        applicationId = "com.remtrik.m3khelper"

        // Android 10+
        minSdk = 29

        // Android 16
        targetSdk = 36

        versionCode = 68
        versionName = "6.3.0-TFDID"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    // Galaxy A07 4G = ARM64
    splits {
        abi {
            isEnable = true

            reset()

            include(
                "arm64-v8a"
            )
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )

            vcsInfo.include = false
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    kotlin {
        jvmToolchain(21)

        compilerOptions {
            optIn.add(
                "-XXLanguage:+PropertyParamAnnotationDefaultTargetMode"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    lint {
        disable += listOf(
            "MissingTranslation",
            "TypographyFractions",
            "TypographyEllipsis",
            "IconLocation",
            "IconDensities",
            "ContentDescription"
        )

        abortOnError = false
        checkReleaseBuilds = false
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }

        resources {
            excludes += "META-INF/*.version"
            excludes += "DebugProbesKt.bin"
            excludes += "kotlin-tooling-metadata.json"
        }
    }

    androidComponents {
        onVariants { variant ->

            variant.outputs.forEach { output ->

                val abi = output.filters
                    .find {
                        it.filterType ==
                            com.android.build.api.variant
                                .FilterConfiguration
                                .FilterType
                                .ABI
                    }
                    ?.identifier

                output.outputFileName =
                    "M3K_Helper_v" +
                        "${defaultConfig.versionName}_" +
                        "${defaultConfig.versionCode}-" +
                        "${variant.name}-" +
                        "${abi ?: "all"}.apk"
            }
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    androidResources {
        generateLocaleConfig = true
    }
}

ksp {
    arg(
        "compose-destinations.defaultTransitions",
        "none"
    )
}

dependencies {

    // ========================================
    // AndroidX / Compose
    // ========================================

    implementation(
        libs.androidx.activity.compose
    )

    implementation(
        libs.androidx.compose.material.icons.extended
    )

    implementation(
        libs.androidx.compose.material3
    )

    implementation(
        libs.androidx.compose.ui
    )

    debugImplementation(
        libs.androidx.compose.ui.tooling
    )

    implementation(
        libs.androidx.lifecycle.runtime.compose
    )

    implementation(
        libs.androidx.lifecycle.runtime.ktx
    )

    implementation(
        libs.androidx.lifecycle.viewmodel.compose
    )


    // ========================================
    // Navigation
    // ========================================

    implementation(
        libs.compose.destinations.core
    )

    ksp(
        libs.compose.destinations.ksp
    )


    // ========================================
    // ROOT / LIBSU
    // ========================================

    implementation(
        libs.com.github.topjohnwu.libsu.core
    )

    implementation(
        libs.com.github.topjohnwu.libsu.service
    )

    implementation(
        libs.com.github.topjohnwu.libsu.nio
    )


    // ========================================
    // SHIZUKU
    // ========================================

    implementation(
        "dev.rikka.shizuku:api:13.1.5"
    )

    implementation(
        "dev.rikka.shizuku:provider:13.1.5"
    )


    // ========================================
    // Kotlin Coroutines
    // ========================================

    implementation(
        libs.kotlinx.coroutines.core
    )


    // ========================================
    // Material
    // ========================================

    implementation(
        libs.material
    )

    implementation(
        libs.materialKolor
    )


    // ========================================
    // OkHttp
    // ========================================

    implementation(
        platform(libs.okhttp.bom)
    )

    implementation(
        libs.okhttp
    )
}
