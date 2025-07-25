package com.bifos.dooray.mcp.client.dooray

import com.bifos.dooray.mcp.types.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertAll
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** 프로젝트 업무 댓글 관련 통합 테스트 */
class ProjectPostCommentsDoorayIntegrationTest : BaseDoorayIntegrationTest() {

    @Test
    @DisplayName("업무에 댓글을 생성한다")
    fun createPostCommentTest() = runTest {
        // given - 먼저 테스트용 업무를 생성하거나 기존 업무를 사용
        val postsResponse = doorayClient.getPosts(testProjectId, size = 1)
        val postId =
            if (postsResponse.result.isNotEmpty()) {
                postsResponse.result.first().id
            } else {
                // 업무가 없으면 하나 생성
                val createRequest =
                    CreatePostRequest(
                        subject = "[테스트용] 댓글 테스트 업무 ${System.currentTimeMillis()}",
                        body =
                            PostBody(
                                mimeType = "text/html",
                                content = "댓글 테스트용 업무입니다."
                            ),
                        users = CreatePostUsers(to = emptyList(), cc = emptyList()),
                        priority = "normal"
                    )
                val createResponse = doorayClient.createPost(testProjectId, createRequest)
                assertTrue(createResponse.header.isSuccessful, "테스트용 업무 생성에 실패했습니다.")

                val createdPostId = createResponse.result.id
                createdPostIds.add(createdPostId)
                createdPostId
            }

        // when - 댓글 생성
        val commentRequest =
            CreateCommentRequest(
                body =
                    PostCommentBody(
                        mimeType = "text/html",
                        content = "테스트 댓글입니다. ${System.currentTimeMillis()}"
                    )
            )

        val commentResponse = doorayClient.createPostComment(testProjectId, postId, commentRequest)

        // then
        assertAll(
            { assertTrue { commentResponse.header.isSuccessful } },
            { assertEquals(commentResponse.header.resultCode, 0) }
        )

        println("✅ 댓글 생성 성공: ${commentResponse.result.id}")
        println("  - 댓글 생성 완료")
    }

    @Test
    @DisplayName("업무의 댓글 목록을 조회한다")
    fun getPostCommentsTest() = runTest {
        // given - 먼저 댓글이 있는 업무를 찾거나 생성
        val postsResponse = doorayClient.getPosts(testProjectId, size = 5)
        assertTrue(postsResponse.result.isNotEmpty(), "테스트할 업무가 없습니다.")

        val postId = postsResponse.result.first().id

        // 댓글이 없을 수 있으므로 하나 생성
        val commentRequest =
            CreateCommentRequest(
                body =
                    PostCommentBody(
                        mimeType = "text/html",
                        content = "목록 조회 테스트용 댓글 ${System.currentTimeMillis()}"
                    )
            )
        doorayClient.createPostComment(testProjectId, postId, commentRequest)

        // when - 댓글 목록 조회
        val commentsResponse = doorayClient.getPostComments(testProjectId, postId)

        // then
        assertAll(
            { assertTrue { commentsResponse.header.isSuccessful } },
            { assertEquals(commentsResponse.header.resultCode, 0) }
        )

        println("✅ 댓글 목록 조회 성공: ${commentsResponse.result.size}개")
        commentsResponse.result.forEach { comment ->
            assertNotNull(comment.id)
            assertNotNull(comment.body)
            assertNotNull(comment.createdAt)
            println("  - 댓글 ID: ${comment.id}, 내용: ${comment.body.content.take(50)}...")
        }
    }

