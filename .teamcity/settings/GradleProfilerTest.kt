import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.failureConditions.BuildFailureOnText
import jetbrains.buildServer.configs.kotlin.v2019_2.failureConditions.failOnText
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs
import jetbrains.buildServer.configs.kotlin.v2019_2.ui.add

open class GradleProfilerTest(os: Os, javaVersion: JavaVersion) : BuildType({
    gradleProfilerVcs()

    params {
        javaHome(os, javaVersion)
    }

    steps {
        gradle {
            tasks = "clean build"
            buildFile = ""
            gradleParams = "-s ${buildCacheConfigurations()} ${toolchainConfiguration(os)}"
            param("org.jfrog.artifactory.selectedDeployableServer.defaultModuleVersionConfiguration", "GLOBAL")
        }
    }

    triggers {
        vcs {
            branchFilter = """
                +:*
                -:pull/*
            """.trimIndent()
        }
    }

    features {
        commitStatusPublisher {
            vcsRootExtId = "${DslContext.settingsRoot.id}"
            publisher = github {
                githubUrl = "https://api.github.com"
                authType = personalToken {
                    token = "%github.bot-teamcity.token%"
                }
            }
        }
    }

    failureConditions {
        add {
            failOnText {
                conditionType = BuildFailureOnText.ConditionType.CONTAINS
                pattern = "%unmaskedFakeCredentials%"
                failureMessage = "This build might be leaking credentials"
                reverse = false
                stopBuildOnFailure = true
            }
        }
    }

    agentRequirement(os)
}) {
    constructor(os: Os, javaVersion: JavaVersion, init: GradleProfilerTest.() -> Unit) : this(os, javaVersion) {
        init()
    }
}

object LinuxJava8 : GradleProfilerTest(Os.linux, JavaVersion.ORACLE_JAVA_8, {
    name = "Linux - Java 8"
})

object LinuxJava11 : GradleProfilerTest(Os.linux, JavaVersion.OPENJDK_11, {
    name = "Linux - Java 11"
})

object MacOSJava8 : GradleProfilerTest(Os.macos, JavaVersion.ORACLE_JAVA_8, {
    name = "MacOS - Java 8"
})

object WindowsJava8 : GradleProfilerTest(Os.windows, JavaVersion.ORACLE_JAVA_8, {
    name = "Windows - Java 8"
})
