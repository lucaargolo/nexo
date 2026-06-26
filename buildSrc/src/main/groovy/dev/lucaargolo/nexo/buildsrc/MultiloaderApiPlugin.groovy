package dev.lucaargolo.nexo.buildsrc

import net.ltgt.gradle.errorprone.CheckSeverity
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.jvm.toolchain.JavaLanguageVersion

class MultiloaderApiPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.pluginManager.apply('java-library')
        project.pluginManager.apply('maven-publish')
        project.pluginManager.apply('net.ltgt.errorprone')

        def modId       = project.property('mod_id') as String
        def mcVersion   = project.property('minecraft_version') as String
        def modName     = project.property('mod_name') as String
        def modAuthor   = project.property('mod_author') as String
        def javaVersion = project.property('java_version') as int

        project.base.archivesName = "${modId}-${project.name}-${mcVersion}"

        project.java {
            toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
            withSourcesJar()
            withJavadocJar()
        }

        project.repositories {
            mavenCentral()
        }

        project.dependencies {
            compileOnly 'org.jetbrains:annotations:24.1.0'
            errorprone 'com.google.errorprone:error_prone_core:2.36.0'
            errorprone 'com.uber.nullaway:nullaway:0.12.7'
        }


        // Declare capabilities on the outgoing configurations
        def publishing = project.extensions.getByType(PublishingExtension)
        ['apiElements', 'runtimeElements', 'sourcesElements', 'javadocElements'].each { variant ->
            project.configurations."$variant".outgoing {
                capability("${project.group}:${project.name}:${project.version}")
                capability("${project.group}:${project.base.archivesName.get()}:${project.version}")
                capability("${project.group}:${modId}-${project.name}-${mcVersion}:${project.version}")
            }
            publishing.publications.configureEach {
                suppressPomMetadataWarningsFor(variant)
            }
        }

        project.tasks.named('sourcesJar').configure {
            from(project.rootProject.file('LICENSE')) {
                rename { "${it}_${modName}" }
            }
        }

        project.tasks.named('jar').configure {
            from(project.rootProject.file('LICENSE')) {
                rename { "${it}_${modName}" }
            }
            manifest {
                attributes([
                    'Specification-Title'    : modName,
                    'Specification-Vendor'   : modAuthor,
                    'Specification-Version'  : project.jar.archiveVersion,
                    'Implementation-Title'   : project.name,
                    'Implementation-Version' : project.jar.archiveVersion,
                    'Implementation-Vendor'  : modAuthor
                ])
            }
        }

        project.configurations {
            apiJava {
                canBeResolved = false
                canBeConsumed = true
            }
            apiResources {
                canBeResolved = false
                canBeConsumed = true
            }
        }

        project.artifacts {
            apiJava project.sourceSets.main.java.sourceDirectories.singleFile
            apiResources project.sourceSets.main.resources.sourceDirectories.singleFile
        }

        publishing.publications {
            register('mavenJava', MavenPublication) {
                artifactId = project.base.archivesName.get()
                from project.components.java
            }
        }
        publishing.repositories {
            maven {
                url = System.getenv('local_maven_url')
            }
        }

        // Error Prone NullAway configuration
        project.tasks.withType(org.gradle.api.tasks.compile.JavaCompile).configureEach {
            options.compilerArgs << '-Xlint:-deprecation' << '-Xlint:-unchecked'
            options.errorprone {
                check('NullAway', CheckSeverity.ERROR)
                option('NullAway:AnnotatedPackages', 'dev.lucaargolo.nexo')
            }
        }

        // @Nullable/@NotNull annotation presence checker (ASM bytecode scan)
        project.tasks.named('compileJava').configure {
            doLast {
                def dir = destinationDirectory.get().asFile
                if (!dir.exists()) return
                def errors = NullAwayScanner.scan(dir)
                if (!errors.isEmpty())
                    throw new GradleException(
                        "Missing @Nullable/@NotNull annotations:\n" + errors.join('\n'))
            }
        }
        project.tasks.withType(org.gradle.api.tasks.javadoc.Javadoc).configureEach {
            options.addStringOption('Xdoclint:none', '-quiet')
        }

    }
}
