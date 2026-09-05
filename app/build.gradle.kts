@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
    id("kotlin-parcelize")
}

android {
    namespace = "com.remtrik.m3khelper"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.remtrik.m3khelper"

        minSdk = 29
        targetSdk = 36

        versionCode = 68
        versionName = "6.3.0-TFDID"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    splits {
        abi {
            isEnable = true

            reset()

            include(
                "arm64-v8a",
                "x86_64"
            )
        }
    }

    buildTypes {

        release {

            isShrinkResources = true
            isMinifyEnabled = true

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
        sourceCompatibility =
            JavaVersion.VERSION_21

        targetCompatibility =
            JavaVersion.VERSION_21
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

            excludes +=
                "META-INF/*.version"

            excludes +=
                "DebugProbesKt.bin"

            excludes +=
                "kotlin-tooling-metadata.json"
        }
    }

    androidComponents {

        onVariants { variant ->

            variant.outputs.forEach { output ->

                val abi =
                    output.filters.find {
                        it.filterType ==
                            com.android.build.api.variant
                                .FilterConfiguration
                                .FilterType
                                .ABI
                    }?.identifier

                output.outputFileName.set(
                    "M3K_Helper_v" +
                        "${defaultConfig.versionName}_" +
                        "${defaultConfig.versionCode}-" +
                        "${variant.name}-" +
                        "${abi ?: "all"}.apk"
                )
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

    // =========================
    // AndroidX / Compose
    // =========================

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

    // =========================
    // Navigation
    // =========================

    implementation(
        libs.compose.destinations.core
    )

    ksp(
        libs.compose.destinations.ksp
    )

    // =========================
    // MAGISK / ROOT
    // =========================

    implementation(
        libs.com.github.topjohnwu.libsu.core
    )

    implementation(
        libs.com.github.topjohnwu.libsu.service
    )

    implementation(
        libs.com.github.topjohnwu.libsu.nio
    )

    // =========================
    // SHIZUKU
    // =========================

    implementation(
        "dev.rikka.shizuku:api:13.1.5"
    )

    implementation(
        "dev.rikka.shizuku:provider:13.1.5"
    )

    // =========================
    // Kotlin
    // =========================

    implementation(
        libs.kotlinx.coroutines.core
    )

    // =========================
    // Material
    // =========================

    implementation(
        libs.material
    )

    implementation(
        libs.materialKolor
    )

    // =========================
    // OkHttp
    // =========================

    implementation(
        platform(libs.okhttp.bom)
    )

    implementation(
        libs.okhttp
    )
}

A API e o provider são módulos separados no projeto oficial, e a documentação demonstra justamente a inclusão de "dev.rikka.shizuku:api" e "dev.rikka.shizuku:provider".

---

⚠️ O ponto que você não deve ignorar

Com esses três arquivos, o aplicativo reconhece e autoriza Shizuku, mas isso não converte automaticamente seu código existente de root para Shizuku.

Por exemplo, se em algum outro arquivo você possui:

Shell.cmd("mount ...").exec()

ou:

Shell.su("settings put ...").exec()

essas chamadas continuam sendo LibSU/root.

Para realmente fazer o M3K Helper ser:

                 M3K Helper
                     │
          ┌──────────┴──────────┐
          │                     │
       ROOT?                 ROOT não?
          │                     │
         SIM                 Shizuku?
          │                     │
        LibSU               SIM → Shizuku
                                │
                              NÃO
                                │
                         sem privilégios

precisamos criar uma camada, por exemplo:

PrivilegedShell.exec(command)

que escolha automaticamente:

LibSU → se Magisk/root estiver disponível

Shizuku → se root não estiver disponível

erro → se nenhum dos dois estiver disponível

Essa é a parte que realmente permitirá substituir Magisk por Shizuku nas operações do M3K Helper. O Shizuku também não fornece poderes equivalentes ao root quando iniciado via ADB; as permissões são diferentes e algumas operações que funcionam com root podem ser recusadas pelo Shizuku.

Fontes oficiais

- "Shizuku API — GitHub oficial" (https://reference-url-citation.invalid/6)
- "Shizuku — GitHub oficial" (https://reference-url-citation.invalid/7)

Se o objetivo é realmente eliminar a dependência de Magisk para as funções do M3K Helper, o próximo arquivo que eu modificaria é justamente a camada que hoje chama "Shell.su()"/"Shell.cmd()": nela podemos implementar o backend Root + Shizuku, em vez de apenas colocar o botão de autorização.
