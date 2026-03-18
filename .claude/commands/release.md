Perform a full release for dooray-mcp-server.

## Steps

1. **Ask for version** if not provided as argument (e.g. `/release 0.4.0`)

2. **Update versions** in:
   - `gradle.properties`: `project.version=<VERSION>`
   - `src/main/kotlin/com/bifos/dooray/mcp/constants/VersionConst.kt`: `VERSION = "<VERSION>"` and add changelog entry

3. **Build and test** (CI mode, integration tests excluded):
   ```
   CI=true ./gradlew clean build shadowJar
   ```
   Stop and report if build fails.

4. **Run MCP server integration tests**:
   ```
   ./gradlew test --tests "com.bifos.dooray.mcp.McpServerIntegrationTest"
   ```
   Stop and report if tests fail.

5. **Commit** all changes with message: `feat: v<VERSION> - <brief description>`

6. **Tag and push**:
   ```
   git tag v<VERSION>
   git push origin main
   git push origin v<VERSION>
   ```

7. **Create GitHub release** with `gh release create v<VERSION>` including:
   - Korean release notes
   - Changelog of changes since last version
   - Docker pull commands
   - Usage example in JSON

## Notes
- Always verify build succeeds before tagging
- Integration tests (requiring real DOORAY_API_KEY) are excluded — only unit + MCP server tests run
- The MCP server integration test uses fake credentials and requires shadowJar
