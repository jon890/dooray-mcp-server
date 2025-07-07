package com.bifos.dooray.mcp.types

import kotlinx.serialization.Serializable

/** Dooray API 공통 응답 래퍼 */
@Serializable data class DoorayApiResponse<T>(val header: DoorayApiHeader, val result: T)

/** Dooray API nullable result 응답 래퍼 (result가 null일 수 있는 경우) */
@Serializable data class DoorayApiNullableResponse<T>(val header: DoorayApiHeader, val result: T?)

/** Dooray API Unit 응답 (result가 없거나 null인 성공 응답용) */
typealias DoorayApiUnitResponse = DoorayApiNullableResponse<Unit>

/** Dooray API 응답 헤더 */
@Serializable
data class DoorayApiHeader(
        val isSuccessful: Boolean,
        val resultCode: Int,
        val resultMessage: String
)

@Serializable data class ProjectOrganization(val id: String?)

@Serializable data class ProjectDrive(val id: String?)

@Serializable data class ProjectWiki(val id: String?)

@Serializable
data class Project(
        val id: String,
        val code: String,
        val description: String? = null,
        val state: String? = null,
        val scope: String? = null,
        val type: String? = null,
        val organization: ProjectOrganization? = null,
        val drive: ProjectDrive? = null,
        val wiki: ProjectWiki? = null
)

@Serializable data class ProjectListResult(val result: List<Project>, val totalCount: Int)

@Serializable
data class ProjectListResponse(
        val header: DoorayApiHeader,
        val result: List<Project>,
        val totalCount: Int
)
