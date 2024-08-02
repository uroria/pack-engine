import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin
import net.thebugmc.gradle.sonatypepublisher.PublishingType.*
import net.thebugmc.gradle.sonatypepublisher.SonatypeCentralPortalPublisherPlugin
import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    `java-library`
    `maven-publish`
    alias(libs.plugins.shadow)
    signing
    alias(libs.plugins.sonatypeCentralPortalPublisher)
}

group = "com.uroria"
val channel = System.getenv("PROJECT_CHANNEL") ?: "dev" // dev (local), stable, latest

subprojects {
    apply<JavaLibraryPlugin>()
    apply<SonatypeCentralPortalPublisherPlugin>()
    apply<ShadowPlugin>()
    apply<SigningPlugin>()

    java {
        withSourcesJar()
        withJavadocJar()
    }

    repositories {
        mavenCentral()
    }

    version = System.getenv("PROJECT_VERSION") ?: "0.0.0"
    group = "com.uroria.$channel"

    java.toolchain.languageVersion = JavaLanguageVersion.of(21)

    tasks {
        withType<Test> {
            useJUnitPlatform()

            minHeapSize = "256m"
            maxHeapSize = "512m"
        }

        withType<JavaCompile> {
            options.encoding = "UTF-8"
        }

        withType<Javadoc> {
            (options as? StandardJavadocDocletOptions)?.apply {
                encoding = "UTF-8"

                addBooleanOption("html5", true)
                addStringOption("-release", "21")
                addStringOption("Xdoclint:-missing", "-quiet")
            }
        }

        withType<Jar> {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }

        processResources {
            from(sourceSets.main.get().resources.srcDirs()) {
                filter<ReplaceTokens>("tokens" to mapOf("version" to project.version.toString()))
                filter<ReplaceTokens>("tokens" to mapOf("description" to project.description.toString()))

                duplicatesStrategy = DuplicatesStrategy.INCLUDE
            }
        }
    }

    centralPortal {
        username = System.getenv("SONATYPE_USERNAME")
        password = System.getenv("SONATYPE_PASSWORD")

        publishingType = if (channel == "latest") AUTOMATIC else USER_MANAGED

        name = project.name

        pom {
            name = project.name
            url = "https://github.com/uroria/pack-engine"
            description = project.description

            licenses {
                license {
                    name = "Apache 2.0"
                    url = "https://github.com/uroria/pack-engine/blob/main/LICENSE"
                }
            }

            developers {
                developer {
                    id = "julian-siebert"
                    name = "Julian Siebert"
                    organization = "Uroria"
                    organizationUrl = "https://github.com/uroria"
                    email = "mail@julian-siebert.de"
                }
            }
            scm {
                connection = "scm:git:git://github.com/uroria/pack-engine.git"
                developerConnection = "scm:git:git@github.com:uroria/pack-engine.git"
                url = "https://github.com/uroria/pack-engine"
                tag = "HEAD"
            }

            ciManagement {
                system = "GitHub Actions"
                url = "https://github.com/uroria/pack-engine/actions"
            }
        }
    }

    signing {
        isRequired = System.getenv("CI") != null

        val privateKey = System.getenv("GPG_PRIVATE_KEY")
        val passphrase = System.getenv("GPG_PASSPHRASE")
        useInMemoryPgpKeys(privateKey, passphrase)

        sign(publishing.publications)
    }
}