package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.types.*
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.TextContent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertContains
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class McpToolsUnitTest {

    @Test
    @DisplayName("위키 목록 조회 도구 - 성공 케이스")
    fun testGetWikisHandlerSuccess() = runTest {
        // given
        val mockDoorayClient = mockk<DoorayClient>()

        val mockWikis =
            listOf(
                Wiki(
                    id = "wiki1",
                    project = WikiProject(id = "project1"),
                    name = "테스트 위키",
                    type = "wiki",
                    scope = "private",
                    home = WikiHome(pageId = "home1")
                )
            )
        val mockResponse =
            WikiListResponse(
                header =
                    DoorayApiHeader(
                        isSuccessful = true,
                        resultCode = 0,
                        resultMessage = "success"
                    ),
                result = mockWikis
            )

        coEvery { mockDoorayClient.getWikis(any(), any()) } returns mockResponse

        val mockRequest = mockk<CallToolRequest>()
        every { mockRequest.arguments } returns
                buildJsonObject {
                    put("page", 0)
                    put("size", 10)
                }

        // when
        val handler = getWikisHandler(mockDoorayClient)
        val result = handler(mockRequest)

        // then
        assertTrue(result.content.isNotEmpty())
        val content = result.content.first() as TextContent
        val responseText = content.text ?: ""
        assertContains(responseText, "\"success\": true")
        assertContains(responseText, "테스트 위키")
    }

    @Test
    @DisplayName("위키 목록 조회 도구 - API 에러 케이스")
    fun testGetWikisHandlerApiError() = runTest {
        // given
        val mockDoorayClient = mockk<DoorayClient>()

        val mockResponse =
            WikiListResponse(
                header =
                    DoorayApiHeader(
                        isSuccessful = false,
                        resultCode = 400,
                        resultMessage = "Bad Request"
                    ),
                result = emptyList()
            )

        coEvery { mockDoorayClient.getWikis(any(), any()) } returns mockResponse

        val mockRequest = mockk<CallToolRequest>()
        every { mockRequest.arguments } returns buildJsonObject {}

        // when
        val handler = getWikisHandler(mockDoorayClient)
        val result = handler(mockRequest)

        // then
        assertTrue(result.content.isNotEmpty())
        val content = result.content.first() as TextContent
        val responseText = content.text ?: ""
        assertContains(responseText, "\"isError\": true")
        assertContains(responseText, "Bad Request")
    }

    @Test
    @DisplayName("위키 페이지 목록 조회 도구 - 성공 케이스")
    fun testGetWikiPagesHandlerSuccess() = runTest {
        // given
        val mockDoorayClient = mockk<DoorayClient>()

        val mockPages =
            listOf(
                WikiPage(
                    id = "page1",
                    wikiId = "wiki1",
                    version = 1,
                    root = false,
                    creator =
                        Creator(
                            type = "member",
                            member = Member(organizationMemberId = "member1")
                        ),
                    subject = "테스트 페이지"
                )
            )
        val mockResponse =
            WikiPagesResponse(
                header =
                    DoorayApiHeader(
                        isSuccessful = true,
                        resultCode = 0,
                        resultMessage = "success"
                    ),
                result = mockPages
            )

        coEvery { mockDoorayClient.getWikiPages(any<String>()) } returns mockResponse

        val mockRequest = mockk<CallToolRequest>()
        every { mockRequest.arguments } returns buildJsonObject { put("project_id", "project1") }

        // when
        val handler = getWikiPagesHandler(mockDoorayClient)
        val result = handler(mockRequest)

        // then
        assertTrue(result.content.isNotEmpty())
        val content = result.content.first() as TextContent
        val responseText = content.text ?: ""
        assertContains(responseText, "\"success\": true")
        assertContains(responseText, "테스트 페이지")
    }

    @Test
    @DisplayName("위키 페이지 목록 조회 도구 - project_id 누락 에러")
    fun testGetWikiPagesHandlerMissingProjectId() = runTest {
        // given
        val mockDoorayClient = mockk<DoorayClient>()

        val mockRequest = mockk<CallToolRequest>()
        every { mockRequest.arguments } returns buildJsonObject {} // project_id 누락

        // when
        val handler = getWikiPagesHandler(mockDoorayClient)
        val result = handler(mockRequest)

        // then
        assertTrue(result.content.isNotEmpty())
        val content = result.content.first() as TextContent
        val responseText = content.text ?: ""
        assertContains(responseText, "\"isError\": true")
        assertContains(responseText, "MISSING_PROJECT_ID")
    }

    @Test
    @DisplayName("프로젝트 목록 조회 도구 - 성공 케이스")
    fun testGetProjectsHandlerSuccess() = runTest {
        // given
        val mockDoorayClient = mockk<DoorayClient>()

        val mockProjects =
            listOf(
                Project(
                    id = "project1",
                    code = "TEST",
                    description = "테스트용 프로젝트입니다",
                    state = "active",
                    scope = "private",
                    type = "project"
                )
            )
        val mockResponse =
            ProjectListResponse(
                header =
                    DoorayApiHeader(
                        isSuccessful = true,
                        resultCode = 0,
                        resultMessage = "success"
                    ),
                result = mockProjects,
                totalCount = 1
            )

        coEvery { mockDoorayClient.getProjects(any(), any(), any(), any(), any()) } returns
                mockResponse

        val mockRequest = mockk<CallToolRequest>()
        every { mockRequest.arguments } returns
                buildJsonObject {
                    put("page", 0)
                    put("size", 20)
                }

        // when
        val handler = getProjectsHandler(mockDoorayClient)
        val result = handler(mockRequest)

        // then
        assertTrue(result.content.isNotEmpty())
        val content = result.content.first() as TextContent
        val responseText = content.text ?: ""
        assertContains(responseText, "\"success\": true")
        assertContains(responseText, "TEST")
    }

    @Test
    @DisplayName("프로젝트 업무 목록 조회 도구 - project_id 누락 에러")
    fun testGetProjectPostsHandlerMissingProjectId() = runTest {
        // given
        val mockDoorayClient = mockk<DoorayClient>()

        val mockRequest = mockk<CallToolRequest>()
        every { mockRequest.arguments } returns buildJsonObject {} // project_id 누락

        // when
        val handler = getProjectPostsHandler(mockDoorayClient)
        val result = handler(mockRequest)

        // then
        assertTrue(result.content.isNotEmpty())
        val content = result.content.first() as TextContent
        val responseText = content.text ?: ""
        assertContains(responseText, "\"isError\": true")
        assertContains(responseText, "MISSING_PROJECT_ID")
    }

    @Test
    @DisplayName("위키 페이지 생성 도구 - 성공 케이스")
    fun testCreateWikiPageHandlerSuccess() = runTest {
        // given
        val mockDoorayClient = mockk<DoorayClient>()

        val mockResponse =
            CreateWikiPageResponse(
                header =
                    DoorayApiHeader(
                        isSuccessful = true,
                        resultCode = 0,
                        resultMessage = "success"
                    ),
                result =
                    CreateWikiPageResult(
                        id = "page1",
                        wikiId = "wiki1",
                        parentPageId = "parent1",
                        version = 1
                    )
            )

        coEvery { mockDoorayClient.createWikiPage(any(), any()) } returns mockResponse

        val mockRequest = mockk<CallToolRequest>()
        every { mockRequest.arguments } returns
                buildJsonObject {
                    put("wiki_id", "wiki1")
                    put("subject", "새 위키 페이지")
                    put("body", "새 위키 페이지 내용")
                    put("parent_page_id", "parent1")
                }

        // when
        val handler = createWikiPageHandler(mockDoorayClient)
        val result = handler(mockRequest)

        // then
        assertTrue(result.content.isNotEmpty())
        val content = result.content.first() as TextContent
        val responseText = content.text ?: ""
        assertContains(responseText, "\"success\": true")
        assertContains(responseText, "성공적으로 생성")
    }

    @Test
    @DisplayName("위키 페이지 생성 도구 - wiki_id 누락 에러")
    fun testCreateWikiPageHandlerMissingWikiId() = runTest {
        // given
        val mockDoorayClient = mockk<DoorayClient>()

        val mockRequest = mockk<CallToolRequest>()
        every { mockRequest.arguments } returns
                buildJsonObject {
                    put("subject", "새 위키 페이지")
                    put("body", "새 위키 페이지 내용")
                    put("parent_page_id", "parent1")
                    // wiki_id 누락
                }

        // when
        val handler = createWikiPageHandler(mockDoorayClient)
        val result = handler(mockRequest)

        // then
        assertTrue(result.content.isNotEmpty())
        val content = result.content.first() as TextContent
        val responseText = content.text ?: ""
        assertContains(responseText, "\"isError\": true")
        assertContains(responseText, "MISSING_WIKI_ID")
    }

    @Test
    @DisplayName("업무 생성 도구 - 성공 케이스")
    fun testCreateProjectPostHandlerSuccess() = runTest {
        // given
        val mockDoorayClient = mockk<DoorayClient>()

        val mockResponse =
            CreatePostApiResponse(
                header =
                    DoorayApiHeader(
                        isSuccessful = true,
                        resultCode = 0,
                        resultMessage = "success"
                    ),
                result = CreatePostResponse(id = "post1")
            )

        coEvery { mockDoorayClient.createPost(any(), any()) } returns mockResponse

        val mockRequest = mockk<CallToolRequest>()
        every { mockRequest.arguments } returns
                buildJsonObject {
                    put("project_id", "project1")
                    put("subject", "새 업무")
                    put("body", "새 업무 내용")
                    putJsonArray("to_member_ids") {
                        add("member1")
                        add("member2")
                    }
                }

        // when
        val handler = createProjectPostHandler(mockDoorayClient)
        val result = handler(mockRequest)

        // then
        assertTrue(result.content.isNotEmpty())
        val content = result.content.first() as TextContent
        val responseText = content.text ?: ""
        assertContains(responseText, "\"success\": true")
        assertContains(responseText, "성공적으로 생성")
    }

    @Test
    @DisplayName("업무 생성 도구 - to_member_ids 누락 에러")
    fun testCreateProjectPostHandlerMissingToMemberIds() = runTest {
        // given
        val mockDoorayClient = mockk<DoorayClient>()

        val mockRequest = mockk<CallToolRequest>()
        every { mockRequest.arguments } returns
                buildJsonObject {
                    put("project_id", "project1")
                    put("subject", "새 업무")
                    put("body", "새 업무 내용")
                    // to_member_ids 누락
                }

        // when
        val handler = createProjectPostHandler(mockDoorayClient)
        val result = handler(mockRequest)

        // then
        assertTrue(result.content.isNotEmpty())
        val content = result.content.first() as TextContent
        val responseText = content.text ?: ""
        assertContains(responseText, "\"isError\": true")
        assertContains(responseText, "MISSING_TO_MEMBER_IDS")
    }

    @Test
    @DisplayName("위키 페이지 수정 도구 - 수정할 내용 없음 에러")
    fun testUpdateWikiPageHandlerNoUpdateContent() = runTest {
        // given
        val mockDoorayClient = mockk<DoorayClient>()

        val mockRequest = mockk<CallToolRequest>()
        every { mockRequest.arguments } returns
                buildJsonObject {
                    put("wiki_id", "wiki1")
                    put("page_id", "page1")
                    // subject, body, referrer_member_ids 모두 누락
                }

        // when
        val handler = updateWikiPageHandler(mockDoorayClient)
        val result = handler(mockRequest)

        // then
        assertTrue(result.content.isNotEmpty())
        val content = result.content.first() as TextContent
        val responseText = content.text ?: ""
        assertContains(responseText, "\"isError\": true")
        assertContains(responseText, "NO_UPDATE_CONTENT")
    }
}
