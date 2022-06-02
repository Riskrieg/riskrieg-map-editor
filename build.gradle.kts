import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    id("org.jetbrains.compose") version "1.1.1"
}

group = "com.riskrieg"
version = "2.7.4"
repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation(compose.desktop.currentOs)

    implementation("com.formdev:flatlaf:2.2")
    implementation("com.formdev:flatlaf-intellij-themes:2.2")
    implementation(compose.materialIconsExtended)

    implementation("com.github.aaronjyoder:polylabel-java-mirror:1.3.0")

    implementation("com.riskrieg:core:3.0.0-0.2206-alpha")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.3") // Needed for workaround at EditorModel.kt#343

    implementation("org.jgrapht:jgrapht-io:1.5.1")

    testImplementation("org.jetbrains.kotlin:kotlin-test:1.6.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "17"
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Rpm)
            packageName = "Riskrieg Map Editor"
            packageVersion = version.toString()
            description = "The official map editor for Riskrieg."
            vendor = "Riskrieg"

            val iconsRoot = project.file("src/main/resources/icon/")

            linux {
                iconFile.set(iconsRoot.resolve("linux.png"))
                menuGroup = "Riskrieg"
            }

            windows {
                iconFile.set(iconsRoot.resolve("windows.ico"))
                menuGroup = "Riskrieg"
                dirChooser = true
                perUserInstall = true
                upgradeUuid = "eb7036b9-4a1d-439e-89dd-9575560086d4"
            }

            macOS { // Requires a Mac in order to notarize
                iconFile.set(iconsRoot.resolve("macos.icns"))
            }

        }
    }
}