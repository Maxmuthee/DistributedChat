pluginManagement {
    repositories {
        google()          // ✅ Required for Google plugins like google-services
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()          // ✅ Also needed here
        mavenCentral()
    }
}

rootProject.name = "VybzChat"
include(":app")
