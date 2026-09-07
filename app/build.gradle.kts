@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
    id("kotlin-parcelize")
}

android {
    namespace = "com.remtrik.m3khelper"

    compileSdk = 36

    defaultConfig {
        applicationId = "com.remtrik.m3khelper"

        minSdk = 29
        targetSdk = 36

        versionCode = 69
        versionName = "6.3.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    /*
     * NÃO usar splits ABI.
     *
     * O APK será universal e poderá ser instalado
     * normalmente em aparelhos ARM64.
     */
    splits {
        abi {
            isEnable = false
        }
    }

    buildTypes {

        debug {
            isMinifyEnabled = false
            isShrinkResources = false

            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }

        release {
            /*
             * Deixe R8 desligado inicialmente.
             * Depois que o APK estiver funcionando,
             * pode ser reativado.
             */
            isMinifyEnabled = false
            isShrinkResources = false

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )

            vcsInfo.include = false
        }
    }

    /*
     * Kotlin / Java 21
     */
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

    /*
     * Jetpack Compose
     */
    buildFeatures {
        compose = true
        buildConfig = true
    }

    /*
     * Lint
     */
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

    /*
     * Packaging
     */
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

    /*
     * Nome dos APKs.
     */
    androidComponents {
        onVariants { variant ->

            variant.outputs.forEach { output ->

                output.outputFileName =
                    "M3K-Helper-" +
                        "${variant.name}-" +
                        "v${defaultConfig.versionName}-" +
                        "${defaultConfig.versionCode}.apk"
            }
        }
    }

    /*
     * Não colocar informações desnecessárias
     * de dependências dentro do APK.
     */
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    /*
     * Configuração de idiomas.
     */
    androidResources {
        generateLocaleConfig = true
    }
}

/*
 * KSP
 */
ksp {
    arg(
        "compose-destinations.defaultTransitions",
        "none"
    )
}

dependencies {

    // ========================================
    // ANDROIDX / COMPOSE
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
    // NAVIGATION
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
    // COROUTINES
    // ========================================

    implementation(
        libs.kotlinx.coroutines.core
    )


    // ========================================
    // MATERIAL
    // ========================================

    implementation(
        libs.material
    )

    implementation(
        libs.materialKolor
    )


    // ========================================
    // OKHTTP
    // ========================================

    implementation(
        platform(libs.okhttp.bom)
    )

    implementation(
        libs.okhttp
    )
}
