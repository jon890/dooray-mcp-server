# Dooray MCP Server - Agent Documentation

This document provides a comprehensive guide to the dooray-mcp-server codebase structure, components, and architecture for AI agents and developers.

## Table of Contents
1. [Project Overview](#project-overview)
2. [Directory Structure](#directory-structure)
3. [Core Components](#core-components)
4. [MCP Tools Reference](#mcp-tools-reference)
5. [Architecture Layers](#architecture-layers)
6. [Key Files Guide](#key-files-guide)
7. [Testing Strategy](#testing-strategy)
8. [Build & Deployment](#build--deployment)

## Project Overview

**dooray-mcp-server** is a Kotlin-based MCP (Model Context Protocol) server that provides Claude and other AI models with tools to interact with Dooray services (project management and wiki platform).

### Key Features
- 15 MCP tools for project and wiki management
- HTTP client for Dooray API integration
- Type-safe response handling with Kotlin data classes
- Integration and unit tests
- Docker containerization support
- GitHub Actions CI/CD pipelines

### Technology Stack
- **Language:** Kotlin
- **Build Tool:** Gradle (Kotlin DSL)
- **Runtime:** JVM
- **Testing:** JUnit, Integration tests
- **Containerization:** Docker
- **CI/CD:** GitHub Actions

---

## Directory Structure

### Root Level Files
```
dooray-mcp-server/
├── build.gradle.kts                 # Gradle build configuration (Kotlin DSL)
├── settings.gradle.kts              # Gradle project settings
├── gradle.properties                # Gradle build properties
├── .env.sample                      # Environment variables template
├── Dockerfile                       # Docker container configuration
├── README.md                        # Project readme
└── AGENTS.md                        # This file
```

### Configuration & Infrastructure
```
.cursor/                             # Cursor IDE configuration
├── rules/                           # Custom Cursor rules

.github/                             # GitHub configuration
├── workflows/
│   ├── main.yml                     # Main branch CI/CD
│   └── pr.yml                       # PR CI/CD

.omc/                                # oh-my-claudecode state
├── project-memory.json
└── state/                           # State tracking files

gradle/                              # Gradle wrapper
└── wrapper/
    └── gradle-wrapper.properties

scripts/                             # Utility scripts
├── docker-build.sh                  # Build Docker image
└── docker-push.sh                   # Push to registry
```

### Source Code Structure
```
src/
├── main/
│   ├── kotlin/com/bifos/dooray/mcp/
│   │   ├── client/                  # Dooray API client layer
│   │   ├── constants/               # Configuration constants
│   │   ├── exception/               # Custom exception classes
│   │   ├── tools/                   # MCP tool implementations
│   │   ├── types/                   # Data type definitions
│   │   ├── utils/                   # Utility functions
│   │   ├── DoorayMcpServer.kt       # Main MCP server
│   │   └── Main.kt                  # Entry point
│   └── resources/
│       └── logback.xml              # Logging configuration

└── test/
    └── kotlin/com/bifos/dooray/mcp/
        ├── client/dooray/           # Integration tests
        ├── tools/                   # Tool unit tests
        ├── util/                    # Test utilities
        └── ClientStdio.kt           # Test client
```

### Documentation
```
docs/                                # API documentation
├── project-api.md                   # Dooray Project API
├── project-post-comment.md          # Post comments API
├── wiki-api.md                      # Wiki API
└── wiki-create-api.md               # Wiki creation API
```

---

## Core Components

### 1. Client Layer (`src/main/kotlin/com/bifos/dooray/mcp/client/`)

#### DoorayClient.kt
- **Purpose:** Interface definition for Dooray API client
- **Type:** Kotlin interface/trait
- **Methods:** Defines contracts for all API operations
- **Usage:** Implemented by DoorayHttpClient

#### DoorayHttpClient.kt
- **Purpose:** Concrete HTTP implementation of Dooray API client
- **Type:** Kotlin class implementing DoorayClient
- **Responsibilities:**
  - HTTP request handling
  - API endpoint communication
  - Response parsing and type conversion
  - Error handling and exception mapping
- **Key Methods:**
  - Wiki operations (get, list, create, update)
  - Project operations (get, list, create, update)
  - Post comment operations (CRUD)
  - Workflow state management

### 2. Constants Layer (`src/main/kotlin/com/bifos/dooray/mcp/constants/`)

#### EnvVariableConst.kt
- **Purpose:** Environment variable declarations and defaults
- **Contains:**
  - API endpoint URLs
  - Authentication tokens/API keys
  - Timeout configurations
  - Feature flags
- **Usage:** Referenced throughout application for configuration

#### VersionConst.kt
- **Purpose:** Version information
- **Contains:** Application version constant
- **Usage:** Version reporting and compatibility checks

### 3. Exception Handling (`src/main/kotlin/com/bifos/dooray/mcp/exception/`)

#### CustomException.kt
- **Purpose:** Base exception class for application
- **Type:** Sealed class/hierarchy
- **Extends:** Exception
- **Usage:** Base for all custom exceptions

#### ToolException.kt
- **Purpose:** MCP tool-specific exceptions
- **Type:** Exception subclass
- **When Used:** Tool execution failures, validation errors
- **Information Provided:** Error code, message, context

### 4. Tool Layer (`src/main/kotlin/com/bifos/dooray/mcp/tools/`) - 15 MCP Tools

#### Wiki Operations (5 tools)

**GetWikisTool.kt**
- **Purpose:** List all available wikis
- **Input Parameters:** Project filter (optional)
- **Output:** List of wiki objects with metadata
- **API Call:** GET /wikis or similar

**GetWikiPagesTool.kt**
- **Purpose:** List pages within a wiki
- **Input Parameters:** Wiki ID, pagination (optional)
- **Output:** List of wiki page summaries
- **API Call:** GET /wikis/{wiki_id}/pages

**GetWikiPageTool.kt**
- **Purpose:** Retrieve full content of a wiki page
- **Input Parameters:** Wiki ID, Page ID
- **Output:** Complete page content, metadata, version info
- **API Call:** GET /wikis/{wiki_id}/pages/{page_id}

**CreateWikiPageTool.kt**
- **Purpose:** Create new wiki page
- **Input Parameters:** Wiki ID, Title, Content (Markdown), Parent page (optional)
- **Output:** Created page object with ID and metadata
- **API Call:** POST /wikis/{wiki_id}/pages
- **Validation:** Required fields, content length limits

**UpdateWikiPageTool.kt**
- **Purpose:** Update existing wiki page
- **Input Parameters:** Wiki ID, Page ID, New content, Title (optional), References
- **Output:** Updated page object
- **API Call:** PUT /wikis/{wiki_id}/pages/{page_id}
- **Validation:** Page existence, permission checks

#### Project Operations (4 tools)

**GetProjectsTool.kt**
- **Purpose:** List all accessible projects
- **Input Parameters:** State filter (active/archived/deleted), Pagination
- **Output:** List of project objects with metadata
- **API Call:** GET /projects
- **Scope:** User's accessible projects only

**GetProjectPostsTool.kt**
- **Purpose:** List posts/tasks in a project
- **Input Parameters:** Project ID, Workflow status filter, Pagination
- **Output:** List of post summaries
- **API Call:** GET /projects/{project_id}/posts
- **Filters:** Status, assignee, tags, milestone

**GetProjectPostTool.kt**
- **Purpose:** Get detailed information about a single post
- **Input Parameters:** Project ID, Post ID
- **Output:** Complete post object with comments, attachments, workflow state
- **API Call:** GET /projects/{project_id}/posts/{post_id}

**CreateProjectPostTool.kt**
- **Purpose:** Create new project post/task
- **Input Parameters:** Project ID, Title, Description, Assignees, Priority, Tags, Milestone, Due date
- **Output:** Created post object
- **API Call:** POST /projects/{project_id}/posts
- **Validation:** Required fields, assignee existence, tag validation

#### Post Management (4 tools)

**UpdateProjectPostTool.kt**
- **Purpose:** Update post details
- **Input Parameters:** Project ID, Post ID, Fields to update (title, content, assignees, etc.)
- **Output:** Updated post object
- **API Call:** PUT /projects/{project_id}/posts/{post_id}

**SetProjectPostWorkflowTool.kt**
- **Purpose:** Change post workflow status
- **Input Parameters:** Project ID, Post ID, New workflow status ID
- **Output:** Updated post with new status
- **API Call:** POST /projects/{project_id}/posts/{post_id}/workflow
- **Valid States:** Project-dependent workflow states

**SetProjectPostDoneTool.kt**
- **Purpose:** Mark post as complete
- **Input Parameters:** Project ID, Post ID
- **Output:** Post marked as done
- **API Call:** POST /projects/{project_id}/posts/{post_id}/done
- **Effect:** Moves post to terminal "done" state

**GetPostCommentsTool.kt**
- **Purpose:** Retrieve all comments on a post
- **Input Parameters:** Project ID, Post ID, Pagination
- **Output:** List of comment objects with metadata
- **API Call:** GET /projects/{project_id}/posts/{post_id}/comments
- **Includes:** Author, timestamp, content, edit history

#### Comments (3 tools)

**CreatePostCommentTool.kt**
- **Purpose:** Add new comment to post
- **Input Parameters:** Project ID, Post ID, Comment content (Markdown)
- **Output:** Created comment object
- **API Call:** POST /projects/{project_id}/posts/{post_id}/comments

**UpdatePostCommentTool.kt**
- **Purpose:** Edit existing comment
- **Input Parameters:** Project ID, Post ID, Comment ID, New content
- **Output:** Updated comment object
- **API Call:** PUT /projects/{project_id}/posts/{post_id}/comments/{comment_id}

**DeletePostCommentTool.kt**
- **Purpose:** Remove comment from post
- **Input Parameters:** Project ID, Post ID, Comment ID
- **Output:** Confirmation of deletion
- **API Call:** DELETE /projects/{project_id}/posts/{post_id}/comments/{comment_id}

### 5. Type Definitions (`src/main/kotlin/com/bifos/dooray/mcp/types/`)

#### DoorayApiErrorType.kt
- **Purpose:** Error response type definitions
- **Contains:** Error codes, error messages, stack traces
- **Usage:** Standardized error handling across tools

#### DoorayApiSuccessType.kt
- **Purpose:** Success response wrapper
- **Structure:** Generic wrapper for successful API responses
- **Usage:** Uniform response handling

#### ProjectPostResponse.kt
- **Purpose:** Data models for project posts
- **Contains:**
  - Post ID, title, description
  - Assignees, priority, status
  - Dates (created, updated, due)
  - Tags, milestone
  - Workflow state

#### WikiListResponse.kt
- **Purpose:** Response model for wiki list operations
- **Contains:** List of wikis with metadata

#### WikiPageResponse.kt
- **Purpose:** Single wiki page response
- **Contains:** Page ID, title, content, author, created/updated dates

#### WikiPagesResponse.kt
- **Purpose:** Multiple wiki pages response
- **Contains:** List of pages with summary information

#### ToolResponseTypes.kt
- **Purpose:** Generic response types for MCP tools
- **Contains:** Base response wrappers, status codes, message structures

### 6. Utilities (`src/main/kotlin/com/bifos/dooray/mcp/utils/`)

#### JsonUtils.kt
- **Purpose:** JSON serialization/deserialization utilities
- **Provides:**
  - Custom JSON serializers
  - Deserialization helpers
  - Date/time formatting utilities
  - Type-safe JSON parsing

### 7. Main Server (`src/main/kotlin/com/bifos/dooray/mcp/`)

#### DoorayMcpServer.kt
- **Purpose:** Main MCP server implementation
- **Responsibilities:**
  - Tool registration and initialization
  - Request routing to appropriate tools
  - Response marshaling
  - Error handling and reporting
- **Key Methods:**
  - `registerTools()` - Register all 15 MCP tools
  - `handleRequest()` - Process incoming MCP requests
  - `shutdown()` - Graceful server shutdown

#### Main.kt
- **Purpose:** Application entry point
- **Responsibilities:**
  - Server instantiation
  - Configuration loading
  - Server startup
  - Signal handling (graceful shutdown)

---

## MCP Tools Reference

### Tool Registration Map

| Tool Name | Category | Operation | Input | Output |
|-----------|----------|-----------|-------|--------|
| GetWikisTool | Wiki | List | filters | List[Wiki] |
| GetWikiPagesTool | Wiki | List | wiki_id, page | List[WikiPage] |
| GetWikiPageTool | Wiki | Retrieve | wiki_id, page_id | WikiPage |
| CreateWikiPageTool | Wiki | Create | wiki_id, title, body | WikiPage |
| UpdateWikiPageTool | Wiki | Update | wiki_id, page_id, body | WikiPage |
| GetProjectsTool | Project | List | filters | List[Project] |
| GetProjectPostsTool | Project | List | project_id, filters | List[Post] |
| GetProjectPostTool | Project | Retrieve | project_id, post_id | Post |
| CreateProjectPostTool | Project | Create | project_id, params | Post |
| UpdateProjectPostTool | Project | Update | project_id, post_id, params | Post |
| SetProjectPostWorkflowTool | Project | Update State | project_id, post_id, workflow_id | Post |
| SetProjectPostDoneTool | Project | Complete | project_id, post_id | Post |
| GetPostCommentsTool | Comments | List | project_id, post_id | List[Comment] |
| CreatePostCommentTool | Comments | Create | project_id, post_id, content | Comment |
| UpdatePostCommentTool | Comments | Update | project_id, post_id, comment_id, content | Comment |
| DeletePostCommentTool | Comments | Delete | project_id, post_id, comment_id | Confirmation |

### Tool Naming Convention

All tools follow the pattern: `{Verb}{Object}Tool.kt`
- **Verbs:** Get, Create, Update, Delete, Set
- **Objects:** Wiki, WikiPage, Project, ProjectPost, PostComment

---

## Architecture Layers

### Layer 1: Entry Point
```
Main.kt
└── Initializes and starts DoorayMcpServer
```

### Layer 2: MCP Server
```
DoorayMcpServer.kt
├── Tool registration
├── Request routing
└── Response marshaling
```

### Layer 3: Tools Layer
```
15 MCP Tools
├── Parse input parameters
├── Validate inputs
├── Call client methods
└── Format responses
```

### Layer 4: Client Layer
```
DoorayHttpClient.kt
├── HTTP communication
├── Endpoint routing
├── Response parsing
└── Error handling
```

### Layer 5: Data Models
```
types/
├── Request models
├── Response models
├── Error types
└── Serialization helpers
```

### Layer 6: Infrastructure
```
constants/          - Configuration
exception/          - Error handling
utils/              - Utility functions
```

---

## Key Files Guide

### Critical Files for Understanding Flow

1. **Entry Point:**
   - `/Users/nhn/personal/dooray-mcp-server/src/main/kotlin/com/bifos/dooray/mcp/Main.kt`
   - Start here to understand application startup

2. **Core Server:**
   - `/Users/nhn/personal/dooray-mcp-server/src/main/kotlin/com/bifos/dooray/mcp/DoorayMcpServer.kt`
   - Understand MCP server implementation and tool registration

3. **Client Implementation:**
   - `/Users/nhn/personal/dooray-mcp-server/src/main/kotlin/com/bifos/dooray/mcp/client/DoorayHttpClient.kt`
   - Understand how API calls are made and responses parsed

4. **Type System:**
   - `/Users/nhn/personal/dooray-mcp-server/src/main/kotlin/com/bifos/dooray/mcp/types/ToolResponseTypes.kt`
   - Understand data structures and response formats

5. **Configuration:**
   - `/Users/nhn/personal/dooray-mcp-server/src/main/kotlin/com/bifos/dooray/mcp/constants/EnvVariableConst.kt`
   - Understand environment variables and configuration

6. **Error Handling:**
   - `/Users/nhn/personal/dooray-mcp-server/src/main/kotlin/com/bifos/dooray/mcp/exception/ToolException.kt`
   - Understand error types and handling

### Files by Responsibility

**Configuration:**
- `build.gradle.kts` - Build configuration
- `gradle.properties` - Build properties
- `.env.sample` - Environment template
- `src/main/kotlin/com/bifos/dooray/mcp/constants/` - Application constants

**API Integration:**
- `src/main/kotlin/com/bifos/dooray/mcp/client/DoorayHttpClient.kt` - HTTP client
- `docs/` - API documentation

**Tools Implementation:**
- `src/main/kotlin/com/bifos/dooray/mcp/tools/` - All 15 MCP tool implementations

**Type Safety:**
- `src/main/kotlin/com/bifos/dooray/mcp/types/` - Data models and response types

**Testing:**
- `src/test/kotlin/com/bifos/dooray/mcp/client/dooray/` - Integration tests
- `src/test/kotlin/com/bifos/dooray/mcp/tools/McpToolsUnitTest.kt` - Tool tests

**Docker & Deployment:**
- `Dockerfile` - Container definition
- `scripts/docker-build.sh` - Build automation
- `scripts/docker-push.sh` - Push automation
- `.github/workflows/` - CI/CD pipelines

---

## Testing Strategy

### Test Structure
```
src/test/kotlin/com/bifos/dooray/mcp/
├── client/dooray/              # Integration tests
│   ├── BaseDoorayIntegrationTest.kt       # Base test class
│   ├── ProjectPostCommentsDoorayIntegrationTest.kt
│   ├── ProjectPostDoorayIntegrationTest.kt
│   └── WikiDoorayIntegrationTest.kt
├── tools/
│   └── McpToolsUnitTest.kt              # Unit tests for tools
├── util/
│   └── TestUtil.kt                      # Test utilities
└── ClientStdio.kt                       # Test client implementation
```

### Integration Tests
- **Location:** `src/test/kotlin/com/bifos/dooray/mcp/client/dooray/`
- **Base Class:** `BaseDoorayIntegrationTest.kt`
- **Coverage:**
  - Wiki operations
  - Project post operations
  - Comment operations
- **Setup:** Real API calls (requires credentials)

### Unit Tests
- **Location:** `src/test/kotlin/com/bifos/dooray/mcp/tools/`
- **File:** `McpToolsUnitTest.kt`
- **Coverage:** Tool input validation, response formatting

### Test Utilities
- **Location:** `src/test/kotlin/com/bifos/dooray/mcp/util/TestUtil.kt`
- **Provides:** Fixture generation, mock builders, assertion helpers

---

## Build & Deployment

### Build Process

**Build System:** Gradle with Kotlin DSL

**Build Configuration File:**
- `build.gradle.kts` - Main build script with:
  - Dependency declarations
  - Plugin configuration
  - Test settings
  - Custom build tasks

**Build Command:**
```bash
./gradlew build
```

**Artifact Output:**
- JAR file in `build/libs/`
- Test results in `build/test-results/`

### Gradle Wrapper
- **Location:** `gradle/wrapper/`
- **Purpose:** Ensures consistent Gradle version across environments
- **Command:** `./gradlew` (replaces `gradle`)

### Docker Deployment

**Dockerfile:**
- Location: `/Users/nhn/personal/dooray-mcp-server/Dockerfile`
- Builds containerized MCP server
- Includes JVM and application runtime

**Build Script:**
```bash
./scripts/docker-build.sh
```

**Push Script:**
```bash
./scripts/docker-push.sh
```

### CI/CD Pipelines

**Main Workflow (`.github/workflows/main.yml`):**
- Triggers on: Push to main branch
- Jobs:
  - Build
  - Test
  - Docker build and push

**PR Workflow (`.github/workflows/pr.yml`):**
- Triggers on: Pull request creation/update
- Jobs:
  - Build
  - Test
  - Code quality checks

### Configuration

**Environment Variables:**
Template provided in `.env.sample`
- API endpoints
- Authentication credentials
- Logging levels
- Timeout settings

---

## Development Workflow

### Adding a New Tool

1. **Create Tool Class**
   - Location: `src/main/kotlin/com/bifos/dooray/mcp/tools/`
   - Name: `{Action}{Resource}Tool.kt`
   - Implement tool interface

2. **Add Type Definitions**
   - Location: `src/main/kotlin/com/bifos/dooray/mcp/types/`
   - Define input/output types if needed

3. **Add Client Method**
   - Location: `src/main/kotlin/com/bifos/dooray/mcp/client/`
   - Implement in `DoorayHttpClient.kt`

4. **Register Tool**
   - Location: `src/main/kotlin/com/bifos/dooray/mcp/DoorayMcpServer.kt`
   - Add to tool registration

5. **Add Tests**
   - Location: `src/test/kotlin/com/bifos/dooray/mcp/`
   - Write integration or unit tests

### Common Tasks

**Update API Endpoint:** Modify `DoorayHttpClient.kt`
**Change Response Format:** Update files in `src/main/kotlin/com/bifos/dooray/mcp/types/`
**Add Configuration:** Update `EnvVariableConst.kt` and `.env.sample`
**Add Logging:** Use SLF4J loggers (configured via `logback.xml`)

---

## Logging Configuration

**Location:** `src/main/resources/logback.xml`
- Defines logging levels
- Configures output format
- Specifies log appenders
- Environment-driven log levels

---

## API Documentation

**Dooray API Reference:**
- `docs/wiki-api.md` - Wiki API endpoints
- `docs/project-api.md` - Project API endpoints
- `docs/project-post-comment.md` - Comments API
- `docs/wiki-create-api.md` - Wiki creation details

---

## Dependencies & Compatibility

- **Kotlin Version:** Specified in `build.gradle.kts`
- **JVM Version:** Specified in `build.gradle.kts`
- **Gradle Version:** Managed by wrapper in `gradle/wrapper/`

See `build.gradle.kts` for complete dependency list.

---

## Troubleshooting Guide

### Build Issues
- Check Java/Kotlin compiler settings in `build.gradle.kts`
- Verify Gradle wrapper: `./gradlew --version`
- Clean build: `./gradlew clean build`

### Test Failures
- Integration tests require valid API credentials (`.env` file)
- Check network connectivity to Dooray API
- Review `logback.xml` for debug logging

### Docker Issues
- Verify Docker installation: `docker --version`
- Check Dockerfile syntax
- Review build script permissions: `chmod +x scripts/docker-*.sh`

### API Integration Issues
- Verify environment variables in `.env`
- Check API endpoint URLs in `DoorayHttpClient.kt`
- Review `docs/` for API documentation updates
- Check authentication token validity

---

## Performance Considerations

- HTTP connection pooling in `DoorayHttpClient.kt`
- Timeout configuration in environment variables
- Pagination support in list operations
- Response caching strategies (if implemented)

---

## Security Notes

- API credentials stored in environment variables (never in code)
- `.env.sample` provides template without secrets
- HTTPS/TLS for all API communications
- Token refresh mechanisms (if applicable)

---

## Future Extension Points

1. **New Tools:** Add to `src/main/kotlin/com/bifos/dooray/mcp/tools/`
2. **New API Endpoints:** Extend `DoorayHttpClient.kt`
3. **New Data Types:** Add to `src/main/kotlin/com/bifos/dooray/mcp/types/`
4. **New Clients:** Implement additional API client versions
5. **Enhanced Caching:** Add cache layer to client
6. **Webhook Support:** Add webhook handling capabilities
7. **Batch Operations:** Add batch operation tools

---

## Contact & Support

For issues, questions, or contributions:
- Review `README.md` for project overview
- Check `docs/` for API documentation
- Run tests to verify functionality
- Check recent commits for recent changes

---

**Document Generated:** 2026-03-18
**Project:** dooray-mcp-server
**Language:** Kotlin
**Build Tool:** Gradle (Kotlin DSL)
