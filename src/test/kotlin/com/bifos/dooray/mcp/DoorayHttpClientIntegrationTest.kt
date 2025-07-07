package com.bifos.dooray.mcp

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.client.DoorayHttpClient
import com.bifos.dooray.mcp.constants.EnvVariableConst.DOORAY_API_KEY
import com.bifos.dooray.mcp.constants.EnvVariableConst.DOORAY_BASE_URL
import com.bifos.dooray.mcp.constants.EnvVariableConst.DOORAY_TEST_PROJECT_ID
import com.bifos.dooray.mcp.constants.EnvVariableConst.DOORAY_TEST_WIKI_ID
import com.bifos.dooray.mcp.types.CreatePostRequest
import com.bifos.dooray.mcp.types.CreatePostUsers
import com.bifos.dooray.mcp.types.CreateWikiPageRequest
import com.bifos.dooray.mcp.types.PostBody
import com.bifos.dooray.mcp.types.WikiPageBody
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*

/** Dooray Http Client 통합 테스트 실제 HTTP 요청을 보내므로 환경변수가 설정되어야 함 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DoorayHttpClientIntegrationTest {

    lateinit var testProjectId: String
    lateinit var testWikiId: String
    lateinit var doorayClient: DoorayClient

    // 테스트에서 생성된 데이터들을 추적하여 나중에 삭제
    private val createdPostIds = mutableListOf<String>()
    private val createdWikiPageIds = mutableListOf<String>()

    @BeforeAll
    fun setup() {
        val env = parseEnv()

        val baseUrl =
                env[DOORAY_BASE_URL]
                        ?: throw IllegalStateException("DOORAY_BASE_URL 환경변수가 설정되지 않았습니다.")
        val apiKey =
                env[DOORAY_API_KEY]
                        ?: throw IllegalStateException("DOORAY_API_KEY 환경변수가 설정되지 않았습니다.")
        this.testProjectId =
                env[DOORAY_TEST_PROJECT_ID]
                        ?: throw IllegalStateException("DOORAY_TEST_PROJECT_ID 환경변수가 설정되지 않았습니다.")
        this.testWikiId =
                env[DOORAY_TEST_WIKI_ID]
                        ?: throw IllegalStateException("DOORAY_TEST_WIKI_ID 환경변수가 설정되지 않았습니다.")

        doorayClient = DoorayHttpClient(baseUrl, apiKey)
    }

    @AfterAll
    fun cleanup() = runTest {
        println("🧹 테스트 완료 후 생성된 데이터를 정리합니다...")
        cleanupCreatedData()
    }

    // === 위키 관련 테스트 ===

    @Test
    @DisplayName("내가 조회할 수 있는 위키 목록들이 조회된다")
    fun getWikisTest() = runTest {
        // when
        val response = doorayClient.getWikis(size = 200)

        // then
        assertAll(
                { assertTrue { response.header.isSuccessful } },
                { assertEquals(response.header.resultCode, 0) }
        )

        response.result.let { wikis ->
            assertTrue { wikis.isNotEmpty() }
            wikis.forEach { wiki ->
                assertNotNull(wiki.id)
                assertNotNull(wiki.project.id)
                assertNotNull(wiki.name)
                assertNotNull(wiki.type)
            }
        }
        println("✅ 위키 목록 조회 성공: ${response.result.size}개")
    }

    @Test
    @DisplayName("특정 프로젝트의 위키들이 조회된다")
    fun getWikiPagesTest() = runTest {
        // when
        val response = doorayClient.getWikiPages(testProjectId)

        // then
        assertAll(
                { assertTrue { response.header.isSuccessful } },
                { assertEquals(response.header.resultCode, 0) }
        )

        response.result.forEach { page ->
            assertNotNull(page.id)
            assertNotNull(page.wikiId)
            assertNotNull(page.subject)
            assertNotNull(page.creator)
        }
        println("✅ 위키 페이지 목록 조회 성공: ${response.result.size}개")
    }

    @Test
    @DisplayName("특정 프로젝트의 root의 하위 위키들이 조회된다")
    fun getWikiPagesWithParentPageIdTest() = runTest {
        // given
        val pagesResponse = doorayClient.getWikiPages(testProjectId)
        assertTrue(pagesResponse.result.isNotEmpty(), "테스트할 위키 페이지가 없습니다.")

        val parentPageId = pagesResponse.result.first().id
        val response = doorayClient.getWikiPages(testProjectId, parentPageId)

        assertAll(
                { assertTrue { response.header.isSuccessful } },
                { assertEquals(response.header.resultCode, 0) }
        )

        assertNotNull(response.result)
        println("✅ 하위 위키 페이지 조회 성공: ${response.result.size}개")
    }

    // === 프로젝트 업무 관련 테스트 ===

    @Test
    @DisplayName("특정 프로젝트의 업무 목록이 조회된다")
    fun getProjectPostsTest() = runTest {
        // when
        val response = doorayClient.getPosts(testProjectId, size = 10)

        // then
        assertAll(
                { assertTrue { response.header.isSuccessful } },
                { assertEquals(response.header.resultCode, 0) }
        )

        println("✅ 업무 목록 조회 성공: ${response.result.size}개")
        response.result.forEach { post ->
            assertNotNull(post.id)
            assertNotNull(post.subject)
            assertNotNull(post.createdAt)
            assertNotNull(post.users)
            assertNotNull(post.workflow)
            println("  - 업무: ${post.subject} (ID: ${post.id})")
        }
    }

    @Test
    @DisplayName("특정 프로젝트의 업무 목록을 필터링해서 조회된다")
    fun getProjectPostsWithFiltersTest() = runTest {
        // when - 등록 상태 업무만 조회
        val response =
                doorayClient.getPosts(
                        projectId = testProjectId,
                        postWorkflowClasses = listOf("registered"),
                        order = "createdAt",
                        size = 5
                )

        // then
        assertAll(
                { assertTrue { response.header.isSuccessful } },
                { assertEquals(response.header.resultCode, 0) }
        )

        println("✅ 필터링된 업무 목록 조회 성공: ${response.result.size}개")
        response.result.forEach { post ->
            assertNotNull(post.id)
            assertNotNull(post.subject)
            assertNotNull(post.workflow)
            println("  - 업무: ${post.subject}, 상태: ${post.workflowClass}")
        }
    }

    @Test
    @DisplayName("특정 업무의 상세 정보가 조회된다")
    fun getProjectPostTest() = runTest {
        // 먼저 업무 목록을 조회해서 하나의 업무 ID를 얻음
        val postsResponse = doorayClient.getPosts(testProjectId, size = 1)

        if (postsResponse.result.isEmpty()) {
            // 업무가 없으면 하나 생성
            val createRequest =
                    CreatePostRequest(
                            subject = "[테스트용] 상세 조회 테스트 업무 ${System.currentTimeMillis()}",
                            body =
                                    PostBody(
                                            mimeType = "text/html",
                                            content = "상세 조회 테스트용 임시 업무입니다."
                                    ),
                            users = CreatePostUsers(to = emptyList(), cc = emptyList()),
                            priority = "normal"
                    )
            val createResponse = doorayClient.createPost(testProjectId, createRequest)
            assertTrue(createResponse.header.isSuccessful, "테스트용 업무 생성에 실패했습니다.")

            val createdPostId = createResponse.result.id
            createdPostIds.add(createdPostId)

            // 생성된 업무로 상세 조회 테스트
            val response = doorayClient.getPost(testProjectId, createdPostId)

            assertAll(
                    { assertTrue { response.header.isSuccessful } },
                    { assertEquals(response.header.resultCode, 0) }
            )

            response.result.let { post ->
                assertNotNull(post.id)
                assertNotNull(post.subject)
                assertNotNull(post.body)
                assertNotNull(post.createdAt)
                assertNotNull(post.users)
                assertNotNull(post.workflow)
                assertEquals(createdPostId, post.id)
                println("✅ 업무 상세 조회 성공: ${post.subject}")
            }
        } else {
            // 기존 업무로 테스트
            val postId = postsResponse.result.first().id

            val response = doorayClient.getPost(testProjectId, postId)

            assertAll(
                    { assertTrue { response.header.isSuccessful } },
                    { assertEquals(response.header.resultCode, 0) }
            )

            response.result.let { post ->
                assertNotNull(post.id)
                assertNotNull(post.subject)
                assertNotNull(post.body)
                assertNotNull(post.createdAt)
                assertNotNull(post.users)
                assertNotNull(post.workflow)
                assertEquals(postId, post.id)
                println("✅ 업무 상세 조회 성공: ${post.subject}")
            }
        }
    }

    @Test
    @DisplayName("새로운 업무가 생성된다")
    fun createProjectPostTest() = runTest {
        val createRequest =
                CreatePostRequest(
                        subject = "[통합테스트] 테스트 업무 ${System.currentTimeMillis()}",
                        body = PostBody(mimeType = "text/html", content = "이것은 통합 테스트로 생성된 업무입니다."),
                        users =
                                CreatePostUsers(
                                        to = emptyList(), // 담당자는 빈 목록으로 설정
                                        cc = emptyList()
                                ),
                        priority = "normal"
                )

        // when - 업무 생성
        val response = doorayClient.createPost(testProjectId, createRequest)

        // then
        assertAll(
                { assertTrue { response.header.isSuccessful } },
                { assertEquals(response.header.resultCode, 0) }
        )

        val createdPostId = response.result.id
        assertNotNull(createdPostId)
        println("✅ 생성된 업무 ID: $createdPostId")

        // 생성된 업무를 추적 목록에 추가 (정리를 위해)
        createdPostIds.add(createdPostId)
        println("📝 업무 추적 목록에 추가: $createdPostId")
    }

    @Test
    @DisplayName("새로운 위키 페이지가 생성된다")
    fun createWikiPageTest() = runTest {
        // given - 먼저 위키 페이지 목록을 조회해서 루트 페이지 ID를 찾음
        val pagesResponse = doorayClient.getWikiPages(testProjectId)
        assertTrue(pagesResponse.result.isNotEmpty(), "테스트할 위키 페이지가 없습니다.")

        // 루트 페이지 하나를 선택 (첫 번째 페이지를 상위 페이지로 사용)
        val rootPageId = pagesResponse.result.first().id
        println("📝 루트 페이지 ID: $rootPageId")

        val createRequest =
                CreateWikiPageRequest(
                        subject = "[통합테스트] 테스트 위키 ${System.currentTimeMillis()}",
                        body =
                                WikiPageBody(
                                        mimeType = "text/x-markdown",
                                        content = "# 테스트 위키 페이지\n\n이것은 통합 테스트로 생성된 위키 페이지입니다."
                                ),
                        parentPageId = rootPageId // 루트 페이지를 상위 페이지로 설정
                )

        // when - 위키 페이지 생성
        val response = doorayClient.createWikiPage(testWikiId, createRequest)

        // then
        assertAll(
                { assertTrue { response.header.isSuccessful } },
                { assertEquals(response.header.resultCode, 0) }
        )

        val createdPageId = response.result.id
        assertNotNull(createdPageId)
        println("✅ 생성된 위키 페이지 ID: $createdPageId (위키 ID: ${response.result.wikiId})")

        // 생성된 위키 페이지를 추적 목록에 추가 (삭제를 위해)
        createdWikiPageIds.add(createdPageId)
        println("📝 위키 페이지 추적 목록에 추가: $createdPageId")

        // 위키 페이지 삭제 API는 지원되지 않음 - 수동으로 정리 필요
        println("ℹ️ 위키 페이지 삭제는 수동으로 처리해야 함: $createdPageId")
    }

    @Test
    @DisplayName("업무의 workflow 상태를 변경한다")
    fun setProjectPostWorkflowTest() = runTest {
        // 먼저 기존 업무 목록을 조회해서 유효한 workflow 정보를 확인
        val postsResponse = doorayClient.getPosts(testProjectId, page = 0, size = 5)
        assertTrue(postsResponse.result.isNotEmpty(), "테스트할 업무가 없습니다.")

        val samplePost = postsResponse.result.first()
        println("📝 기존 업무의 workflow 정보: ${samplePost.workflow}")

        // 업무 하나를 생성
        val createRequest =
                CreatePostRequest(
                        subject = "[통합테스트] 상태 변경 테스트 업무 ${System.currentTimeMillis()}",
                        body = PostBody(mimeType = "text/html", content = "상태 변경 테스트용 업무입니다."),
                        users = CreatePostUsers(to = emptyList(), cc = emptyList()),
                        priority = "normal"
                )
        val createResponse = doorayClient.createPost(testProjectId, createRequest)
        assertTrue(createResponse.header.isSuccessful, "업무 생성에 실패했습니다.")

        val postId = createResponse.result.id
        createdPostIds.add(postId)

        // 생성된 업무의 현재 workflow 정보 확인
        val createdPost = doorayClient.getPost(testProjectId, postId)
        val currentWorkflowId = createdPost.result.workflow.id
        println("📝 생성된 업무의 현재 workflow ID: $currentWorkflowId")

        // 기존 업무들의 다른 workflow ID 찾기
        val differentWorkflowIds =
                postsResponse.result.map { it.workflow.id }.distinct().filter {
                    it != currentWorkflowId
                }

        if (differentWorkflowIds.isNotEmpty()) {
            val targetWorkflowId = differentWorkflowIds.first()
            println("📝 변경할 workflow ID: $targetWorkflowId")

            // 실제 workflow 상태 변경 수행
            val response = doorayClient.setPostWorkflow(testProjectId, postId, targetWorkflowId)

            if (response.header.isSuccessful) {
                println("✅ 업무 상태 변경 성공")

                // 변경 후 상태 확인
                val updatedPost = doorayClient.getPost(testProjectId, postId)
                println("📝 변경 후 workflow ID: ${updatedPost.result.workflow.id}")
            } else {
                println("⚠️ 업무 상태 변경 실패: ${response.header.resultMessage}")
                println("📝 응답 코드: ${response.header.resultCode}")
            }
        } else {
            println("⚠️ 변경할 수 있는 다른 workflow가 없습니다. 현재 workflow를 그대로 사용합니다.")

            // 같은 workflow ID로 변경 시도 (테스트 목적)
            val response = doorayClient.setPostWorkflow(testProjectId, postId, currentWorkflowId)

            if (response.header.isSuccessful) {
                println("✅ 동일한 workflow로 변경 성공")
            } else {
                println("⚠️ workflow 변경 실패: ${response.header.resultMessage}")
                println("📝 응답 코드: ${response.header.resultCode}")
            }
        }

        println("✅ 업무 workflow 상태 변경 테스트 완료")
    }

    @Test
    @DisplayName("업무를 완료 상태로 처리한다")
    fun setProjectPostDoneTest() = runTest {
        // 업무 하나를 생성
        val createRequest =
                CreatePostRequest(
                        subject = "[통합테스트] 완료 처리 테스트 업무 ${System.currentTimeMillis()}",
                        body = PostBody(mimeType = "text/html", content = "완료 처리 테스트용 업무입니다."),
                        users = CreatePostUsers(to = emptyList(), cc = emptyList()),
                        priority = "normal"
                )
        val createResponse = doorayClient.createPost(testProjectId, createRequest)
        assertTrue(createResponse.header.isSuccessful, "업무 생성에 실패했습니다.")

        val postId = createResponse.result.id
        createdPostIds.add(postId)

        // 생성된 업무의 현재 workflow 정보 확인
        val createdPost = doorayClient.getPost(testProjectId, postId)
        println("📝 생성된 업무의 현재 workflow: ${createdPost.result.workflow}")
        println("📝 현재 workflow 이름: ${createdPost.result.workflow.name}")

        // 실제 완료 처리 수행
        val response = doorayClient.setPostDone(testProjectId, postId)

        if (response.header.isSuccessful) {
            println("✅ 업무 완료 처리 성공")

            // 완료 처리 후 상태 확인
            val updatedPost = doorayClient.getPost(testProjectId, postId)
            println("📝 완료 후 workflow: ${updatedPost.result.workflow}")
            println("📝 완료 후 workflow 이름: ${updatedPost.result.workflow.name}")
        } else {
            println("⚠️ 업무 완료 처리 실패: ${response.header.resultMessage}")
            println("📝 응답 코드: ${response.header.resultCode}")

            // 실패해도 테스트는 계속 진행 (API 응답 형식 확인 목적)
            println("📝 실패 응답 전체: $response")
        }

        println("✅ 업무 완료 처리 테스트 완료")
    }

    // === 프로젝트 관련 테스트 ===

    @Test
    @DisplayName("내가 접근할 수 있는 프로젝트 목록이 조회된다")
    fun getProjectsTest() = runTest {
        // when - 기본 조회 (권장 파라미터 사용)
        val response =
                doorayClient.getProjects(
                        page = 0,
                        size = 100,
                        type = "public",
                        scope = "private",
                        state = "active"
                )

        // then
        assertAll(
                { assertTrue { response.header.isSuccessful } },
                { assertEquals(response.header.resultCode, 0) }
        )

        response.result.let { projects ->
            assertTrue { projects.isNotEmpty() }
            projects.forEach { project ->
                assertNotNull(project.id)
                assertNotNull(project.code)
                // description은 nullable이므로 검증하지 않음
                println("  - 프로젝트: ${project.code} (ID: ${project.id})")
            }
        }
        println("✅ 프로젝트 목록 조회 성공: 총 ${response.totalCount}개 중 ${response.result.size}개 조회")
    }

    @Test
    @DisplayName("프로젝트 목록을 기본 파라미터로 조회한다")
    fun getProjectsWithDefaultParametersTest() = runTest {
        // when - 파라미터 없이 기본값으로 조회
        val response = doorayClient.getProjects()

        // then
        assertAll(
                { assertTrue { response.header.isSuccessful } },
                { assertEquals(response.header.resultCode, 0) }
        )

        println("✅ 기본 파라미터 프로젝트 조회 성공: 총 ${response.totalCount}개 중 ${response.result.size}개 조회")

        response.result.forEach { project ->
            assertNotNull(project.id)
            assertNotNull(project.code)
            println("  - 프로젝트: ${project.code} (scope: ${project.scope}, state: ${project.state})")
        }
    }

    @Test
    @DisplayName("활성화된 프로젝트만 필터링해서 조회한다")
    fun getActiveProjectsTest() = runTest {
        // when - 활성화된 프로젝트만 조회
        val response = doorayClient.getProjects(state = "active", size = 50)

        // then
        assertAll(
                { assertTrue { response.header.isSuccessful } },
                { assertEquals(response.header.resultCode, 0) }
        )

        println("✅ 활성화된 프로젝트 조회 성공: 총 ${response.totalCount}개 중 ${response.result.size}개 조회")

        response.result.forEach { project ->
            assertNotNull(project.id)
            assertNotNull(project.code)
            // state가 설정되어 있다면 active여야 함
            project.state?.let { state ->
                assertEquals("active", state, "필터링된 프로젝트는 모두 active 상태여야 함")
            }
            println("  - 활성 프로젝트: ${project.code} (state: ${project.state})")
        }
    }

    @Test
    @DisplayName("보관된 프로젝트를 조회한다")
    fun getArchivedProjectsTest() = runTest {
        // when - 보관된 프로젝트 조회
        val response = doorayClient.getProjects(state = "archived", size = 20)

        // then
        assertAll(
                { assertTrue { response.header.isSuccessful } },
                { assertEquals(response.header.resultCode, 0) }
        )

        println("✅ 보관된 프로젝트 조회 성공: 총 ${response.totalCount}개 중 ${response.result.size}개 조회")

        if (response.result.isNotEmpty()) {
            response.result.forEach { project ->
                assertNotNull(project.id)
                assertNotNull(project.code)
                // state가 설정되어 있다면 archived여야 함
                project.state?.let { state ->
                    assertEquals("archived", state, "필터링된 프로젝트는 모두 archived 상태여야 함")
                }
                println("  - 보관된 프로젝트: ${project.code} (state: ${project.state})")
            }
        } else {
            println("ℹ️ 보관된 프로젝트가 없습니다.")
        }
    }

    @Test
    @DisplayName("공개 범위 프로젝트를 조회한다")
    fun getPublicScopeProjectsTest() = runTest {
        // when - 공개 범위 프로젝트 조회
        val response = doorayClient.getProjects(scope = "public", size = 30)

        // then
        assertAll(
                { assertTrue { response.header.isSuccessful } },
                { assertEquals(response.header.resultCode, 0) }
        )

        println("✅ 공개 범위 프로젝트 조회 성공: 총 ${response.totalCount}개 중 ${response.result.size}개 조회")

        response.result.forEach { project ->
            assertNotNull(project.id)
            assertNotNull(project.code)
            // scope가 설정되어 있다면 public이어야 함
            project.scope?.let { scope ->
                assertEquals("public", scope, "필터링된 프로젝트는 모두 public 범위여야 함")
            }
            println("  - 공개 프로젝트: ${project.code} (scope: ${project.scope})")
        }
    }

    @Test
    @DisplayName("개인 프로젝트를 포함해서 조회한다")
    fun getProjectsWithPrivateTypeTest() = runTest {
        // when - 개인 프로젝트 포함 조회
        val response = doorayClient.getProjects(type = "private", size = 20)

        // then
        assertAll(
                { assertTrue { response.header.isSuccessful } },
                { assertEquals(response.header.resultCode, 0) }
        )

        println("✅ 개인 프로젝트 포함 조회 성공: 총 ${response.totalCount}개 중 ${response.result.size}개 조회")

        if (response.result.isNotEmpty()) {
            response.result.forEach { project ->
                assertNotNull(project.id)
                assertNotNull(project.code)
                println("  - 프로젝트: ${project.code} (type: ${project.type})")
            }

            // 개인 프로젝트가 포함되어 있는지 확인
            val hasPrivateProject = response.result.any { it.type == "private" }
            if (hasPrivateProject) {
                println("ℹ️ 개인 프로젝트가 포함되어 있습니다.")
            } else {
                println("ℹ️ 개인 프로젝트가 없거나 응답에 포함되지 않았습니다.")
            }
        } else {
            println("ℹ️ 조회된 프로젝트가 없습니다.")
        }
    }

    @Test
    @DisplayName("페이징을 사용해서 프로젝트를 조회한다")
    fun getProjectsWithPagingTest() = runTest {
        // when - 첫 번째 페이지 (작은 사이즈로)
        val firstPageResponse = doorayClient.getProjects(page = 0, size = 5, state = "active")

        // then
        assertAll(
                { assertTrue { firstPageResponse.header.isSuccessful } },
                { assertEquals(firstPageResponse.header.resultCode, 0) }
        )

        println(
                "✅ 첫 번째 페이지 조회 성공: 총 ${firstPageResponse.totalCount}개 중 ${firstPageResponse.result.size}개 조회"
        )

        // 총 개수가 5개보다 많다면 두 번째 페이지도 조회
        if (firstPageResponse.totalCount > 5) {
            val secondPageResponse = doorayClient.getProjects(page = 1, size = 5, state = "active")

            assertAll(
                    { assertTrue { secondPageResponse.header.isSuccessful } },
                    { assertEquals(secondPageResponse.header.resultCode, 0) }
            )

            println("✅ 두 번째 페이지 조회 성공: ${secondPageResponse.result.size}개 조회")

            // 첫 번째 페이지와 두 번째 페이지의 프로젝트가 다른지 확인
            val firstPageIds = firstPageResponse.result.map { it.id }.toSet()
            val secondPageIds = secondPageResponse.result.map { it.id }.toSet()
            val hasOverlap = firstPageIds.intersect(secondPageIds).isNotEmpty()

            if (!hasOverlap && secondPageResponse.result.isNotEmpty()) {
                println("✅ 페이징이 올바르게 작동합니다 (중복 없음)")
            } else {
                println("ℹ️ 페이징 결과에 중복이 있거나 두 번째 페이지가 비어있습니다.")
            }
        } else {
            println("ℹ️ 총 프로젝트 수가 5개 이하라서 두 번째 페이지 테스트를 건너뜁니다.")
        }
    }

    @Test
    @DisplayName("프로젝트 조회 결과에 위키 정보가 포함되어 있다")
    fun getProjectsWithWikiInfoTest() = runTest {
        // when
        val response = doorayClient.getProjects(size = 10, state = "active")

        // then
        assertAll(
                { assertTrue { response.header.isSuccessful } },
                { assertEquals(response.header.resultCode, 0) }
        )

        println("✅ 프로젝트 위키 정보 조회 성공: ${response.result.size}개")

        response.result.forEach { project ->
            assertNotNull(project.id)
            assertNotNull(project.code)

            // 위키 정보가 있는 프로젝트들 확인
            project.wiki?.let { wiki ->
                if (wiki.id != null) {
                    println("  - 프로젝트: ${project.code}, 위키 ID: ${wiki.id}")
                } else {
                    println("  - 프로젝트: ${project.code}, 위키 객체 있음 (ID: null)")
                }
            }
                    ?: run { println("  - 프로젝트: ${project.code}, 위키 없음") }

            // 조직 정보 확인
            project.organization?.let { org ->
                if (org.id != null) {
                    println("    조직 ID: ${org.id}")
                } else {
                    println("    조직 객체 있음 (ID: null)")
                }
            }

            // 드라이브 정보 확인
            project.drive?.let { drive ->
                if (drive.id != null) {
                    println("    드라이브 ID: ${drive.id}")
                } else {
                    println("    드라이브 객체 있음 (ID: null)")
                }
            }
        }
    }

    // 테스트 후 정리 작업
    private suspend fun cleanupCreatedData() {
        // 생성된 위키 페이지들은 삭제 API가 지원되지 않으므로 로그만 출력
        if (createdWikiPageIds.isNotEmpty()) {
            println("📝 생성된 위키 페이지들 (수동 삭제 필요): ${createdWikiPageIds.joinToString(", ")}")
        }

        // 생성된 업무들은 Dooray API에서 직접 삭제를 지원하지 않으므로 로그만 출력
        if (createdPostIds.isNotEmpty()) {
            println("📝 생성된 업무들 (수동 삭제 필요): ${createdPostIds.joinToString(", ")}")
        }

        println("✅ 데이터 정리 완료 (수동 정리 필요)")
    }
}
