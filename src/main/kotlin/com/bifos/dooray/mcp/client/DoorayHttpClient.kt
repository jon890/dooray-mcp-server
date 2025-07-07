package com.bifos.dooray.mcp.client

import com.bifos.dooray.mcp.exception.CustomException
import com.bifos.dooray.mcp.types.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class DoorayHttpClient(private val baseUrl: String, private val doorayApiKey: String) :
    DoorayClient {

    private val log = LoggerFactory.getLogger(DoorayHttpClient::class.java)
    private val httpClient: HttpClient

    init {
        httpClient = initHttpClient()
    }

    private fun initHttpClient(): HttpClient {
        return HttpClient {
            defaultRequest {
                url(baseUrl)
                header("Authorization", "dooray-api $doorayApiKey")
                contentType(ContentType.Application.Json)
            }

            // install content negotiation plugin for JSON serialization/deserialization
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        prettyPrint = true
                    }
                )
            }

            // HTTP 요청/응답 로깅 활성화
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.ALL
            }
        }
    }

    /**
     * API 호출을 공통 템플릿으로 처리합니다.
     * @param operation API 요청 설명 (로깅용)
     * @param expectedStatusCode 성공으로 간주할 HTTP 상태 코드
     * @param successMessage 성공 시 로깅할 메시지 (null이면 기본 메시지)
     * @param apiCall 실제 HTTP 호출을 수행하는 lambda
     */
    private suspend inline fun <reified T> executeApiCall(
        operation: String,
        expectedStatusCode: HttpStatusCode = HttpStatusCode.OK,
        successMessage: String? = null,
        crossinline apiCall: suspend () -> HttpResponse
    ): T {
        try {
            log.info("🔗 API 요청: $operation")
            val response = apiCall()
            log.info("📡 응답 수신: ${response.status} ${response.status.description}")

            return when (response.status) {
                expectedStatusCode -> {
                    val result = response.body<T>()
                    log.info(successMessage ?: "✅ API 호출 성공")
                    result
                }

                else -> {
                    handleErrorResponse(response)
                }
            }
        } catch (e: CustomException) {
            throw e
        } catch (e: Exception) {
            handleGenericException(e)
        }
    }

    /** 에러 응답을 공통으로 처리합니다. */
    private suspend fun handleErrorResponse(response: HttpResponse): Nothing {
        val responseBody = response.bodyAsText()
        log.error("❌ API 오류 응답:")
        log.error("  상태 코드: ${response.status.value} ${response.status.description}")
        log.error("  응답 본문: $responseBody")

        try {
            val errorResponse = response.body<DoorayErrorResponse>()
            val errorMessage = "API 호출 실패: ${errorResponse.header.resultMessage}"
            throw CustomException(errorMessage, response.status.value)
        } catch (parseException: Exception) {
            val errorMessage = "API 응답 파싱 실패 (${response.status.value}): $responseBody"
            throw CustomException(errorMessage, response.status.value, parseException)
        }
    }

    /** 일반 예외를 공통으로 처리합니다. */
    private fun handleGenericException(e: Exception): Nothing {
        log.error("❌ 네트워크 또는 기타 오류:")
        log.error("  타입: ${e::class.simpleName}")
        log.error("  메시지: ${e.message}")
        log.error("스택 트레이스:", e)

        val errorMessage = "API 호출 중 오류 발생: ${e.message}"
        throw CustomException(errorMessage, null, e)
    }

    /** result가 null일 수 있는 API 호출을 위한 특별 처리 */
    private suspend fun executeApiCallForNullableResult(
        operation: String,
        expectedStatusCode: HttpStatusCode = HttpStatusCode.OK,
        successMessage: String,
        apiCall: suspend () -> HttpResponse
    ): DoorayApiUnitResponse {
        try {
            log.info("🔗 API 요청: $operation")
            val response = apiCall()
            log.info("📡 응답 수신: ${response.status} ${response.status.description}")

            return when (response.status) {
                expectedStatusCode -> {
                    // result가 null일 수 있는 응답을 파싱
                    val jsonResponse = response.body<DoorayApiUnitResponse>()
                    if (jsonResponse.header.isSuccessful) {
                        log.info(successMessage)
                    } else {
                        log.warn("⚠️ API 응답 에러: ${jsonResponse.header.resultMessage}")
                    }
                    jsonResponse
                }

                else -> {
                    handleErrorResponse(response)
                }
            }
        } catch (e: CustomException) {
            throw e
        } catch (e: Exception) {
            handleGenericException(e)
        }
    }

    override suspend fun getWikis(page: Int?, size: Int?): WikiListResponse {
        return executeApiCall(operation = "GET /wiki/v1/wikis", successMessage = "✅ 위키 목록 조회 성공") {
            httpClient.get("/wiki/v1/wikis") {
                page?.let { parameter("page", it) }
                size?.let { parameter("size", it) }
            }
        }
    }

    override suspend fun getWikiPages(projectId: String): WikiPagesResponse {
        return executeApiCall(
            operation = "GET /wiki/v1/wikis/$projectId/pages",
            successMessage = "✅ 위키 페이지 목록 조회 성공"
        ) { httpClient.get("/wiki/v1/wikis/$projectId/pages") }
    }

    override suspend fun getWikiPages(projectId: String, parentPageId: String): WikiPagesResponse {
        return executeApiCall(
            operation = "GET /wiki/v1/wikis/$projectId/pages?parentPageId=$parentPageId",
            successMessage = "✅ 자식 위키 페이지 목록 조회 성공"
        ) {
            httpClient.get("/wiki/v1/wikis/$projectId/pages") {
                parameter("parentPageId", parentPageId)
            }
        }
    }

    override suspend fun getWikiPage(projectId: String, pageId: String): WikiPageResponse {
        return executeApiCall(
            operation = "GET /wiki/v1/wikis/$projectId/pages/$pageId",
            successMessage = "✅ 위키 페이지 조회 성공"
        ) { httpClient.get("/wiki/v1/wikis/$projectId/pages/$pageId") }
    }

    override suspend fun createWikiPage(
        wikiId: String,
        request: CreateWikiPageRequest
    ): CreateWikiPageResponse {
        return executeApiCall(
            operation = "POST /wiki/v1/wikis/$wikiId/pages",
            expectedStatusCode = HttpStatusCode.Created,
            successMessage = "✅ 위키 페이지 생성 성공"
        ) { httpClient.post("/wiki/v1/wikis/$wikiId/pages") { setBody(request) } }
    }

    override suspend fun updateWikiPage(
        wikiId: String,
        pageId: String,
        request: UpdateWikiPageRequest
    ): WikiPageResponse {
        return executeApiCall(
            operation = "PUT /wiki/v1/wikis/$wikiId/pages/$pageId",
            successMessage = "✅ 위키 페이지 수정 성공"
        ) { httpClient.put("/wiki/v1/wikis/$wikiId/pages/$pageId") { setBody(request) } }
    }

    override suspend fun updateWikiPageTitle(
        wikiId: String,
        pageId: String,
        subject: String
    ): DoorayApiUnitResponse {
        return executeApiCallForNullableResult(
            operation = "PUT /wiki/v1/wikis/$wikiId/pages/$pageId/title",
            successMessage = "✅ 위키 페이지 제목 수정 성공"
        ) {
            httpClient.put("/wiki/v1/wikis/$wikiId/pages/$pageId/title") {
                setBody(mapOf("subject" to subject))
            }
        }
    }

    override suspend fun updateWikiPageContent(
        wikiId: String,
        pageId: String,
        body: String
    ): DoorayApiUnitResponse {
        return executeApiCallForNullableResult(
            operation = "PUT /wiki/v1/wikis/$wikiId/pages/$pageId/content",
            successMessage = "✅ 위키 페이지 내용 수정 성공"
        ) {
            httpClient.put("/wiki/v1/wikis/$wikiId/pages/$pageId/content") {
                setBody(mapOf("body" to mapOf("mimeType" to "text/x-markdown", "content" to body)))
            }
        }
    }

    override suspend fun updateWikiPageReferrers(
        wikiId: String,
        pageId: String,
        referrers: List<WikiReferrer>
    ): DoorayApiUnitResponse {
        return executeApiCallForNullableResult(
            operation = "PUT /wiki/v1/wikis/$wikiId/pages/$pageId/referrers",
            successMessage = "✅ 위키 페이지 참조자 수정 성공"
        ) {
            httpClient.put("/wiki/v1/wikis/$wikiId/pages/$pageId/referrers") {
                setBody(mapOf("referrers" to referrers))
            }
        }
    }

    // ============ 프로젝트 업무 관련 API 구현 ============

    override suspend fun createPost(
        projectId: String,
        request: CreatePostRequest
    ): CreatePostApiResponse {
        return executeApiCall(
            operation = "POST /project/v1/projects/$projectId/posts",
            expectedStatusCode = HttpStatusCode.OK,
            successMessage = "✅ 업무 생성 성공"
        ) { httpClient.post("/project/v1/projects/$projectId/posts") { setBody(request) } }
    }

    override suspend fun getPosts(
        projectId: String,
        page: Int?,
        size: Int?,
        fromMemberIds: List<String>?,
        toMemberIds: List<String>?,
        ccMemberIds: List<String>?,
        tagIds: List<String>?,
        parentPostId: String?,
        postNumber: String?,
        postWorkflowClasses: List<String>?,
        postWorkflowIds: List<String>?,
        milestoneIds: List<String>?,
        subjects: String?,
        createdAt: String?,
        updatedAt: String?,
        dueAt: String?,
        order: String?
    ): PostListResponse {
        return executeApiCall(
            operation = "GET /project/v1/projects/$projectId/posts",
            successMessage = "✅ 업무 목록 조회 성공"
        ) {
            httpClient.get("/project/v1/projects/$projectId/posts") {
                page?.let { parameter("page", it) }
                size?.let { parameter("size", it) }
                fromMemberIds?.let {
                    if (it.isNotEmpty()) parameter("fromMemberIds", it.joinToString(","))
                }
                toMemberIds?.let {
                    if (it.isNotEmpty()) parameter("toMemberIds", it.joinToString(","))
                }
                ccMemberIds?.let {
                    if (it.isNotEmpty()) parameter("ccMemberIds", it.joinToString(","))
                }
                tagIds?.let { if (it.isNotEmpty()) parameter("tagIds", it.joinToString(",")) }
                parentPostId?.let { parameter("parentPostId", it) }
                postNumber?.let { parameter("postNumber", it) }
                postWorkflowClasses?.let {
                    if (it.isNotEmpty()) parameter("postWorkflowClasses", it.joinToString(","))
                }
                postWorkflowIds?.let {
                    if (it.isNotEmpty()) parameter("postWorkflowIds", it.joinToString(","))
                }
                milestoneIds?.let {
                    if (it.isNotEmpty()) parameter("milestoneIds", it.joinToString(","))
                }
                subjects?.let { parameter("subjects", it) }
                createdAt?.let { parameter("createdAt", it) }
                updatedAt?.let { parameter("updatedAt", it) }
                dueAt?.let { parameter("dueAt", it) }
                order?.let { parameter("order", it) }
            }
        }
    }

    override suspend fun getPost(projectId: String, postId: String): PostDetailResponse {
        return executeApiCall(
            operation = "GET /project/v1/projects/$projectId/posts/$postId",
            successMessage = "✅ 업무 상세 조회 성공"
        ) { httpClient.get("/project/v1/projects/$projectId/posts/$postId") }
    }

    override suspend fun updatePost(
        projectId: String,
        postId: String,
        request: UpdatePostRequest
    ): UpdatePostResponse {
        return executeApiCall(
            operation = "PUT /project/v1/projects/$projectId/posts/$postId",
            successMessage = "✅ 업무 수정 성공"
        ) { httpClient.put("/project/v1/projects/$projectId/posts/$postId") { setBody(request) } }
    }

    override suspend fun updatePostUserWorkflow(
        projectId: String,
        postId: String,
        organizationMemberId: String,
        workflowId: String
    ): DoorayApiUnitResponse {
        return executeApiCallForNullableResult(
            operation =
                "PUT /project/v1/projects/$projectId/posts/$postId/to/$organizationMemberId",
            successMessage = "✅ 담당자 상태 변경 성공"
        ) {
            httpClient.put(
                "/project/v1/projects/$projectId/posts/$postId/to/$organizationMemberId"
            ) { setBody(SetWorkflowRequest(workflowId)) }
        }
    }

    override suspend fun setPostWorkflow(
        projectId: String,
        postId: String,
        workflowId: String
    ): DoorayApiUnitResponse {
        return executeApiCallForNullableResult(
            operation = "POST /project/v1/projects/$projectId/posts/$postId/set-workflow",
            successMessage = "✅ 업무 상태 변경 성공"
        ) {
            httpClient.post("/project/v1/projects/$projectId/posts/$postId/set-workflow") {
                setBody(SetWorkflowRequest(workflowId))
            }
        }
    }

    override suspend fun setPostDone(projectId: String, postId: String): DoorayApiUnitResponse {
        return executeApiCallForNullableResult(
            operation = "POST /project/v1/projects/$projectId/posts/$postId/set-done",
            successMessage = "✅ 업무 완료 처리 성공"
        ) { httpClient.post("/project/v1/projects/$projectId/posts/$postId/set-done") }
    }

    override suspend fun setPostParent(
        projectId: String,
        postId: String,
        parentPostId: String
    ): DoorayApiUnitResponse {
        return executeApiCallForNullableResult(
            operation = "POST /project/v1/projects/$projectId/posts/$postId/set-parent-post",
            successMessage = "✅ 상위 업무 설정 성공"
        ) {
            httpClient.post("/project/v1/projects/$projectId/posts/$postId/set-parent-post") {
                setBody(SetParentPostRequest(parentPostId))
            }
        }
    }

    override suspend fun getProjects(
        page: Int?,
        size: Int?,
        type: String?,
        scope: String?,
        state: String?
    ): ProjectListResponse {
        return executeApiCall(
            operation = "GET /project/v1/projects",
            successMessage = "✅ 프로젝트 목록 조회 성공"
        ) {
            httpClient.get("/project/v1/projects") {
                parameter("member", "me")
                page?.let { parameter("page", it) }
                size?.let { parameter("size", it) }
                type?.let { parameter("type", it) }
                scope?.let { parameter("scope", it) }
                state?.let { parameter("state", it) }
            }
        }
    }
}