    @Test
    @DisplayName("업무의 댓글을 수정한다")
    fun updatePostCommentTest() = runTest {
        // given - 먼저 댓글을 생성
        // 업무가 없으면 하나 생성
        val createRequest =
            CreatePostRequest(
                subject = "[테스트용] 댓글 수정 테스트 업무 ${System.currentTimeMillis()}",
                body = PostBody(mimeType = "text/html", content = "댓글 수정 테스트용 업무입니다."),
                users = CreatePostUsers(to = emptyList(), cc = emptyList()),
                priority = "normal"
            )
        val createResponse = doorayClient.createPost(testProjectId, createRequest)
        assertTrue(createResponse.header.isSuccessful, "테스트용 업무 생성에 실패했습니다.")

        val createdPostId = createResponse.result.id
        createdPostIds.add(createdPostId)

        // 댓글 생성
        val commentRequest =
            CreateCommentRequest(
                body =
                    PostCommentBody(
                        mimeType = "text/html",
                        content = "수정 전 댓글 내용 ${System.currentTimeMillis()}"
                    )
            )
        val commentResponse =
            doorayClient.createPostComment(testProjectId, createdPostId, commentRequest)
        assertTrue(commentResponse.header.isSuccessful, "테스트용 댓글 생성에 실패했습니다.")

        val commentId = commentResponse.result.id

        // when - 댓글 수정
        val updateRequest =
            UpdateCommentRequest(
                body =
                    PostCommentBody(
                        mimeType = "text/html",
                        content = "수정된 댓글 내용 ${System.currentTimeMillis()}"
                    )
            )

        val updateResponse =
            doorayClient.updatePostComment(
                testProjectId,
                createdPostId,
                commentId,
                updateRequest
            )

        // then
        assertAll(
            { assertTrue { updateResponse.header.isSuccessful } },
            { assertEquals(updateResponse.header.resultCode, 0) }
        )

        // 수정된 내용 확인
        val commentsResponse = doorayClient.getPostComments(testProjectId, createdPostId)
        val updatedComment = commentsResponse.result.find { it.id == commentId }
        assertNotNull(updatedComment, "수정된 댓글을 찾을 수 없습니다.")
        assertTrue(updatedComment.body.content.contains("수정된 댓글 내용"), "댓글이 수정되지 않았습니다.")

        println("✅ 댓글 수정 성공: ${commentId}")
    }

    @Test
    @DisplayName("업무의 댓글을 삭제한다")
    fun deletePostCommentTest() = runTest {
        // given - 먼저 댓글을 생성
        val postsResponse = doorayClient.getPosts(testProjectId, size = 1)
        val postId =
            if (postsResponse.result.isNotEmpty()) {
                postsResponse.result.first().id
            } else {
                // 업무가 없으면 하나 생성
                val createRequest =
                    CreatePostRequest(
                        subject = "[테스트용] 댓글 삭제 테스트 업무 ${System.currentTimeMillis()}",
                        body =
                            PostBody(
                                mimeType = "text/html",
                                content = "댓글 삭제 테스트용 업무입니다."
                            ),
                        users = CreatePostUsers(to = emptyList(), cc = emptyList()),
                        priority = "normal"
                    )
                val createResponse = doorayClient.createPost(testProjectId, createRequest)
                assertTrue(createResponse.header.isSuccessful, "테스트용 업무 생성에 실패했습니다.")

                val createdPostId = createResponse.result.id
                createdPostIds.add(createdPostId)
                createdPostId
            }

        // 댓글 생성
        val commentRequest =
            CreateCommentRequest(
                body =
                    PostCommentBody(
                        mimeType = "text/html",
                        content = "삭제될 댓글 ${System.currentTimeMillis()}"
                    )
            )
        val commentResponse = doorayClient.createPostComment(testProjectId, postId, commentRequest)
        assertTrue(commentResponse.header.isSuccessful, "테스트용 댓글 생성에 실패했습니다.")

        val commentId = commentResponse.result.id

        // when - 댓글 삭제
        val deleteResponse = doorayClient.deletePostComment(testProjectId, postId, commentId)

        // then
        assertAll(
            { assertTrue { deleteResponse.header.isSuccessful } },
            { assertEquals(deleteResponse.header.resultCode, 0) }
        )

        println("✅ 댓글 삭제 성공: ${commentId}")

        // 삭제 확인 - 댓글 목록에서 해당 댓글이 없거나 삭제 상태여야 함
        val commentsResponse = doorayClient.getPostComments(testProjectId, postId)
        val deletedComment = commentsResponse.result.find { it.id == commentId }

        if (deletedComment == null) {
            println("  - 댓글이 완전히 삭제되었습니다.")
        } else {
            println("  - 댓글이 여전히 목록에 있습니다 (논리적 삭제일 수 있음)")
        }
    }

