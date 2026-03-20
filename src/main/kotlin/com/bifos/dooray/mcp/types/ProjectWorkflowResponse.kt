package com.bifos.dooray.mcp.types

import kotlinx.serialization.Serializable

/** 프로젝트 워크플로우 상태 정보 */
@Serializable
data class ProjectWorkflow(
    val id: String,
    val name: String,
    val `class`: String,  // backlog, registered, working, closed
    val order: Int? = null
)

/** 프로젝트 워크플로우 목록 응답 */
@Serializable
data class ProjectWorkflowListResponse(
    val header: DoorayApiHeader,
    val result: List<ProjectWorkflow>
)
