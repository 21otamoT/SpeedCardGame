import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// 1. local.propertiesを読み込む設定
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
    namespace = "com.yukihiro.speedWithYukihiro"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.yukihiro.speedWithYukihiro"
        minSdk = 26
        targetSdk = 36
        versionCode = 5
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Kotlinコード側でBuildConfigを使えるようにする設定
        buildConfigField("Boolean", "IS_RELEASE", "false")
    }

    buildTypes {
        // 🛠️ 開発・デバッグ環境の設定
        getByName("debug") {
            isMinifyEnabled = false

            // AndroidManifest.xml に渡す変数
            manifestPlaceholders["admobAppId"] = localProperties.getProperty("ADMOB_APP_ID_DEBUG")?.trim('"') ?: ""
            // 💡 Kotlinコードから R.string.admob_unit_id で呼べるようにする
            resValue("string", "admob_unit_id", localProperties.getProperty("ADMOB_UNIT_ID_DEBUG")?.trim('"') ?: "")
        }

        // 🚀 本番公開環境の設定
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            // AndroidManifest.xml に渡す変数
            manifestPlaceholders["admobAppId"] = localProperties.getProperty("ADMOB_APP_ID_RELEASE")?.trim('"') ?: ""
            // 💡 Kotlinコードから R.string.admob_unit_id で呼べるようにする
            resValue("string", "admob_unit_id", localProperties.getProperty("ADMOB_UNIT_ID_RELEASE")?.trim('"') ?: "")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    // Google Mobile Ads SDK の追加
    implementation(libs.play.services.ads)
}