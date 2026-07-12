import dev.kikugie.stonecutter.data.ParsedVersion
import org.gradle.api.Action
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import java.io.File
import java.net.URI

plugins {
    // `maven-publish`
    // id("me.modmuss50.mod-publish-plugin")
    id("dev.kikugie.fletching-table.fabric") version "0.1.0-alpha.22"
}

apply(plugin = if (sc.current.version == "26.1") "net.fabricmc.fabric-loom" else "net.fabricmc.fabric-loom-remap")

version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = property("mod.id") as String

val isNonRemappingMinecraft = sc.current.version == "26.1"
val loomExtension = extensions.getByName("loom")
val requiredJava = when {
    isNonRemappingMinecraft -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.6" -> JavaVersion.VERSION_21
    sc.current.parsed >= "1.18" -> JavaVersion.VERSION_17
    sc.current.parsed >= "1.17" -> JavaVersion.VERSION_16
    else -> JavaVersion.VERSION_1_8
}

repositories {
    /**
     * Restricts dependency search of the given [groups] to the [maven URL][url],
     * improving the setup speed.
     */
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }
    strictMaven("https://www.cursemaven.com", "CurseForge", "curse.maven")
    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")

    maven {
        name = "Terraformers"
        url = URI.create("https://maven.terraformersmc.com/")
    }

    maven {
        name = "Xander Maven"
        url = URI.create("https://maven.isxander.dev/releases")
    }

    maven {
        name = "UkuLib Maven"
        url = URI.create("https://maven.uku3lig.net/releases")
    }

    maven {
        name = "Jitpack"
        url = URI.create("https://jitpack.io")
    }
}

dependencies {
    val modImplementationConfiguration = if (isNonRemappingMinecraft) "implementation" else "modImplementation"
    val modApiConfiguration = if (isNonRemappingMinecraft) "api" else "modApi"

    // minecraft things
    add("minecraft", "com.mojang:minecraft:${sc.current.version}")
    if (!isNonRemappingMinecraft) {
        add("mappings", loomExtension.javaClass.getMethod("officialMojangMappings").invoke(loomExtension))
    }
    add(modImplementationConfiguration, "net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")

    // mod dependencies / integrations
    if (!isNonRemappingMinecraft) {
        add(modImplementationConfiguration, "net.uku3lig:ukulib:${property("deps.ukulib")}")
    }
    add(modImplementationConfiguration, "net.kyori:adventure-platform-fabric:${property("deps.adventure")}")
    add(modImplementationConfiguration, "net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")

    // bundled dependencies & libraries
    add("include", "net.kyori:adventure-platform-fabric:${property("deps.adventure")}")
    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")
    testImplementation("org.junit.jupiter:junit-jupiter:6.0.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.0.3")

    // mod integrations
    add(modApiConfiguration, fletchingTable.modrinth("modmenu", sc.current.version, "fabric"))
    if (!isNonRemappingMinecraft) {
        add(modApiConfiguration, fletchingTable.modrinth("tiertagger", sc.current.version, "fabric"))
    }
}

@Suppress("UNCHECKED_CAST")
val decompilerOptions = loomExtension.javaClass.getMethod("getDecompilerOptions").invoke(loomExtension) as NamedDomainObjectContainer<Any>
@Suppress("UNCHECKED_CAST")
val runConfigs = loomExtension.javaClass.getMethod("getRunConfigs").invoke(loomExtension) as NamedDomainObjectContainer<Any>

(loomExtension.javaClass.getMethod("getFabricModJsonPath").invoke(loomExtension) as RegularFileProperty)
    .set(rootProject.file("src/main/resources/fabric.mod.json")) // Useful for interface injection
(loomExtension.javaClass.getMethod("getAccessWidenerPath").invoke(loomExtension) as RegularFileProperty)
    .set(rootProject.file("src/main/resources/${if (isNonRemappingMinecraft) "template-26.1.accesswidener" else "template.accesswidener"}"))

decompilerOptions.named("vineflower").configure(object : Action<Any> {
    override fun execute(decompiler: Any) {
        val options = decompiler.javaClass.getMethod("getOptions").invoke(decompiler)
        options.javaClass.getMethod("put", Any::class.java, Any::class.java)
            .invoke(options, "mark-corresponding-synthetics", "1") // Adds names to lambdas - useful for mixins
    }
})

runConfigs.all(object : Action<Any> {
    override fun execute(runConfig: Any) {
        runConfig.javaClass.getMethod("ideConfigGenerated", Boolean::class.javaPrimitiveType!!).invoke(runConfig, true)
        runConfig.javaClass.getMethod("vmArgs", Array<String>::class.java)
            .invoke(runConfig, arrayOf("-Dmixin.debug.export=true") as Any) // Exports transformed classes for debugging
        runConfig.javaClass.getMethod("setRunDir", String::class.java).invoke(runConfig, "../../run") // Shares the run directory between versions
    }
})

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava
}

val generatedConfigContainerIndexDir = layout.buildDirectory.dir("generated/sources/configContainerIndex/java/main")

sourceSets.named("main") {
    java.srcDir(generatedConfigContainerIndexDir)
    if (isNonRemappingMinecraft) {
        java.exclude("dev/candycup/lifestealutils/integrations/tiertagger/**")
        resources.exclude("tiertagger-fairplay.mixins.json")
    }
}

