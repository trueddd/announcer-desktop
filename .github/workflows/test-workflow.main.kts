@file:DependsOn("it.krzeminski:github-actions-kotlin-dsl:0.14.0")

import it.krzeminski.githubactions.actions.actions.CheckoutV3
import it.krzeminski.githubactions.actions.actions.SetupJavaV3
import it.krzeminski.githubactions.actions.actions.UploadArtifactV3
import it.krzeminski.githubactions.actions.gradle.GradleBuildActionV2
import it.krzeminski.githubactions.domain.RunnerType
import it.krzeminski.githubactions.domain.triggers.Push
import it.krzeminski.githubactions.domain.triggers.WorkflowDispatch
import it.krzeminski.githubactions.dsl.*
import java.nio.file.Paths

val testWorkflow = workflow(
    name = "test-build",
    on = listOf(
        Push(
            branches = listOf("develop"),
        ),
        WorkflowDispatch(),
    ),
    sourceFile = Paths.get(System.getProperty("user.dir"), "test.main.kts"),
    targetFile = Paths.get(System.getProperty("user.dir"), "test.yml"),
) {

    job(
        id = "windows-build",
        runsOn = RunnerType.WindowsLatest,
        _customArguments = mapOf(
            "permissions" to ObjectCustomValue(
                mapOf(
                    "contents" to "write",
                    "packages" to "write",
                )
            )
        ),
    ) {

        uses(
            name = "Fetch sources",
            action = CheckoutV3(
                fetchDepth = CheckoutV3.FetchDepth.Value(0),
            ),
        )

        uses(
            name = "Setup Java",
            action = SetupJavaV3(
                javaVersion = "16",
                distribution = SetupJavaV3.Distribution.Zulu,
                cache = SetupJavaV3.BuildPlatform.Gradle,
            ),
        )

        uses(
            name = "Build distributable",
            action = GradleBuildActionV2(
                arguments = "packageMsi",
            ),
            env = linkedMapOf(
                "ENCRYPTION_SECRET_KEY" to expr("secrets.ENCRYPTION_SECRET_KEY"),
                "STORAGE_BUCKET" to expr("secrets.STORAGE_BUCKET"),
            ),
        )

        uses(
            name = "Upload distributable to Github Packages",
            action = UploadArtifactV3(
                name = "installer",
                path = listOf("./build/compose/binaries/main/msi/announcer-*.msi"),
            ),
        )
    }
}
