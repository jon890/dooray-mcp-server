package com.bifos.dooray.mcp.types

import kotlinx.serialization.Serializable

/** 프로젝트 멤버 정보 */
@Serializable
data class ProjectMember(
    val organizationMemberId: String,
    val name: String? = null,
    val emailAddress: String? = null,
    val department: String? = null,
    val position: String? = null,
    val role: String? = null
)

/** 프로젝트 멤버 목록 응답 */
@Serializable
data class ProjectMemberListResponse(
    val header: DoorayApiHeader,
    val result: List<ProjectMember>,
    val totalCount: Int? = null
)
