package com.bifos.dooray.mcp

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DockerPublishingPolicyTest {
    @Test
    fun `수동 시험 게시에서는 latest 태그를 만들지 않는다`() {
        val result =
            runPolicy(
                "PROJECT_VERSION" to "0.5.0-alpha.1",
                "EVENT_NAME" to "workflow_dispatch",
                "INPUT_VERSION" to "0.5.0-alpha.1",
                "PUSH_REQUESTED" to "true",
            )

        assertEquals(0, result.exitCode, result.stderr)
        assertEquals("0.5.0-alpha.1", result.outputs["VERSION"])
        assertEquals("true", result.outputs["IS_PRERELEASE"])
        assertEquals("false", result.outputs["PUSH_LATEST"])
    }

    @Test
    fun `수동 안정 버전 게시에서도 latest 태그를 만들지 않는다`() {
        val result =
            runPolicy(
                "PROJECT_VERSION" to "0.5.0",
                "EVENT_NAME" to "workflow_dispatch",
                "INPUT_VERSION" to "0.5.0",
                "PUSH_REQUESTED" to "true",
            )

        assertEquals(0, result.exitCode, result.stderr)
        assertEquals("false", result.outputs["IS_PRERELEASE"])
        assertEquals("false", result.outputs["PUSH_LATEST"])
    }

    @Test
    fun `안정 GitHub Release만 latest 태그를 만든다`() {
        val result =
            runPolicy(
                "PROJECT_VERSION" to "0.5.0",
                "EVENT_NAME" to "release",
                "RELEASE_TAG" to "v0.5.0",
                "RELEASE_PRERELEASE" to "false",
                "PUSH_REQUESTED" to "true",
            )

        assertEquals(0, result.exitCode, result.stderr)
        assertEquals("0.5.0", result.outputs["VERSION"])
        assertEquals("false", result.outputs["IS_PRERELEASE"])
        assertEquals("true", result.outputs["PUSH_LATEST"])
    }

    @Test
    fun `prerelease GitHub Release는 latest 태그를 만들지 않는다`() {
        val result =
            runPolicy(
                "PROJECT_VERSION" to "0.5.0-alpha.1",
                "EVENT_NAME" to "release",
                "RELEASE_TAG" to "v0.5.0-alpha.1",
                "RELEASE_PRERELEASE" to "true",
                "PUSH_REQUESTED" to "true",
            )

        assertEquals(0, result.exitCode, result.stderr)
        assertEquals("true", result.outputs["IS_PRERELEASE"])
        assertEquals("false", result.outputs["PUSH_LATEST"])
    }

    @Test
    fun `게시 버전이 프로젝트 버전과 다르면 실패한다`() {
        val result =
            runPolicy(
                "PROJECT_VERSION" to "0.5.0-alpha.1",
                "EVENT_NAME" to "workflow_dispatch",
                "INPUT_VERSION" to "0.5.0-alpha.2",
                "PUSH_REQUESTED" to "true",
            )

        assertEquals(1, result.exitCode)
        assertTrue(result.stderr.contains("project.version"))
    }

    @Test
    fun `Docker 워크플로는 정책 스크립트의 latest 판정만 사용한다`() {
        val dockerWorkflow = Files.readString(Path.of(".github/workflows/docker.yml"))
        val mainWorkflow = Files.readString(Path.of(".github/workflows/main.yml"))

        assertTrue(dockerWorkflow.contains("run: scripts/docker-tag-policy.sh"))
        assertTrue(mainWorkflow.contains("run: scripts/docker-tag-policy.sh"))
        assertTrue(
            dockerWorkflow.contains("type=raw,value=latest,enable=\${{ steps.version.outputs.PUSH_LATEST == 'true' }}"),
        )
        assertTrue(
            mainWorkflow.contains("type=raw,value=latest,enable=\${{ steps.version.outputs.PUSH_LATEST == 'true' }}"),
        )
        assertFalse(dockerWorkflow.contains("workflow_dispatch' && github.event.inputs.push == 'true'"))
    }

    private fun runPolicy(vararg env: Pair<String, String>): PolicyResult {
        val outputFile = Files.createTempFile("docker-tag-policy", ".out")
        val process =
            ProcessBuilder("bash", "scripts/docker-tag-policy.sh")
                .directory(Path.of(".").toFile())
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .apply {
                    environment().put("GITHUB_OUTPUT", outputFile.toString())
                    env.forEach { (key, value) -> environment()[key] = value }
                }
                .start()

        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        val outputs =
            if (Files.exists(outputFile)) {
                Files.readAllLines(outputFile)
                    .filter { it.contains('=') }
                    .associate { it.substringBefore('=') to it.substringAfter('=') }
            } else {
                emptyMap()
            }
        Files.deleteIfExists(outputFile)

        return PolicyResult(exitCode, outputs, stdout, stderr)
    }

    private data class PolicyResult(
        val exitCode: Int,
        val outputs: Map<String, String>,
        val stdout: String,
        val stderr: String,
    )
}
