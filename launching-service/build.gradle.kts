plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.maven.publish)
}

val libraryVersion = providers.fileContents(rootProject.layout.projectDirectory.file("VERSION.txt"))
  .asText
  .map(String::trim)

android {
  namespace = "me.gorani.launchingservice"
  compileSdk = 36

  defaultConfig {
    minSdk = 23
    consumerProguardFiles("consumer-rules.pro")
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  testOptions {
    unitTests.isReturnDefaultValues = true
  }
}

dependencies {
  api(platform(libs.firebase.bom))
  api(libs.firebase.config)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.play.services)

  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
}

mavenPublishing {
  coordinates(
    groupId = "me.gorani",
    artifactId = "launching-service",
    version = libraryVersion.get(),
  )

  publishToMavenCentral()

  if (providers.environmentVariable("ORG_GRADLE_PROJECT_signingInMemoryKey").isPresent) {
    signAllPublications()
  }

  pom {
    name.set("LaunchingService Android")
    description.set("Firebase Remote Config based launch policy for Android applications.")
    inceptionYear.set("2026")
    url.set("https://github.com/swift-man/LaunchingService-Android")

    licenses {
      license {
        name.set("MIT License")
        url.set("https://opensource.org/licenses/MIT")
        distribution.set("repo")
      }
    }

    developers {
      developer {
        id.set("swift-man")
        name.set("swift-man")
        url.set("https://github.com/swift-man")
      }
    }

    scm {
      url.set("https://github.com/swift-man/LaunchingService-Android")
      connection.set("scm:git:git://github.com/swift-man/LaunchingService-Android.git")
      developerConnection.set("scm:git:ssh://git@github.com/swift-man/LaunchingService-Android.git")
    }
  }
}
