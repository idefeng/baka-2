import java.security.MessageDigest

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val generatedVoskAssetsDir = layout.buildDirectory.dir("generated/assets/vosk")
val generateVoskModelUuid by tasks.registering {
    val sourceModelDir = layout.projectDirectory.dir("src/main/assets/model")
    val uuidFile = generatedVoskAssetsDir.map { it.file("model/uuid") }
    inputs.files(
        fileTree(sourceModelDir) {
            exclude("README.md")
            exclude("uuid")
        }
    )
    outputs.file(uuidFile)

    doLast {
        val outputFile = uuidFile.get().asFile
        outputFile.parentFile.mkdirs()
        val modelFiles = fileTree(sourceModelDir) {
            exclude("README.md")
            exclude("uuid")
        }.files.filter { it.isFile }.sortedBy { it.relativeTo(sourceModelDir.asFile).path }
        val digest = MessageDigest.getInstance("SHA-256")
        modelFiles.forEach { file ->
            // Vosk StorageService 用 uuid 判断是否需要重新解包；模型变更时必须随之变化。
            digest.update(file.relativeTo(sourceModelDir.asFile).path.toByteArray())
            digest.update(file.readBytes())
        }
        val hash = digest.digest().joinToString("") { "%02x".format(it) }.take(16)
        outputFile.writeText("livesense-japanese-vosk-model-$hash")
    }
}

android {
    namespace = "com.livesense.japanese"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.livesense.japanese"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    sourceSets {
        getByName("main") {
            assets.srcDir(generatedVoskAssetsDir)
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    // 统一 Kotlin 字节码目标，避免单元测试编译目标不一致。
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

tasks.named("preBuild") {
    dependsOn(generateVoskModelUuid)
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.10.01"))
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("com.google.mediapipe:tasks-genai:0.10.27")
    implementation("com.alphacephei:vosk-android:0.3.75")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}
