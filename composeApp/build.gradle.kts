import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.buildconfig)
}

kotlin {
    jvmToolchain(25)

    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.preview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.jetbrains.navigation3.ui)
            implementation(libs.jetbrains.lifecycle.viewmodel.nav3)
            implementation(libs.jetbrains.lifecycle.viewmodel)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.coroutines)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}

val appVersion = "1.1.1"

buildConfig {
    packageName("com.rafambn.keymanager")
    buildConfigField("APP_VERSION", appVersion)
}

compose.desktop {
    application {
        mainClass = "com.rafambn.keymanager.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "KeyManager"
            packageVersion = appVersion

            linux {
                iconFile.set(project.file("src/jvmMain/resources/icon.png"))
            }
            macOS {
                iconFile.set(project.file("src/jvmMain/resources/icon.icns"))
            }
            windows {
                iconFile.set(project.file("src/jvmMain/resources/icon.ico"))
            }

            modules(
                "java.base",
                "java.datatransfer",
                "java.desktop",
                "java.instrument",
                "java.logging",
                "java.management",
                "java.naming",
                "java.net.http",
                "java.prefs",
                "java.rmi",
                "java.security.jgss",
                "java.security.sasl",
                "java.sql",
                "java.xml",
                "jdk.unsupported",
                "jdk.security.auth",
                "jdk.security.jgss"
            )
        }

    }
}

// jlink strips native commands (--strip-native-commands), which removes keytool from
// the bundled runtime. A dedicated task copies it back from the build JDK before packaging.
abstract class CopyKeytoolTask : DefaultTask() {
    @get:InputFile
    abstract val sourceKeytool: RegularFileProperty

    @get:OutputDirectory
    abstract val targetDir: DirectoryProperty

    @TaskAction
    fun copy() {
        val source = sourceKeytool.get().asFile
        val dir = targetDir.get().asFile
        dir.mkdirs()
        if (source.exists()) {
            val dest = dir.resolve(source.name)
            source.copyTo(dest, overwrite = true)
            dest.setExecutable(true)
        } else {
            logger.warn("keytool not found at ${source.absolutePath} — bundled runtime will not include keytool")
        }
    }
}

afterEvaluate {
    val executable = if (System.getProperty("os.name").lowercase().contains("windows")) "keytool.exe" else "keytool"
    val sourceKeytool = File(System.getProperty("java.home")).resolve("bin/$executable")

    val copyTask = tasks.register("copyKeytool", CopyKeytoolTask::class) {
        this.sourceKeytool.set(sourceKeytool)
        targetDir.set(layout.buildDirectory.dir("compose/tmp/main/runtime/bin"))
        dependsOn(tasks.named("createRuntimeImage"))
    }

    // On macOS/Windows, createDistributable sits between createRuntimeImage and the
    // package task and reads the same runtime directory — it must also depend on copyKeytool.
    // On Linux, packageDeb uses the runtime directly with no intermediate task.
    listOf("createDistributable", "packageDeb", "packageMsi", "packageDmg").forEach { name ->
        tasks.findByName(name)?.dependsOn(copyTask)
    }
}
