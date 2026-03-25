import com.android.build.api.dsl.ApplicationDefaultConfig
import com.android.build.api.dsl.CommonExtension
import com.android.build.gradle.api.AndroidBasePlugin

plugins {
    alias(libs.plugins.agp.app) apply false
    alias(libs.plugins.agp.lib) apply false
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.lsplugin.cmaker)
}

cmaker {
    default {
        arguments.addAll(
            arrayOf(
                "-DANDROID_STL=none",
            )
        )
        abiFilters("arm64-v8a", "x86_64")
    }
    buildTypes {
        if (it.name == "release") {
            arguments += "-DDEBUG_SYMBOLS_PATH=${layout.buildDirectory.asFile.get().absolutePath}/symbols"
        }
    }
}

val androidMinSdkVersion = 26
val androidTargetSdkVersion = 36
val androidCompileSdkVersion = 36
val androidBuildToolsVersion = "36.1.0"
val androidCompileNdkVersion by extra(libs.versions.ndk.get())
val androidSourceCompatibility = JavaVersion.VERSION_21
val androidTargetCompatibility = JavaVersion.VERSION_21
val managerVersionCode by extra(getVersionCode())
val managerVersionName by extra(getVersionName())

fun runGitCommand(vararg args: String): String {
    val process = ProcessBuilder("git", *args)
        .directory(rootDir)
        .redirectErrorStream(true)
        .start()
    val output = process.inputStream.bufferedReader().use { it.readText().trim() }
    process.waitFor()
    return if (process.exitValue() == 0) output else ""
}

fun getGitCommitCount(): Int {
    return runGitCommand("rev-list", "--count", "HEAD").toInt()
}

fun getGitBranchName(): String {
    return runGitCommand("rev-parse", "--abbrev-ref", "HEAD")
}

fun getGitShortSha(): String {
    return runGitCommand("rev-parse", "--short=8", "HEAD")
}

fun normalizeVersionBranch(branch: String): String {
    return branch
        .removePrefix("origin/")
        .removePrefix("refs/heads/")
        .removePrefix("agent/")
        .removeSuffix("-build-manager")
        .replace('/', '-')
        .replace(Regex("[^A-Za-z0-9._-]"), "-")
        .trim('-')
        .ifEmpty { "dev" }
        .take(16)
        .trim('-')
}

fun getVersionCode(): Int {
    val commitCount = getGitCommitCount()
    val major = 1
    return major * 30000 + commitCount
}

fun getVersionName(): String {
    val branch = normalizeVersionBranch(getGitBranchName())
    val shortSha = getGitShortSha()
    return "$branch-$shortSha-nikitos4683"
}

subprojects {
    plugins.withType(AndroidBasePlugin::class.java) {
        extensions.configure(CommonExtension::class.java) {
            compileSdk = androidCompileSdkVersion
            ndkVersion = androidCompileNdkVersion

            defaultConfig {
                minSdk = androidMinSdkVersion
                if (this is ApplicationDefaultConfig) {
                    targetSdk = androidTargetSdkVersion
                    versionCode = managerVersionCode
                    versionName = managerVersionName
                }
                ndk {
                    abiFilters += listOf("arm64-v8a", "x86_64")
                }
            }

            lint {
                abortOnError = true
                checkReleaseBuilds = false
            }

            compileOptions {
                sourceCompatibility = androidSourceCompatibility
                targetCompatibility = androidTargetCompatibility
            }
        }
    }
}
