package com.bifos.dooray.mcp.client

import com.bifos.dooray.mcp.exception.CustomException
import com.bifos.dooray.mcp.types.DoorayErrorResponse
import com.bifos.dooray.mcp.types.WikiPagesResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.get
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
                headers { append("Authorization", "dooray-api $doorayApiKey") }
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
        val response: HttpResponse = httpClient.get("/wiki/v1/wikis/$projectId/pages")

        return when (response.status) {
            HttpStatusCode.OK -> {
                response.body<WikiPagesResponse>()
            }

            else -> {
                val errorResponse = response.body<DoorayErrorResponse>()
                throw CustomException(errorResponse.header.resultMessage, HttpStatusCode.Unauthorized.value)
            }
        }
    }

    override suspend fun getWikiPages(
        projectId: String,
        parentPageId: String
    ): WikiPagesResponse {
        TODO("Not yet implemented")
    }
}