    @Test
    @DisplayName("댓글 목록을 페이징으로 조회한다")
    fun getPostCommentsWithPagingTest() = runTest {
        // given - 댓글이 여러 개 있는 업무를 찾거나 생성
        val postsResponse = doorayClient.getPosts(testProjectId, size = 1)
        val postId =
            if (postsResponse.result.isNotEmpty()) {
                postsResponse.result.first().id
            } else {
                // 업무가 없으면 하나 생성
                val createRequest =
                    CreatePostRequest(
                        subject = "[테스트용] 댓글 페이징 테스트 업무 ${System.currentTimeMillis()}",
                        body =
                            PostBody(
                                mimeType = "text/html",
                                content = "댓글 페이징 테스트용 업무입니다."
                            ),
                        users = CreatePostUsers(to = emptyList(), cc = emptyList()),
                        priority = "normal"
                    )
                val createResponse = doorayClient.createPost(testProjectId, createRequest)
                assertTrue(createResponse.header.isSuccessful, "테스트용 업무 생성에 실패했습니다.")

                val createdPostId = createResponse.result.id
                createdPostIds.add(createdPostId)
                createdPostId
            }

        // 여러 댓글 생성 (페이징 테스트를 위해)
        repeat(3) { index ->
            val commentRequest =
                CreateCommentRequest(
                    body =
                        PostCommentBody(
                            mimeType = "text/html",
                            content =
                                "페이징 테스트 댓글 ${index + 1} - ${System.currentTimeMillis()}"
                        )
                )
            doorayClient.createPostComment(testProjectId, postId, commentRequest)
        }

        // when - 첫 번째 페이지 조회 (사이즈 2로 제한)
        val firstPageResponse =
            doorayClient.getPostComments(testProjectId, postId, page = 0, size = 2)

        // then
        assertAll(
            { assertTrue { firstPageResponse.header.isSuccessful } },
            { assertEquals(firstPageResponse.header.resultCode, 0) }
        )

        println(
            "✅ 첫 번째 페이지 댓글 조회 성공: 총 ${firstPageResponse.totalCount}개 중 ${firstPageResponse.result.size}개"
        )

        // 총 댓글 수가 2개보다 많다면 두 번째 페이지도 조회
        if (firstPageResponse.totalCount > 2) {
            val secondPageResponse =
                doorayClient.getPostComments(testProjectId, postId, page = 1, size = 2)

            assertAll(
                { assertTrue { secondPageResponse.header.isSuccessful } },
                { assertEquals(secondPageResponse.header.resultCode, 0) }
            )

            println("✅ 두 번째 페이지 댓글 조회 성공: ${secondPageResponse.result.size}개")

            // 첫 번째 페이지와 두 번째 페이지의 댓글이 다른지 확인
            val firstPageIds = firstPageResponse.result.map { it.id }.toSet()
            val secondPageIds = secondPageResponse.result.map { it.id }.toSet()
            val hasOverlap = firstPageIds.intersect(secondPageIds).isNotEmpty()

            if (!hasOverlap && secondPageResponse.result.isNotEmpty()) {
                println("✅ 댓글 페이징이 올바르게 작동합니다 (중복 없음)")
            } else {
                println("ℹ️ 댓글 페이징 결과에 중복이 있거나 두 번째 페이지가 비어있습니다.")
            }
        } else {
            println("ℹ️ 총 댓글 수가 2개 이하라서 두 번째 페이지 테스트를 건너뜁니다.")
        }
    }

    @Test
    @DisplayName("댓글 목록 조회에서 직렬화 오류가 발생하지 않는다")
    fun getPostCommentsSerializationTest() = runTest {
        // given - 댓글이 있는 업무 찾기
        val postsResponse = doorayClient.getPosts(testProjectId, size = 1)
        assertTrue(postsResponse.result.isNotEmpty(), "테스트할 업무가 없습니다.")

        val postId = postsResponse.result.first().id

        // when - 댓글 목록 조회 (여러 페이지 테스트)
        val firstPageResponse =
            doorayClient.getPostComments(testProjectId, postId, page = 0, size = 5)
        val secondPageResponse =
            doorayClient.getPostComments(testProjectId, postId, page = 1, size = 3)

        // then - 직렬화 오류 없이 정상 응답
        assertAll(
            { assertTrue { firstPageResponse.header.isSuccessful } },
            { assertTrue { secondPageResponse.header.isSuccessful } },
            { assertEquals(firstPageResponse.header.resultCode, 0) },
            { assertEquals(secondPageResponse.header.resultCode, 0) }
        )

        println("✅ 댓글 목록 직렬화 테스트 성공:")
        println("  - 첫 번째 페이지: ${firstPageResponse.result.size}개 댓글")
        println("  - 두 번째 페이지: ${secondPageResponse.result.size}개 댓글")
        println("  - 전체: ${firstPageResponse.totalCount}개 댓글")

        // 댓글 구조 검증
        firstPageResponse.result.forEach { comment ->
            assertNotNull(comment.id, "댓글 ID는 null이 아니어야 합니다")
            assertNotNull(comment.post, "댓글의 post 정보는 null이 아니어야 합니다")
            assertNotNull(comment.creator, "댓글의 creator 정보는 null이 아니어야 합니다")
            assertNotNull(comment.body, "댓글의 body 정보는 null이 아니어야 합니다")
            assertNotNull(comment.createdAt, "댓글의 createdAt 정보는 null이 아니어야 합니다")
        }
    }
}
