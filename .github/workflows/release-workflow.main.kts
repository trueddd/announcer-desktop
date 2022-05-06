@file:DependsOn("it.krzeminski:github-actions-kotlin-dsl:0.14.0")
@file:Import("actions.main.kts")

import it.krzeminski.githubactions.actions.actions.CheckoutV3
import it.krzeminski.githubactions.actions.actions.SetupJavaV3
import it.krzeminski.githubactions.actions.googlegithubactions.AuthV0
import it.krzeminski.githubactions.actions.gradle.GradleBuildActionV2
import it.krzeminski.githubactions.domain.RunnerType
import it.krzeminski.githubactions.domain.triggers.Push
import it.krzeminski.githubactions.dsl.ObjectCustomValue
import it.krzeminski.githubactions.dsl.expr
import it.krzeminski.githubactions.dsl.workflow
import java.nio.file.Paths

val releaseWorkflow = workflow(
    name = "release-build",
    on = listOf(
        Push(
            branches = listOf("master"),
        ),
    ),
    sourceFile = Paths.get(System.getProperty("user.dir"), "release.main.kts"),
    targetFile = Paths.get(System.getProperty("user.dir"), "release.yml"),
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
            name = "Authenticate to Google Cloud",
            action = AuthV0(
                credentialsJson = expr("secrets.FIREBASE_AUTH"),
            ),
        )

        uses(
            name = "Upload distributable to Google Storage",
            action = GoogleStorageUploadV0(
                path = "./build/compose/binaries/main/msi",
                glob = "announcer-*.msi",
                destination = expr("secrets.STORAGE_BUCKET"),
            )
        )

        val previousTagStep = uses(
            name = "Get previous tag",
            action = GithubGetPreviousTagV1(),
        )

        uses(
            name = "Create Github release",
            action = CreateGithubReleaseV1(
                files = listOf("./build/compose/binaries/main/msi/announcer-*.msi"),
                draft = true,
                tagName = expr(previousTagStep.outputs.tag),
            )
        )
    }
}
