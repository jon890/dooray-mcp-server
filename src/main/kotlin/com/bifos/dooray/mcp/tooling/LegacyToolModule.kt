package com.bifos.dooray.mcp.tooling

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.service.ProjectResolver
import com.bifos.dooray.mcp.tools.*

class LegacyToolModule(
    private val doorayClient: DoorayClient,
) : ToolModule {
    override val toolSet: ToolSet = ToolSet.LEGACY

    override fun bindings(): List<ToolBinding> {
        val projectResolver = ProjectResolver(doorayClient)
        return listOf(
            ToolBinding(getWikisTool(), getWikisHandler(doorayClient)),
            ToolBinding(getWikiPagesTool(), getWikiPagesHandler(doorayClient)),
            ToolBinding(getWikiPageTool(), getWikiPageHandler(doorayClient)),
            ToolBinding(createWikiPageTool(), createWikiPageHandler(doorayClient)),
            ToolBinding(updateWikiPageTool(), updateWikiPageHandler(doorayClient)),
            ToolBinding(getWikiPageCommentsTool(), getWikiPageCommentsHandler(doorayClient)),
            ToolBinding(getProjectsTool(), getProjectsHandler(doorayClient, projectResolver)),
            ToolBinding(getProjectMembersTool(), getProjectMembersHandler(doorayClient, projectResolver)),
            ToolBinding(getProjectWorkflowsTool(), getProjectWorkflowsHandler(doorayClient, projectResolver)),
            ToolBinding(getProjectPostsTool(), getProjectPostsHandler(doorayClient, projectResolver)),
            ToolBinding(getProjectPostTool(), getProjectPostHandler(doorayClient, projectResolver)),
            ToolBinding(createProjectPostTool(), createProjectPostHandler(doorayClient, projectResolver)),
            ToolBinding(updateProjectPostTool(), updateProjectPostHandler(doorayClient, projectResolver)),
            ToolBinding(setProjectPostWorkflowTool(), setProjectPostWorkflowHandler(doorayClient, projectResolver)),
            ToolBinding(setProjectPostDoneTool(), setProjectPostDoneHandler(doorayClient, projectResolver)),
            ToolBinding(setProjectPostParentTool(), setProjectPostParentHandler(doorayClient, projectResolver)),
            ToolBinding(createPostCommentTool(), createPostCommentHandler(doorayClient, projectResolver)),
            ToolBinding(getPostCommentsTool(), getPostCommentsHandler(doorayClient, projectResolver)),
            ToolBinding(updatePostCommentTool(), updatePostCommentHandler(doorayClient, projectResolver)),
            ToolBinding(deletePostCommentTool(), deletePostCommentHandler(doorayClient, projectResolver)),
        )
    }

    companion object {
        val TOOL_NAMES = listOf(
            "dooray_wiki_list_projects",
            "dooray_wiki_list_pages",
            "dooray_wiki_get_page",
            "dooray_wiki_create_page",
            "dooray_wiki_update_page",
            "dooray_wiki_get_page_comments",
            "dooray_project_list_projects",
            "dooray_project_list_members",
            "dooray_project_list_workflows",
            "dooray_project_list_posts",
            "dooray_project_get_post",
            "dooray_project_create_post",
            "dooray_project_update_post",
            "dooray_project_set_post_workflow",
            "dooray_project_set_post_done",
            "dooray_project_set_post_parent",
            "dooray_project_create_post_comment",
            "dooray_project_get_post_comments",
            "dooray_project_update_post_comment",
            "dooray_project_delete_post_comment",
        )
    }
}
