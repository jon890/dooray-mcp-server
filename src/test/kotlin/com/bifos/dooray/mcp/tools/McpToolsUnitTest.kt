package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.service.ProjectResolver
import com.bifos.dooray.mcp.types.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
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
        val result = handler(mockk<ClientConnection>(), mockRequest)

        // then
        assertTrue(result.content.isNotEmpty())
        val content = result.content.first() as TextContent
        val responseText = content.text
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
        val result = handler(mockk<ClientConnection>(), mockRequest)

        // then
        assertTrue(result.content.isNotEmpty())
        val content = result.content.first() as TextContent
        val responseText = content.text
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
        val result = handler(mockk<ClientConnection>(), mockRequest)

        // then
        assertTrue(result.content.isNotEmpty())
        val content = result.content.first() as TextContent
        val responseText = content.text
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
        val result = handler(mockk<ClientConnection>(), mockRequest)

        // then
        assertTrue(result.content.isNotEmpty())
        val content = result.content.first() as TextContent
        val responseText = content.text
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
        val mockProjectResolver = mockk<ProjectResolver>(relaxed = true)
        val handler = getProjectsHandler(mockDoorayClient, mockProjectResolver)
        val result = handler(mockk<ClientConnection>(), mockRequest)

        // then
        assertTrue(result.content.isNotEmpty())
        val content = result.content.first() as TextContent
        val responseText = content.text
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
        val mockProjectResolver = mockk<ProjectResolver>(relaxed = true)
        val handler = getProjectPostsHandler(mockDoorayClient, mockProjectResolver)
        val result = handler(mockk<ClientConnection>(), mockRequest)

        // then
        assertTrue(result.content.isNotEmpty())
        val content = result.content.first() as TextContent
        val responseText = content.text
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
        val result = handler(mockk<ClientConnection>(), mockRequest)

        // then
        assertTrue(result.content.isNotEmpty())
        val content = result.content.first() as TextContent
        val responseText = content.text
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
        val result = handler(mockk<ClientConnection>(), mockRequest)

        // then
        assertTrue(result.content.isNotEmpty())
        val content = result.content.first() as TextContent
        val responseText = content.text
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
        val mockProjectResolver = mockk<ProjectResolver>()
        coEvery { mockProjectResolver.resolveProjectId("project1") } returns "project1"
        val handler = createProjectPostHandler(mockDoorayClient, mockProjectResolver)
        val result = handler(mockk<ClientConnection>(), mockRequest)

        // then
        assertTrue(result.content.isNotEmpty())
        val content = result.content.first() as TextContent
        val responseText = content.text
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
        val mockProjectResolver = mockk<ProjectResolver>(relaxed = true)
        val handler = createProjectPostHandler(mockDoorayClient, mockProjectResolver)
        val result = handler(mockk<ClientConnection>(), mockRequest)

        // then
        assertTrue(result.content.isNotEmpty())
        val content = result.content.first() as TextContent
        val responseText = content.text
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
        val result = handler(mockk<ClientConnection>(), mockRequest)

        // then
        assertTrue(result.content.isNotEmpty())
        val content = result.content.first() as TextContent
        val responseText = content.text
        assertContains(responseText, "\"isError\": true")
        assertContains(responseText, "NO_UPDATE_CONTENT")
    }

    // === 댓글 관련 도구 테스트 ===

    @Test
    @DisplayName("업무 댓글 목록 조회 도구 - 성공 케이스")
    fun testGetPostCommentsHandlerSuccess() = runTest {
        // given
        val mockDoorayClient = mockk<DoorayClient>()

        val mockComments =
            listOf(
                PostComment(
                    id = "comment1",
                    post = PostInfo(id = "post1"),
                    type = "comment",
                    subtype = "general",
                    createdAt = "2025-01-25T10:00:00+09:00",
                    modifiedAt = null,
                    creator =
                        PostUser(
                            type = "member",
                            member = Member(organizationMemberId = "member1")
                        ),
                    mailUsers = null,
                    body =
                        PostCommentBody(
                            mimeType = "text/html",
                            content = "테스트 댓글입니다."
                        ),
                    files = null
                )
            )
        val mockResponse =
            PostCommentListResponse(
                header =
                    DoorayApiHeader(
                        isSuccessful = true,
                        resultCode = 0,
                        resultMessage = "success"
                    ),
                result = mockComments,
                totalCount = 1
            )

        coEvery { mockDoorayClient.getPostComments(any(), any(), any(), any(), any()) } returns
                mockResponse

        val mockRequest = mockk<CallToolRequest>()
        every { mockRequest.arguments } returns
                buildJsonObject {
                    put("project_id", "project1")
                    put("post_id", "post1")
                    put("page", 0)
                    put("size", 10)
                }

        // when
        val mockProjectResolver = mockk<ProjectResolver>()
        coEvery { mockProjectResolver.resolveProjectId("project1") } returns "project1"
        val handler = getPostCommentsHandler(mockDoorayClient, mockProjectResolver)
        val result = handler(mockk<ClientConnection>(), mockRequest)

        // then
        assertTrue(result.content.isNotEmpty())
        val content = result.content.first() as TextContent
        val responseText = content.text
        assertContains(responseText, "\"success\": true")
        assertContains(responseText, "\"comments\":")
        assertContains(responseText, "\"totalCount\": 1")
        assertContains(responseText, "\"currentPage\": 0")
        assertContains(responseText, "\"pageSize\": 10")
        assertContains(responseText, "테스트 댓글입니다")
    }

    @Test
    @DisplayName("업무 댓글 목록 조회 도구 - project_id 누락 에러")
    fun testGetPostCommentsHandlerMissingProjectId() = runTest {
        // given
        val mockDoorayClient = mockk<DoorayClient>()

        val mockRequest = mockk<CallToolRequest>()
        every { mockRequest.arguments } returns
                buildJsonObject {
                    put("post_id", "post1")
                    // project_id 누락
                }

        // when
        val mockProjectResolver1 = mockk<ProjectResolver>(relaxed = true)
        val handler = getPostCommentsHandler(mockDoorayClient, mockProjectResolver1)
        val result = handler(mockk<ClientConnection>(), mockRequest)

        // then
        assertTrue(result.content.isNotEmpty())
        val content = result.content.first() as TextContent
        val responseText = content.text
        assertContains(responseText, "\"isError\": true")
        assertContains(responseText, "MISSING_PROJECT_ID")
    }

    @Test
    @DisplayName("업무 댓글 목록 조회 도구 - post_id 누락 에러")
    fun testGetPostCommentsHandlerMissingPostId() = runTest {
        // given
        val mockDoorayClient = mockk<DoorayClient>()

        val mockRequest = mockk<CallToolRequest>()
        every { mockRequest.arguments } returns
                buildJsonObject {
                    put("project_id", "project1")
                    // post_id 누락
                }

        // when
        val mockProjectResolver2 = mockk<ProjectResolver>(relaxed = true)
        val handler = getPostCommentsHandler(mockDoorayClient, mockProjectResolver2)
        val result = handler(mockk<ClientConnection>(), mockRequest)

        // then
        assertTrue(result.content.isNotEmpty())
        val content = result.content.first() as TextContent
        val responseText = content.text
        assertContains(responseText, "\"isError\": true")
        assertContains(responseText, "MISSING_POST_ID")
    }

    @Test
    @DisplayName("업무 댓글 목록 조회 도구 - API 에러 케이스")
    fun testGetPostCommentsHandlerApiError() = runTest {
        // given
        val mockDoorayClient = mockk<DoorayClient>()

        val mockResponse =
            PostCommentListResponse(
                header =
                    DoorayApiHeader(
                        isSuccessful = false,
                        resultCode = 404,
                        resultMessage = "Post not found"
                    ),
                result = emptyList(),
                totalCount = 0
            )

        coEvery { mockDoorayClient.getPostComments(any(), any(), any(), any(), any()) } returns
                mockResponse

        val mockRequest = mockk<CallToolRequest>()
        every { mockRequest.arguments } returns
                buildJsonObject {
                    put("project_id", "project1")
                    put("post_id", "invalid_post_id")
                }

        // when
        val mockProjectResolver = mockk<ProjectResolver>()
        coEvery { mockProjectResolver.resolveProjectId("project1") } returns "project1"
        val handler = getPostCommentsHandler(mockDoorayClient, mockProjectResolver)
        val result = handler(mockk<ClientConnection>(), mockRequest)

        // then
        assertTrue(result.content.isNotEmpty())
        val content = result.content.first() as TextContent
        val responseText = content.text
        assertContains(responseText, "\"isError\": true")
        assertContains(responseText, "Post not found")
        assertContains(responseText, "DOORAY_API_404")
    }

    @Test
    @DisplayName("업무 댓글 생성 도구 - 성공 케이스")
    fun testCreatePostCommentHandlerSuccess() = runTest {
        // given
        val mockDoorayClient = mockk<DoorayClient>()

        val mockResponse =
            CreateCommentApiResponse(
                header =
                    DoorayApiHeader(
                        isSuccessful = true,
                        resultCode = 0,
                        resultMessage = "success"
                    ),
                result = CreateCommentResponse(id = "comment1")
            )

        coEvery { mockDoorayClient.createPostComment(any(), any(), any()) } returns mockResponse

        val mockRequest = mockk<CallToolRequest>()
        every { mockRequest.arguments } returns
                buildJsonObject {
                    put("project_id", "project1")
                    put("post_id", "post1")
                    put("content", "새 댓글 내용")
                }

        // when
        val mockProjectResolver = mockk<ProjectResolver>()
        coEvery { mockProjectResolver.resolveProjectId("project1") } returns "project1"
        val handler = createPostCommentHandler(mockDoorayClient, mockProjectResolver)
        val result = handler(mockk<ClientConnection>(), mockRequest)

        // then
        assertTrue(result.content.isNotEmpty())
        val content = result.content.first() as TextContent
        val responseText = content.text
        assertContains(responseText, "\"success\": true")
        assertContains(responseText, "성공적으로 생성")
    }

    @Test
    @DisplayName("업무 댓글 생성 도구 - content 누락 에러")
    fun testCreatePostCommentHandlerMissingContent() = runTest {
        // given
        val mockDoorayClient = mockk<DoorayClient>()

        val mockRequest = mockk<CallToolRequest>()
        every { mockRequest.arguments } returns
                buildJsonObject {
                    put("project_id", "project1")
                    put("post_id", "post1")
                    // content 누락
                }

        // when
        val mockProjectResolver = mockk<ProjectResolver>(relaxed = true)
        val handler = createPostCommentHandler(mockDoorayClient, mockProjectResolver)
        val result = handler(mockk<ClientConnection>(), mockRequest)

        // then
        assertTrue(result.content.isNotEmpty())
        val content = result.content.first() as TextContent
        val responseText = content.text
        assertContains(responseText, "\"isError\": true")
        assertContains(responseText, "MISSING_CONTENT")
    }

    @Test
    @DisplayName("업무 댓글 수정 도구 - 성공 케이스")
    fun testUpdatePostCommentHandlerSuccess() = runTest {
        // given
        val mockDoorayClient = mockk<DoorayClient>()

        val mockResponse =
            UpdateCommentResponse(
                header =
                    DoorayApiHeader(
                        isSuccessful = true,
                        resultCode = 0,
                        resultMessage = "success"
                    ),
                result = null
            )

        coEvery { mockDoorayClient.updatePostComment(any(), any(), any(), any()) } returns
                mockResponse

        val mockRequest = mockk<CallToolRequest>()
        every { mockRequest.arguments } returns
                buildJsonObject {
                    put("project_id", "project1")
                    put("post_id", "post1")
                    put("log_id", "comment1")
                    put("content", "수정된 댓글 내용")
                }

        // when
        val mockProjectResolver = mockk<ProjectResolver>()
        coEvery { mockProjectResolver.resolveProjectId("project1") } returns "project1"
        val handler = updatePostCommentHandler(mockDoorayClient, mockProjectResolver)
        val result = handler(mockk<ClientConnection>(), mockRequest)

        // then
        assertTrue(result.content.isNotEmpty())
        val content = result.content.first() as TextContent
        val responseText = content.text
        assertContains(responseText, "\"success\": true")
        assertContains(responseText, "성공적으로 수정")
    }

    @Test
    @DisplayName("프로젝트 업무 목록 조회 도구 - 신규 필터 파라미터가 API 호출에 전달되는지 검증")
    fun testGetProjectPostsHandlerNewFilterParams() = runTest {
        // given
        val mockDoorayClient = mockk<DoorayClient>()

        val mockPost = Post(
            id = "post1",
            subject = "테스트 업무",
            project = ProjectInfo(id = "project1", code = "TEST"),
            taskNumber = "TEST-1",
            closed = false,
            createdAt = "2024-01-01T00:00:00+09:00",
            updatedAt = "2024-01-02T00:00:00+09:00",
            number = 1,
            priority = "normal",
            workflowClass = "working",
            workflow = Workflow(id = "wf1", name = "진행 중"),
            users = PostUsers(
                from = PostUser(type = "member", member = Member(organizationMemberId = "member1")),
                to = listOf(PostUser(type = "member", member = Member(organizationMemberId = "member2"))),
                cc = emptyList()
            )
        )
        val mockResponse = PostListResponse(
            header = DoorayApiHeader(isSuccessful = true, resultCode = 0, resultMessage = "success"),
            result = listOf(mockPost),
            totalCount = 1
        )

        coEvery { mockDoorayClient.getPosts(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns mockResponse

        val mockRequest = mockk<CallToolRequest>()
        every { mockRequest.arguments } returns buildJsonObject {
            put("project_id", "project1")
            putJsonArray("from_member_ids") { add("member1") }
            put("post_number", "42")
            putJsonArray("post_workflow_ids") { add("wf1"); add("wf2") }
            put("created_at", "2024-01-01T00:00:00+09:00")
            put("updated_at", "2024-01-02T00:00:00+09:00")
            put("due_at", "2024-12-31T18:00:00+09:00")
        }

        val mockProjectResolver = mockk<ProjectResolver>()
        coEvery { mockProjectResolver.resolveProjectId("project1") } returns "project1"

        // when
        val handler = getProjectPostsHandler(mockDoorayClient, mockProjectResolver)
        val result = handler(mockk<ClientConnection>(), mockRequest)

        // then - 응답 성공 확인
        assertTrue(result.content.isNotEmpty())
        val responseText = (result.content.first() as TextContent).text
        assertContains(responseText, "\"success\": true")

        // then - 새 파라미터가 실제로 API에 전달됐는지 확인
        coVerify {
            mockDoorayClient.getPosts(
                projectId = "project1",
                page = any(),
                size = any(),
                fromMemberIds = listOf("member1"),
                toMemberIds = any(),
                ccMemberIds = any(),
                tagIds = any(),
                parentPostId = any(),
                postNumber = "42",
                postWorkflowClasses = any(),
                postWorkflowIds = listOf("wf1", "wf2"),
                milestoneIds = any(),
                subjects = any(),
                createdAt = "2024-01-01T00:00:00+09:00",
                updatedAt = "2024-01-02T00:00:00+09:00",
                dueAt = "2024-12-31T18:00:00+09:00",
                order = any()
            )
        }
    }

    @Test
    @DisplayName("프로젝트 업무 목록 조회 도구 - 새 필터 없이 기본 파라미터만 사용 시 null 전달")
    fun testGetProjectPostsHandlerNewFilterParamsDefaultNull() = runTest {
        // given
        val mockDoorayClient = mockk<DoorayClient>()
        val mockResponse = PostListResponse(
            header = DoorayApiHeader(isSuccessful = true, resultCode = 0, resultMessage = "success"),
            result = emptyList(),
            totalCount = 0
        )

        coEvery { mockDoorayClient.getPosts(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns mockResponse

        val mockRequest = mockk<CallToolRequest>()
        every { mockRequest.arguments } returns buildJsonObject { put("project_id", "project1") }

        val mockProjectResolver = mockk<ProjectResolver>()
        coEvery { mockProjectResolver.resolveProjectId("project1") } returns "project1"

        // when
        val handler = getProjectPostsHandler(mockDoorayClient, mockProjectResolver)
        handler(mockk<ClientConnection>(), mockRequest)

        // then - 미입력 파라미터는 null로 전달돼야 함
        coVerify {
            mockDoorayClient.getPosts(
                projectId = "project1",
                page = any(),
                size = any(),
                fromMemberIds = null,
                toMemberIds = any(),
                ccMemberIds = any(),
                tagIds = any(),
                parentPostId = any(),
                postNumber = null,
                postWorkflowClasses = any(),
                postWorkflowIds = any(),
                milestoneIds = any(),
                subjects = any(),
                createdAt = any(),
                updatedAt = any(),
                dueAt = null,
                order = any()
            )
        }
    }

    @Test
    @DisplayName("업무 댓글 삭제 도구 - 성공 케이스")
    fun testDeletePostCommentHandlerSuccess() = runTest {
        // given
        val mockDoorayClient = mockk<DoorayClient>()

        val mockResponse =
            DeleteCommentResponse(
                header =
                    DoorayApiHeader(
                        isSuccessful = true,
                        resultCode = 0,
                        resultMessage = "success"
                    ),
                result = null
            )

        coEvery { mockDoorayClient.deletePostComment(any(), any(), any()) } returns mockResponse

        val mockRequest = mockk<CallToolRequest>()
        every { mockRequest.arguments } returns
                buildJsonObject {
                    put("project_id", "project1")
                    put("post_id", "post1")
                    put("log_id", "comment1")
                }

        // when
        val mockProjectResolver = mockk<ProjectResolver>()
        coEvery { mockProjectResolver.resolveProjectId("project1") } returns "project1"
        val handler = deletePostCommentHandler(mockDoorayClient, mockProjectResolver)
        val result = handler(mockk<ClientConnection>(), mockRequest)

        // then
        assertTrue(result.content.isNotEmpty())
        val content = result.content.first() as TextContent
        val responseText = content.text
        assertContains(responseText, "\"success\": true")
        assertContains(responseText, "성공적으로 삭제")
    }
}
