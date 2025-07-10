package com.bifos.dooray.mcp

import com.bifos.dooray.mcp.types.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*

/** 위키 관련 통합 테스트 */
class WikiIntegrationTest : BaseIntegrationTest() {

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

    @Test
    @DisplayName("특정 위키 페이지의 상세 정보가 조회된다")
    fun getWikiPageTest() = runTest {
        // given
        val pagesResponse = doorayClient.getWikiPages(testProjectId)
        assertTrue(pagesResponse.result.isNotEmpty(), "테스트할 위키 페이지가 없습니다.")

        val pageId = pagesResponse.result.first().id

        // when
        val response = doorayClient.getWikiPage(testProjectId, pageId)

        // then
        assertAll(
                { assertTrue { response.header.isSuccessful } },
                { assertEquals(response.header.resultCode, 0) }
        )

        response.result.let { page ->
            assertNotNull(page.id)
            assertNotNull(page.wikiId)
            assertNotNull(page.subject)
            assertNotNull(page.creator)
            assertNotNull(page.body)
            assertNotNull(page.createdAt)
            assertEquals(pageId, page.id)
        }
        println("✅ 위키 페이지 상세 조회 성공: ${response.result.subject}")
    }

    @Test
    @DisplayName("새로운 위키 페이지를 생성할 수 있다")
    fun createWikiPageTest() = runTest {
        // given
        val request =
                CreateWikiPageRequest(
                        subject = "[테스트] 새로운 위키 페이지 ${System.currentTimeMillis()}",
                        body =
                                WikiPageBody(
                                        mimeType = "text/x-markdown",
                                        content = "# 테스트 위키 페이지\n\n이것은 테스트용 위키 페이지입니다."
                                )
                )

        // when
        val response = doorayClient.createWikiPage(testWikiId, request)

        // then
        assertAll(
                { assertTrue { response.header.isSuccessful } },
                { assertEquals(response.header.resultCode, 0) }
        )

        response.result.let { page ->
            assertNotNull(page.id)
            assertNotNull(page.wikiId)
            assertEquals(testWikiId, page.wikiId)
            createdWikiPageIds.add(page.id)
        }
        println("✅ 위키 페이지 생성 성공: ${response.result.id}")
    }

    @Test
    @DisplayName("위키 페이지를 수정할 수 있다")
    fun updateWikiPageTest() = runTest {
        // given - 먼저 테스트용 위키 페이지를 생성
        val createRequest =
                CreateWikiPageRequest(
                        subject = "[테스트] 수정할 위키 페이지 ${System.currentTimeMillis()}",
                        body =
                                WikiPageBody(
                                        mimeType = "text/x-markdown",
                                        content = "# 원본 내용\n\n수정 전 내용입니다."
                                )
                )

        val createResponse = doorayClient.createWikiPage(testWikiId, createRequest)
        assertTrue(createResponse.header.isSuccessful, "테스트용 위키 페이지 생성 실패")

        val createdPageId = createResponse.result.id
        createdWikiPageIds.add(createdPageId)

        // when - 위키 페이지 수정
        val updateRequest =
                UpdateWikiPageRequest(
                        subject = "[테스트] 수정된 위키 페이지 ${System.currentTimeMillis()}",
                        body =
                                WikiPageBody(
                                        mimeType = "text/x-markdown",
                                        content = "# 수정된 내용\n\n수정 후 내용입니다."
                                )
                )

        val updateResponse = doorayClient.updateWikiPage(testWikiId, createdPageId, updateRequest)

        // then
        assertAll(
                { assertTrue { updateResponse.header.isSuccessful } },
                { assertEquals(updateResponse.header.resultCode, 0) }
        )

        println("✅ 위키 페이지 수정 성공")

        // 수정 결과 확인
        val getResponse = doorayClient.getWikiPage(testWikiId, createdPageId)
        assertTrue(getResponse.header.isSuccessful, "수정된 위키 페이지 조회 실패")
        assertTrue(getResponse.result.subject.contains("수정된 위키 페이지"), "제목이 수정되지 않았습니다")
        assertTrue(getResponse.result.body?.content?.contains("수정된 내용") == true, "내용이 수정되지 않았습니다")
    }

    @Test
    @DisplayName("위키 페이지의 제목만 수정할 수 있다")
    fun updateWikiPageTitleTest() = runTest {
        // given - 먼저 테스트용 위키 페이지를 생성
        val createRequest =
                CreateWikiPageRequest(
                        subject = "[테스트] 제목 수정 테스트 ${System.currentTimeMillis()}",
                        body =
                                WikiPageBody(
                                        mimeType = "text/x-markdown",
                                        content = "# 원본 내용\n\n제목만 수정할 예정입니다."
                                )
                )

        val createResponse = doorayClient.createWikiPage(testWikiId, createRequest)
        assertTrue(createResponse.header.isSuccessful, "테스트용 위키 페이지 생성 실패")

        val createdPageId = createResponse.result.id
        createdWikiPageIds.add(createdPageId)

        // when - 제목만 수정
        val newSubject = "[테스트] 제목만 수정됨 ${System.currentTimeMillis()}"
        val updateResponse = doorayClient.updateWikiPageTitle(testWikiId, createdPageId, newSubject)

        // then
        assertAll(
                { assertTrue { updateResponse.header.isSuccessful } },
                { assertEquals(updateResponse.header.resultCode, 0) }
        )

        println("✅ 위키 페이지 제목 수정 성공")

        // 수정 결과 확인
        val getResponse = doorayClient.getWikiPage(testWikiId, createdPageId)
        assertTrue(getResponse.header.isSuccessful, "수정된 위키 페이지 조회 실패")
        assertEquals(newSubject, getResponse.result.subject, "제목이 올바르게 수정되지 않았습니다")
    }

    @Test
    @DisplayName("위키 페이지의 내용만 수정할 수 있다")
    fun updateWikiPageContentTest() = runTest {
        // given - 먼저 테스트용 위키 페이지를 생성
        val createRequest =
                CreateWikiPageRequest(
                        subject = "[테스트] 내용 수정 테스트 ${System.currentTimeMillis()}",
                        body =
                                WikiPageBody(
                                        mimeType = "text/x-markdown",
                                        content = "# 원본 내용\n\n내용만 수정할 예정입니다."
                                )
                )

        val createResponse = doorayClient.createWikiPage(testWikiId, createRequest)
        assertTrue(createResponse.header.isSuccessful, "테스트용 위키 페이지 생성 실패")

        val createdPageId = createResponse.result.id
        createdWikiPageIds.add(createdPageId)

        // when - 내용만 수정
        val newContent = "# 수정된 내용\n\n내용만 수정되었습니다. ${System.currentTimeMillis()}"
        val updateResponse =
                doorayClient.updateWikiPageContent(testWikiId, createdPageId, newContent)

        // then
        assertAll(
                { assertTrue { updateResponse.header.isSuccessful } },
                { assertEquals(updateResponse.header.resultCode, 0) }
        )

        println("✅ 위키 페이지 내용 수정 성공")

        // 수정 결과 확인
        val getResponse = doorayClient.getWikiPage(testWikiId, createdPageId)
        assertTrue(getResponse.header.isSuccessful, "수정된 위키 페이지 조회 실패")
        assertEquals(newContent, getResponse.result.body?.content, "내용이 올바르게 수정되지 않았습니다")
    }
}
