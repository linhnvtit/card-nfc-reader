plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id(libs.plugins.maven.publish.get().pluginId)
}

fun Any?.getInt() = this?.toString()?.toIntOrNull()
fun Any?.getString() = this?.toString()

android {
    namespace = properties["namespace"].getString()
    compileSdk = properties["compileSdk"].getInt()

    defaultConfig {
        minSdk = properties["minSdk"].getInt()
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = properties["jvmTarget"].toString()
    }
}

afterEvaluate {
    android.libraryVariants.forEach { variant ->
        publishing.publications.create(variant.name, MavenPublication::class.java) {
            from(components.findByName(variant.name))

            groupId = properties["publishGroupId"].toString()
            artifactId = properties["publishArtifactId"].toString()
            version = properties["publishVersion"].toString()
        }
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.android.material)
}