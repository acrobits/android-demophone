@file:Suppress("UnstableApiUsage")

rootProject.name = "Acrobits Demo Phone"

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    // Load license information from gradle.properties
    @Suppress("LocalVariableName")
    val acrobits_saas_package: String by settings
    @Suppress("LocalVariableName")
    val acrobits_saas_key: String by settings

    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        // Acrobits Maven
        maven {
            url = uri("https://maven.acrobits.net/repository/maven-releases/")
            credentials(PasswordCredentials::class) {
                username = acrobits_saas_package
                password = acrobits_saas_key
            }
        }
    }
}

include(":DemoPhone")
