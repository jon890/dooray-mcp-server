package com.bifos.dooray.mcp.service

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.constants.EnvVariableConst.DOORAY_PROJECT_CACHE_TTL_MINUTES
import com.bifos.dooray.mcp.exception.ToolException
import com.bifos.dooray.mcp.types.Project
import com.bifos.dooray.mcp.utils.Env
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class ProjectResolver(private val doorayClient: DoorayClient) {

    private val log = LoggerFactory.getLogger(ProjectResolver::class.java)
    private val cacheByCode = ConcurrentHashMap<String, Project>() // lowercase code → Project
    private val cacheById = ConcurrentHashMap<String, Project>()   // id → Project
    private val mutex = Mutex()
    private var lastRefreshTime: Instant = Instant.EPOCH
    private val ttl: Duration = Duration.ofMinutes(
        Env.getLong(DOORAY_PROJECT_CACHE_TTL_MINUTES, default = 5L)
    )

    suspend fun resolveProjectId(input: String): String {
        // If it looks like a numeric ID and is in cache, return it directly
        if (cacheById.containsKey(input)) {
            return input
        }

        // Try to find by code (case-insensitive)
        val lowered = input.lowercase()
        val cached = cacheByCode[lowered]
        if (cached != null && !isCacheExpired()) {
            log.debug("ProjectResolver: resolved code '{}' -> id '{}'", input, cached.id)
            return cached.id
        }

        // Cache miss or expired — refresh once under mutex
        mutex.withLock {
            // Double-check after acquiring lock
            val afterLock = cacheByCode[lowered]
            if (afterLock != null && !isCacheExpired()) {
                log.debug("ProjectResolver: resolved code '{}' -> id '{}' (after lock)", input, afterLock.id)
                return afterLock.id
            }
            refreshCacheInternal()
        }

        // If input is a numeric-style ID, check again after refresh
        val byId = cacheById[input]
        if (byId != null) {
            return input
        }

        // Retry by code after refresh
        val resolved = cacheByCode[lowered]
        if (resolved != null) {
            log.debug("ProjectResolver: resolved code '{}' -> id '{}' (after refresh)", input, resolved.id)
            return resolved.id
        }

        // Not found — throw with helpful message listing available codes
        val availableCodes = cacheByCode.keys.sorted().joinToString(", ")
        throw ToolException(
            type = ToolException.VALIDATION_ERROR,
            message = "프로젝트를 찾을 수 없습니다: '$input'. 사용 가능한 프로젝트 코드: [$availableCodes]. dooray_project_list_projects로 전체 목록을 확인하세요.",
            code = "PROJECT_NOT_FOUND"
        )
    }

    suspend fun refreshCache() {
        mutex.withLock {
            refreshCacheInternal()
        }
    }

    private suspend fun refreshCacheInternal() {
        log.debug("ProjectResolver: refreshing project cache...")
        val allProjects = mutableListOf<Project>()
        var page = 0
        val pageSize = 100

        while (true) {
            val response = doorayClient.getProjects(page = page, size = pageSize)
            if (!response.header.isSuccessful) {
                log.warn("ProjectResolver: failed to fetch projects page {}: {}", page, response.header.resultMessage)
                break
            }
            allProjects.addAll(response.result)
            if (response.result.size < pageSize) break
            page++
        }

        updateCacheInternal(allProjects)
        lastRefreshTime = Instant.now()
        log.debug("ProjectResolver: cache refreshed with {} projects", allProjects.size)
    }

    fun updateCache(projects: List<Project>) {
        updateCacheInternal(projects)
        if (lastRefreshTime == Instant.EPOCH) {
            lastRefreshTime = Instant.now()
        }
    }

    private fun updateCacheInternal(projects: List<Project>) {
        projects.forEach { project ->
            cacheById[project.id] = project
            cacheByCode[project.code.lowercase()] = project
        }
    }

    private fun isCacheExpired(): Boolean =
        Instant.now().isAfter(lastRefreshTime.plus(ttl))
}
