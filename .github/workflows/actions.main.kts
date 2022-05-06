@file:DependsOn("it.krzeminski:github-actions-kotlin-dsl:0.14.0")

import it.krzeminski.githubactions.actions.Action
import it.krzeminski.githubactions.actions.ActionWithOutputs

class GithubGetPreviousTagV1 : ActionWithOutputs<GithubGetPreviousTagV1.Outputs>(
    actionOwner = "WyriHaximus",
    actionName = "github-action-get-previous-tag",
    actionVersion = "v1",
) {

    class Outputs(stepId: String) {
        val tag = "steps.$stepId.outputs.tag"
    }

    override fun buildOutputObject(stepId: String) = Outputs(stepId)

    override fun toYamlArguments() = linkedMapOf<String, String>()
}

class GoogleStorageUploadV0(
    private val path: String,
    private val glob: String,
    private val destination: String,
    private val parent: Boolean = false,
) : Action(
    actionOwner = "google-github-actions",
    actionName = "upload-cloud-storage",
    actionVersion = "v0",
) {

    override fun toYamlArguments() = linkedMapOf(
        "path" to path,
        "glob" to glob,
        "destination" to destination,
        "parent" to "$parent",
    )
}

class CreateGithubReleaseV1(
    private val files: List<String>,
    private val draft: Boolean = false,
    private val tagName: String,
    private val name: String = "v$tagName",
) : Action(
    actionOwner = "softprops",
    actionName = "action-gh-release",
    actionVersion = "v1",
) {

    override fun toYamlArguments() = linkedMapOf(
        "files" to files.joinToString(","),
        "draft" to "$draft",
        "tag_name" to tagName,
        "name" to name,
    )
}
