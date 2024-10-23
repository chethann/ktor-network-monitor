import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.room)
    alias(libs.plugins.ksp)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialisation)
    id("com.vanniktech.maven.publish") version "0.30.0"
}

mavenPublishing {
    coordinates(
        groupId = "io.github.chethann",
        artifactId = "network-monitor",
        version = "0.0.1-alpha1"
    )

    pom {
        name.set("Ktor Network call Monitor Client for KMP")
        description.set("Ktor Network call Monitor Client for KMP. This is inspired by libraries like Chuck which are there only for Android")
        inceptionYear.set("2024")
        url.set("")

        licenses {
            license {
                name.set("MIT")
                url.set("https://opensource.org/license/mit")
            }
        }

        developers {
            developer {
                id.set("chethann")
                name.set("Chethan N")
                email.set("chethann12793@gmail.com")
            }
        }
    }

    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    signAllPublications()
}



kotlin {

    jvm()

    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
        publishLibraryVariants("debug", "release")
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "library"
            isStatic = true
        }
    }

    sourceSets {

        iosMain {
            kotlin.srcDir("build/generated/ksp/metadata")
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }

        commonMain.dependencies {
            implementation(libs.bundles.ktor)
            implementation(libs.bundles.database)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.material3)
            implementation(compose.material3AdaptiveNavigationSuite)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.bundles.material.adaptive)
            implementation(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        // Common compiler options applied to all Kotlin source sets
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

android {
    namespace = "io.github.chethann.network.monitor"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}


dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspJvm", libs.room.compiler)
    //add("kspCommonMainMetadata", libs.room.compiler)

    add("kspIosSimulatorArm64", libs.room.compiler)
    add("kspIosX64", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
}

room {
    schemaDirectory("$projectDir/schemas")
}