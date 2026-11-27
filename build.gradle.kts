// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt.android) apply false
    id("jacoco")
}

jacoco {
    toolVersion = "0.8.15"
}

val coveredModules = listOf(":core:common", ":core:domain", ":core:data", ":app")

val jacocoExcludes = listOf(
    "**/ui/**",
    "**/di/**",
    "**/widget/**",
    "**/domain/repository/**",
    "**/*\$DefaultImpls*",
    "**/databinding/**",
    "**/*Binding*",
    "**/R$*",
    "**/R.class",
    "**/BuildConfig.*",
    "**/*_HiltModules*",
    "**/Hilt_*",
    "**/*_Factory*",
    "**/*_Impl*",
    "**/*MembersInjector*"
)

// Aggregated JaCoCo coverage across core modules and app.
// Protects domain and data/mapper logic from regressions across the module boundary.
tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn(coveredModules.map { "$it:testDebugUnitTest" })

    reports {
        xml.required.set(true)
        html.required.set(true)
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/jacocoTestReport/jacocoTestReport.xml"))
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/jacocoTestReport/html"))
    }

    classDirectories.setFrom(
        coveredModules.map { path ->
            val moduleDir = project(path).layout.buildDirectory
            files(
                fileTree(moduleDir.dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes")) {
                    exclude(jacocoExcludes)
                },
                fileTree(moduleDir.dir("intermediates/javac/debug/compileDebugJavaWithJavac/classes")) {
                    exclude(jacocoExcludes)
                }
            )
        }
    )

    sourceDirectories.setFrom(
        coveredModules.map { path -> project(path).file("src/main/java") }
    )

    executionData.setFrom(
        coveredModules.map { path ->
            fileTree(project(path).layout.buildDirectory) {
                include("jacoco/testDebugUnitTest.exec")
            }
        }
    )
}

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
        if (!report.exists()) {
            throw GradleException("Missing JaCoCo XML report at ${report.absolutePath}")
        }
        val factory = javax.xml.parsers.DocumentBuilderFactory.newInstance()
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        val doc = factory.newDocumentBuilder().parse(report)
        val packages = doc.getElementsByTagName("package")
        var failures = 0
        val checkedPackages = mutableListOf<String>()
        for (i in 0 until packages.length) {
            val pkgEl = packages.item(i) as org.w3c.dom.Element
            val pkgName = pkgEl.getAttribute("name")
            val floorEntry = coverageFloors.entries.firstOrNull { pkgName.startsWith(it.key) }
                ?: continue
            checkedPackages += pkgName
            val floor = floorEntry.value
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
        if (checkedPackages.isEmpty()) {
            throw GradleException("No gated packages found in JaCoCo report — check classDirectories configuration!")
        }
        if (failures > 0) throw GradleException("$failures package(s) below coverage floor")
    }
}