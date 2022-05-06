@file:DependsOn("it.krzeminski:github-actions-kotlin-dsl:0.14.0")
@file:Import("release-workflow.main.kts")
@file:Import("test-workflow.main.kts")

import it.krzeminski.githubactions.yaml.writeToFile

listOf(
    testWorkflow,
    releaseWorkflow,
).forEach { it.writeToFile() }
