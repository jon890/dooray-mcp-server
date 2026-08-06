package com.bifos.dooray.mcp

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class DockerfileContractTest {
    private val dockerfile = Files.readString(Path.of("Dockerfile"))

    @Test
    fun `기본 이미지 버전은 Gradle 프로젝트 버전과 일치한다`() {
        val projectVersion =
            Files.readAllLines(Path.of("gradle.properties"))
                .first { it.startsWith("project.version=") }
                .substringAfter('=')
                .trim()
        val dockerVersion =
            Regex("(?m)^ARG VERSION=(.+)$")
                .find(dockerfile)
                ?.groupValues
                ?.get(1)
                ?.trim()

        assertEquals(projectVersion, dockerVersion)
    }

    @Test
    fun `런타임 이미지는 시험 태그와 무관하게 shadowJar 산출물을 복사한다`() {
        assertContains(
            dockerfile,
            "COPY --from=builder /app/build/libs/*-all.jar app.jar",
        )
    }
}
