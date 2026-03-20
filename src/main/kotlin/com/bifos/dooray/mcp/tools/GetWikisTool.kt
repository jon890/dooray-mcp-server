package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.client.DoorayClient
import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Tool
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

fun getWikisTool(): Tool {
    return Tool(
        name = "dooray_wiki_list_projects",
        description = "두레이에서 접근 가능한 위키 프로젝트 목록을 조회합니다. 특정 프로젝트의 이름으로 프로젝트 ID를 찾을 때 사용하세요.",
        inputSchema =
            ToolSchema(
                properties =
                    buildJsonObject {
                        paginationProperties(defaultSize = 200)
                    }
            ),
        outputSchema = null,
        annotations = null
    )
}

fun getWikisHandler(doorayClient: DoorayClient): suspend (ClientConnection, CallToolRequest) -> CallToolResult {
    return { _, request ->
        toolHandler {
            val page = request.arguments?.get("page")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            val size = request.arguments?.get("size")?.jsonPrimitive?.content?.toIntOrNull() ?: 200

            val response = doorayClient.getWikis(page, size)

            if (response.header.isSuccessful) {
                val pageInfo = if (page == 0) "첫 번째 페이지" else "${page + 1}번째 페이지"
                val nextStepHint =
                    if (response.result.isNotEmpty()) {
                        "\n\n💡 다음 단계: 특정 프로젝트의 위키 페이지들을 보려면 dooray_wiki_list_pages를 사용하세요."
                    } else {
                        if (page == 0) "\n\n📋 조회 결과가 없습니다. 접근 권한을 확인해주세요."
                        else "\n\n📄 더 이상 프로젝트가 없습니다."
                    }
                successResult(
                    data = response.result,
                    message = "📚 두레이 위키 프로젝트 목록을 성공적으로 조회했습니다 ($pageInfo, 총 ${response.result.size}개)$nextStepHint"
                )
            } else {
                apiErrorResult(response.header)
            }
        }
    }
}
