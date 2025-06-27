package com.bifos.dooray.mcp.client

import com.bifos.dooray.mcp.exception.CustomException
import com.bifos.dooray.mcp.types.DoorayErrorResponse
import com.bifos.dooray.mcp.types.WikiPagesResponse
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

class DoorayHttpClient(private val baseUrl: String, private val doorayApiKey: String) : DoorayClient {

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
                logger = Logger.SIMPLE
                level = LogLevel.ALL
            }
        }
    }

    override suspend fun getWikiPages(projectId: String): WikiPagesResponse {
        try {
            println("🔗 API 요청: GET /wiki/v1/wikis/$projectId/pages")
            val response: HttpResponse = httpClient.get("/wiki/v1/wikis/$projectId/pages")

            println("📡 응답 수신: ${response.status} ${response.status.description}")

            return when (response.status) {
                HttpStatusCode.OK -> {
                    val result = response.body<WikiPagesResponse>()
                    println("✅ 성공적으로 파싱: ${result.result?.size ?: 0}개 페이지")
                    result
                }

                else -> {
                    val responseBody = response.bodyAsText()
                    println("❌ API 오류 응답:")
                    println("  상태 코드: ${response.status.value} ${response.status.description}")
                    println("  응답 본문: $responseBody")

                    try {
                        val errorResponse = response.body<DoorayErrorResponse>()
                        val errorMessage = "API 호출 실패: ${errorResponse.header.resultMessage}"
                        throw CustomException(errorMessage, response.status.value)
                    } catch (parseException: Exception) {
                        val errorMessage = "API 응답 파싱 실패 (${response.status.value}): $responseBody"
                        throw CustomException(errorMessage, response.status.value, parseException)
                    }
                }
            }
        } catch (e: CustomException) {
            // CustomException은 그대로 다시 던지기
            throw e
        } catch (e: Exception) {
            println("❌ 네트워크 또는 기타 오류:")
            println("  타입: ${e::class.simpleName}")
            println("  메시지: ${e.message}")
            e.printStackTrace()

            val errorMessage = "API 호출 중 오류 발생: ${e.message}"
            throw CustomException(errorMessage, null, e)
        }
    }

    override suspend fun getWikiPages(projectId: String, parentPageId: String): WikiPagesResponse {
        TODO("Not yet implemented")
    }
}
