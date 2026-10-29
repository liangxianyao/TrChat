taboolib {
    description {
        name(rootProject.name)
        desc("Minecraft 聊天管理插件")
        links {
            name("homepage").url("https://trchat.trixey.cc/")
        }
        contributors {
            name("Arasple")
            name("ItsFlicker")
        }
        dependencies {
            name("PlaceholderAPI").with("bukkit")
            name("LuckPerms").with("bukkit").optional(true)
            name("DiscordSRV").with("bukkit").optional(true)
            name("EcoEnchants").with("bukkit").optional(true)
            name("ItemsAdder").with("bukkit").optional(true)
            name("Nova").with("bukkit").optional(true)
            name("SayanVanish").with("bukkit").optional(true)
            name("Multiverse-Core").with("bukkit").loadafter(true)
            name("Geyser-Spigot").with("bukkit").loadafter(true)
        }
//        version {
//            isSkipKotlin = true
//            isSkipKotlinRelocate = true
//        }
    }
    relocate("com.eatthepath.uuid.", "${rootProject.group}.library.uuid.")
//    relocate("com.electronwill.nightconfig", "com.electronwill.nightconfig_3_6_7")
}

dependencies {
    taboo("com.eatthepath:fast-uuid:0.2.0")
}

tasks {
    val paperModules = listOf(
        ":project:common",
        ":project:module-adventure",
        ":project:module-chat",
        ":project:module-compat",
        ":project:module-nms",
        ":project:runtime-bukkit"
    )
    jar {
        // 构件名
        archiveBaseName.set(rootProject.name)
        // 打包子项目源代码
        paperModules.forEach { from(project(it).sourceSets["main"].output) }
    }
    sourcesJar {
        // 构件名
        archiveBaseName.set(rootProject.name)
        // 打包子项目源代码
        paperModules.forEach { from(project(it).sourceSets["main"].allSource) }
    }
}
