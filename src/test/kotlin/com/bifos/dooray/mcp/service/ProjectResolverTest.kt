package com.bifos.dooray.mcp.service

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.exception.ToolException
import com.bifos.dooray.mcp.types.DoorayApiHeader
import com.bifos.dooray.mcp.types.Project
import com.bifos.dooray.mcp.types.ProjectListResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertContains

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProjectResolverTest {

    private lateinit var mockDoorayClient: DoorayClient
    private lateinit var projectResolver: ProjectResolver

    private val successHeader = DoorayApiHeader(isSuccessful = true, resultCode = 0, resultMessage = "success")

    private val projects = listOf(
        Project(id = "1234567890", code = "backend-team", description = "Backend Team", state = "active"),
        Project(id = "9876543210", code = "Frontend", description = "Frontend Team", state = "active")
    )

    @BeforeEach
    fun setUp() {
        mockDoorayClient = mockk()
        projectResolver = ProjectResolver(mockDoorayClient)
    }

    @Test
    @DisplayName("숫자 ID를 그대로 반환한다 (캐시에 있을 때)")
    fun testResolveByNumericIdInCache() = runTest {
        // given: pre-populate cache
        projectResolver.updateCache(projects)

        // when
        val result = projectResolver.resolveProjectId("1234567890")

        // then: returns same ID without API call
        assertEquals("1234567890", result)
        coVerify(exactly = 0) { mockDoorayClient.getProjects(any(), any(), any(), any(), any()) }
    }

    @Test
    @DisplayName("프로젝트 코드로 ID를 조회한다")
    fun testResolveByProjectCode() = runTest {
        // given
        projectResolver.updateCache(projects)

        // when
        val result = projectResolver.resolveProjectId("backend-team")

        // then
        assertEquals("1234567890", result)
    }

    @Test
    @DisplayName("프로젝트 코드 대소문자 구분 없이 조회한다")
    fun testResolveByProjectCodeCaseInsensitive() = runTest {
        // given
        projectResolver.updateCache(projects)

        // when - use different case
        val result = projectResolver.resolveProjectId("BACKEND-TEAM")

        // then
        assertEquals("1234567890", result)
    }

    @Test
    @DisplayName("프로젝트 코드 대소문자 구분 없이 조회한다 - mixed case stored code")
    fun testResolveByProjectCodeStoredMixedCase() = runTest {
        // given: "Frontend" is stored with capital F
        projectResolver.updateCache(projects)

        // when
        val result = projectResolver.resolveProjectId("frontend")

        // then
        assertEquals("9876543210", result)
    }

    @Test
    @DisplayName("캐시 미스 시 API를 호출하여 갱신한 후 ID를 반환한다")
    fun testCacheRefreshOnMiss() = runTest {
        // given: empty cache, API returns projects
        coEvery { mockDoorayClient.getProjects(page = 0, size = 100, any(), any(), any()) } returns
            ProjectListResponse(header = successHeader, result = projects, totalCount = projects.size)

        // when
        val result = projectResolver.resolveProjectId("backend-team")

        // then
        assertEquals("1234567890", result)
        coVerify(exactly = 1) { mockDoorayClient.getProjects(page = 0, size = 100, any(), any(), any()) }
    }

    @Test
    @DisplayName("존재하지 않는 코드는 refresh 후 ToolException을 던진다")
    fun testUnknownCodeThrowsWithHelpfulMessage() = runTest {
        // given
        coEvery { mockDoorayClient.getProjects(page = 0, size = 100, any(), any(), any()) } returns
            ProjectListResponse(header = successHeader, result = projects, totalCount = projects.size)

        // when / then
        val ex = assertFailsWith<ToolException> {
            projectResolver.resolveProjectId("nonexistent-project")
        }

        assertEquals("PROJECT_NOT_FOUND", ex.code)
        assertContains(ex.message!!, "nonexistent-project")
        assertContains(ex.message!!, "backend-team")
    }

    @Test
    @DisplayName("두 번째 호출에서는 캐시를 사용한다 (API 1회만 호출)")
    fun testCacheIsUsedOnSecondCall() = runTest {
        // given
        coEvery { mockDoorayClient.getProjects(page = 0, size = 100, any(), any(), any()) } returns
            ProjectListResponse(header = successHeader, result = projects, totalCount = projects.size)

        // when: call twice
        projectResolver.resolveProjectId("backend-team")
        projectResolver.resolveProjectId("backend-team")

        // then: API called only once
        coVerify(exactly = 1) { mockDoorayClient.getProjects(any(), any(), any(), any(), any()) }
    }

    @Test
    @DisplayName("updateCache 후 추가된 프로젝트를 코드로 조회할 수 있다")
    fun testUpdateCacheAddsProjects() = runTest {
        // given
        val newProject = Project(id = "1111111111", code = "new-project")
        projectResolver.updateCache(listOf(newProject))

        // when
        val result = projectResolver.resolveProjectId("new-project")

        // then
        assertEquals("1111111111", result)
        coVerify(exactly = 0) { mockDoorayClient.getProjects(any(), any(), any(), any(), any()) }
    }
}
