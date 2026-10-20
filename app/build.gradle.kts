plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.salamander.dnsswitch"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.salamander.dnsswitch"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "2.0"

        // НОВЫЙ СПОСОБ фильтрации языков
        androidResources {
            localeFilters += listOf("en", "ru", "it", "cs")
        }
    }

    // Не встраиваем в APK/AAB метаданные о зависимостях (метрика Google Play)
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            setProguardFiles(listOf(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            ))
        }
        // Добавим эти настройки и для отладки, если хочешь видеть реальный размер
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/**/LICENSE.txt"
            excludes += "/META-INF/**/*.version"
            excludes += "**/DebugProbesKt.bin"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Оставляем только самое необходимое
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)

    // Инструменты превью нужны только в debug-сборке
    debugImplementation(libs.androidx.compose.ui.tooling)
}