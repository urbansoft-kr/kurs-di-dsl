plugins {
  kotlin("jvm") version "2.3.21"
  id("com.vanniktech.maven.publish") version "0.36.0"
  id("org.jetbrains.dokka") version "2.2.0"
}

group = "kr.urbansoft.tools"

version = "1.0.0"

repositories { mavenCentral() }

dependencies { testImplementation(kotlin("test")) }

kotlin { jvmToolchain(25) }

tasks.test { useJUnitPlatform() }

mavenPublishing {
  publishToMavenCentral()

  signAllPublications()
  
  coordinates("kr.urbansoft.tools", "kurs-di-dsl", "1.0.0")

  pom {
    name.set("Kurs DI DSL")
    description.set(
        "A lightweight, zero-dependency Kotlin DI DSL for environments where heavy DI frameworks are unavailable."
    )
    inceptionYear.set("2026")
    url.set("https://github.com/urbansoft-kr/kurs-di-dsl")
    licenses {
      license {
        name.set("The MIT License")
        url.set("https://opensource.org/licenses/MIT")
      }
    }
    developers {
      developer {
        id.set("urbansoft")
        name.set("urbansoft")
        url.set("https://github.com/urbansoft-kr")
      }
    }
    scm {
      url.set("https://github.com/urbansoft-kr/kurs-di-dsl")
      connection.set("scm:git:git://github.com/urbansoft-kr/kurs-di-dsl.git")
      developerConnection.set("scm:git:ssh://git@github.com/urbansoft-kr/kurs-di-dsl.git")
    }
  }
}
