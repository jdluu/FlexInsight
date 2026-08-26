plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    id("jacoco")
}

android {
    namespace = "com.jdluu.flexinsight"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.jdluu.flexinsight"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = freeCompilerArgs + listOf("-Xskip-metadata-version-check")
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.text.google.fonts)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore (user preferences; API key uses encrypted prefs)
    implementation(libs.androidx.datastore.preferences)

    // Security
    implementation(libs.androidx.security.crypto)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.androidx.work.testing)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)

    // ML Kit Prompt API (Gemini Nano)
    implementation(libs.mlkit.genai.prompt)
    implementation(libs.guava.android)
    implementation(libs.kotlinx.coroutines.guava)

    // Health Connect
    implementation(libs.androidx.health.connect)

    // Home screen widget (Glance)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
}

// JaCoCo coverage for JVM unit tests. UI composables/theme and generated
// code are excluded; the gate protects domain/data logic from regressions.
tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("testDebugUnitTest"))
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    val classDirs = fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
        exclude(
            "**/ui/**",
            "**/di/**",
            "**/databinding/**",
            "**/*Binding*",
            "**/R$*",
            "**/R.class",
            "**/*_HiltModules*",
            "**/Hilt_*",
            "**/*_Factory*",
            "**/*_Impl*",
            "**/*MembersInjector*"
        )
    }
    classDirectories.setFrom(classDirs)
    sourceDirectories.setFrom(files("src/main/java"))
    executionData.setFrom(fileTree(layout.buildDirectory) {
        include("jacoco/testDebugUnitTest.exec")
    })
}

tasks.withType<Test>().configureEach {
    finalizedBy(tasks.named("jacocoTestReport"))
}

// Coverage gate on refactored logic packages. Fails the build if any drops
// below its floor, protecting domain/data behavior from regression.
val coverageFloors = mapOf(
    "com/jdluu/flexinsight/domain" to 0.95,
    "com/jdluu/flexinsight/data/mapper" to 0.90
)

tasks.register("verifyCoverage") {
    dependsOn(tasks.named("jacocoTestReport"))
    doLast {
        val report = layout.buildDirectory.file(
            "reports/jacoco/jacocoTestReport/jacocoTestReport.xml"
        ).get().asFile
        val factory = javax.xml.parsers.DocumentBuilderFactory.newInstance()
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        val doc = factory.newDocumentBuilder().parse(report)
        val packages = doc.getElementsByTagName("package")
        var failures = 0
        for (i in 0 until packages.length) {
            val pkgEl = packages.item(i) as org.w3c.dom.Element
            val pkgName = pkgEl.getAttribute("name")
            val floorEntry = coverageFloors.entries.firstOrNull { pkgName.startsWith(it.key) }
                ?: continue
            val floor = floorEntry.value
            // Aggregate the package-level LINE counter (direct child), not per-class ones.
            var missed = 0.0
            var covered = 0.0
            val children = pkgEl.childNodes
            for (j in 0 until children.length) {
                val node = children.item(j)
                if (node is org.w3c.dom.Element && node.tagName == "counter" && node.getAttribute("type") == "LINE") {
                    missed = node.getAttribute("missed").toDouble()
                    covered = node.getAttribute("covered").toDouble()
                }
            }
            val ratio = covered / (missed + covered)
            if (ratio < floor) {
                failures++
                println("COVERAGE GATE FAILED: $pkgName " + String.format("%.1f", ratio * 100) + "% < " + floor * 100 + "%")
            } else {
                println("Coverage OK: $pkgName " + String.format("%.1f", ratio * 100) + "%")
            }
        }
        if (failures > 0) throw GradleException("$failures package(s) below coverage floor")
    }
}
