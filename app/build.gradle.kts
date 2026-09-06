plugins {
    id("org.jetbrains.kotlin.android")
    id("com.android.application")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

android {
    namespace = "dev.expensemate"
    compileSdk = 35

    defaultConfig {
        // Keep the installed identity so existing users retain their database and preferences.
        applicationId = "com.example.expensemate"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    sourceSets.getByName("test").java.srcDir(rootProject.file("tests"))
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    // Robolectric's bytecode reader must understand the bundled JBR 25 runtime.
    testImplementation(platform("org.ow2.asm:asm-bom:9.8"))
}