tasks {
    register("generateConfigContainerIndex") {
        group = "build"
        description = "Generates config container index from @Configurable* declarations"

        val sourceRoot = rootProject.file("src/main/java")
        val outputRoot = generatedConfigContainerIndexDir
        val outputFile = outputRoot.map {
            File(it.asFile, "dev/candycup/lifestealutils/config/generated/GeneratedConfigContainerIndex.java")
        }

        inputs.dir(sourceRoot)
        outputs.file(outputFile)

        doLast {
            val configContainerRegex = Regex("@(?:SerialEntry|Configurable(?:Boolean|String|Minimessage|Float|Enum|List|ToggleGroup))\\b")
            val packageRegex = Regex("(?m)^\\s*package\\s+([a-zA-Z0-9_.]+)\\s*;")
            val classRegex = Regex("(?m)^\\s*(public\\s+)?(final\\s+)?(abstract\\s+)?(class|enum|interface|record)\\s+([A-Za-z_][A-Za-z0-9_]*)")
            val discoveredClasses = linkedSetOf<String>()

            sourceRoot.walkTopDown()
                .filter { it.isFile && it.extension == "java" }
                .forEach { javaFile ->
                    val source = javaFile.readText()
                    if (!configContainerRegex.containsMatchIn(source)) {
                        return@forEach
                    }

                    val packageName = packageRegex.find(source)?.groupValues?.get(1) ?: return@forEach
                    val className = classRegex.find(source)?.groupValues?.get(5) ?: return@forEach
                    discoveredClasses.add("$packageName.$className")
                }

            val sortedClasses = discoveredClasses.toList().sorted()
            val output = outputFile.get()
            output.parentFile.mkdirs()

            val content = buildString {
                appendLine("package dev.candycup.lifestealutils.config.generated;")
                appendLine()
                appendLine("import dev.candycup.lifestealutils.config.ConfigContainerRegistry;")
                appendLine()
                appendLine("public final class GeneratedConfigContainerIndex {")
                appendLine("   private GeneratedConfigContainerIndex() {")
                appendLine("   }")
                appendLine()
                appendLine("   public static void registerAll() {")
                appendLine("      ConfigContainerRegistry.clear();")
                sortedClasses.forEach { fqcn ->
                    appendLine("      ConfigContainerRegistry.registerContainer($fqcn.class);")
                }
                appendLine("   }")
                appendLine("}")
            }

            output.writeText(content)
        }
    }

    named("compileJava") {
        dependsOn("generateConfigContainerIndex")
    }

    named("sourcesJar") {
        dependsOn("generateConfigContainerIndex")
    }

    named<Test>("test") {
        useJUnitPlatform()
    }

    processResources {
        inputs.property("id", project.property("mod.id"))
        inputs.property("name", project.property("mod.name"))
        inputs.property("version", project.property("mod.version"))
        inputs.property("minecraft", project.property("mod.mc_dep"))
        inputs.property("java", requiredJava.majorVersion)
        inputs.property("tiertaggerMixins", if (isNonRemappingMinecraft) "lifestealutils-empty.mixins.json" else "tiertagger-fairplay.mixins.json")

        val props = mapOf(
            "id" to project.property("mod.id"),
            "name" to project.property("mod.name"),
            "version" to project.property("mod.version"),
            "minecraft" to project.property("mod.mc_dep"),
            "java" to requiredJava.majorVersion,
            "tiertaggerMixins" to if (isNonRemappingMinecraft) "lifestealutils-empty.mixins.json" else "tiertagger-fairplay.mixins.json"
        )

        filesMatching("fabric.mod.json") { expand(props) }

        val mixinJava = "JAVA_${requiredJava.majorVersion}"
        filesMatching("*.mixins.json") { expand("java" to mixinJava) }
    }

    // Builds the version into a shared folder in `build/libs/${mod version}/`
    register<Copy>("buildAndCollect") {
        group = "build"
        val jarTaskName = if (isNonRemappingMinecraft) "jar" else "remapJar"
        val sourcesJarTaskName = if (isNonRemappingMinecraft) "sourcesJar" else "remapSourcesJar"
        from(named<AbstractArchiveTask>(jarTaskName).map { it.archiveFile }, named<AbstractArchiveTask>(sourcesJarTaskName).map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }
}

/*
// Publishes builds to Modrinth and Curseforge with changelog from the CHANGELOG.md file
publishMods {
    file = tasks.remapJar.map { it.archiveFile.get() }
    additionalFiles.from(tasks.remapSourcesJar.map { it.archiveFile.get() })
    displayName = "${property("mod.name")} ${property("mod.version")} for ${property("mod.mc_title")}"
    version = property("mod.version") as String
    changelog = rootProject.file("CHANGELOG.md").readText()
    type = STABLE
    modLoaders.add("fabric")

    dryRun = providers.environmentVariable("MODRINTH_TOKEN").getOrNull() == null
        || providers.environmentVariable("CURSEFORGE_TOKEN").getOrNull() == null

    modrinth {
        projectId = property("publish.modrinth") as String
        accessToken = providers.environmentVariable("MODRINTH_TOKEN")
        minecraftVersions.addAll(property("mod.mc_targets").toString().split(' '))
        requires {
            slug = "fabric-api"
        }
    }

    curseforge {
        projectId = property("publish.curseforge") as String
        accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
        minecraftVersions.addAll(property("mod.mc_targets").toString().split(' '))
        requires {
            slug = "fabric-api"
        }
    }
}
 */
/*
// Publishes builds to a maven repository under `com.example:template:0.1.0+mc`
publishing {
    repositories {
        maven("https://maven.example.com/releases") {
            name = "myMaven"
            // To authenticate, create `myMavenUsername` and `myMavenPassword` properties in your Gradle home properties.
            // See https://stonecutter.kikugie.dev/wiki/tips/properties#defining-properties
            credentials(PasswordCredentials::class.java)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }

    publications {
        create<MavenPublication>("mavenJava") {
            groupId = "${property("mod.group")}.${property("mod.id")}"
            artifactId = property("mod.id") as String
            version = project.version

            from(components["java"])
        }
    }
}
 */
