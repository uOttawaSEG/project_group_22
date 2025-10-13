pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
<<<<<<< HEAD

    // ✅ Declare plugin + version here (Option A)
    plugins {
        id("com.google.gms.google-services") version "4.4.2"
    }
}

=======
}
>>>>>>> origin/main
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

<<<<<<< HEAD
rootProject.name = "SEG2105_Project_1_Tutor_Registration_Form"
=======
rootProject.name = "OTAMS_GROUP22"
>>>>>>> origin/main
include(":app")
