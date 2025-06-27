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

class DoorayHttpClient(private val baseUrl: String, private val doorayApiKey: String) : DoorayClient {

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

    /** DELETE 요청과 같이 응답 본문이 없는 경우를 위한 특별 처리 */
    private suspend fun executeApiCallWithoutBody(
        operation: String,
        expectedStatusCode: HttpStatusCode = HttpStatusCode.NoContent,
        successMessage: String,
        apiCall: suspend () -> HttpResponse
    ): DoorayApiResponse<Unit> {
        try {
            log.info("🔗 API 요청: $operation")
            val response = apiCall()
            log.info("📡 응답 수신: ${response.status} ${response.status.description}")

            return when (response.status) {
                expectedStatusCode -> {
                    log.info(successMessage)
                    DoorayApiResponse(DoorayApiHeader(true, expectedStatusCode.value, "성공"), Unit)
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
        projectId: String,
        request: CreateWikiPageRequest
    ): WikiPageResponse {
        return executeApiCall(
            operation = "POST /wiki/v1/wikis/$projectId/pages",
            expectedStatusCode = HttpStatusCode.Created,
            successMessage = "✅ 위키 페이지 생성 성공"
        ) { httpClient.post("/wiki/v1/wikis/$projectId/pages") { setBody(request) } }
    }

    override suspend fun updateWikiPage(
        projectId: String,
        pageId: String,
        request: UpdateWikiPageRequest
    ): WikiPageResponse {
        return executeApiCall(
            operation = "PUT /wiki/v1/wikis/$projectId/pages/$pageId",
            successMessage = "✅ 위키 페이지 수정 성공"
        ) { httpClient.put("/wiki/v1/wikis/$projectId/pages/$pageId") { setBody(request) } }
    }

    override suspend fun deleteWikiPage(
        projectId: String,
        pageId: String
    ): DoorayApiResponse<Unit> {
        return executeApiCallWithoutBody(
            operation = "DELETE /wiki/v1/wikis/$projectId/pages/$pageId",
            successMessage = "✅ 위키 페이지 삭제 성공"
        ) { httpClient.delete("/wiki/v1/wikis/$projectId/pages/$pageId") }
    }

    override suspend fun getWikiPageVersions(
        projectId: String,
        pageId: String
    ): WikiPageVersionsResponse {
        return executeApiCall(
            operation = "GET /wiki/v1/wikis/$projectId/pages/$pageId/versions",
            successMessage = "✅ 위키 페이지 버전 목록 조회 성공"
        ) { httpClient.get("/wiki/v1/wikis/$projectId/pages/$pageId/versions") }
    }

    override suspend fun getWikiPageVersion(
        projectId: String,
        pageId: String,
        version: Int
    ): WikiPageResponse {
        return executeApiCall(
            operation = "GET /wiki/v1/wikis/$projectId/pages/$pageId/versions/$version",
            successMessage = "✅ 위키 페이지 버전 조회 성공"
        ) { httpClient.get("/wiki/v1/wikis/$projectId/pages/$pageId/versions/$version") }
    }

    override suspend fun searchWikiPages(
        projectId: String,
        query: String,
        size: Int?,
        page: Int?
    ): WikiSearchResponse {
        return executeApiCall(
            operation = "GET /wiki/v1/wikis/$projectId/pages/search?q=$query",
            successMessage = "✅ 위키 페이지 검색 성공"
        ) {
            httpClient.get("/wiki/v1/wikis/$projectId/pages/search") {
                parameter("q", query)
                size?.let { parameter("size", it) }
                page?.let { parameter("page", it) }
            }
        }
    }
}
