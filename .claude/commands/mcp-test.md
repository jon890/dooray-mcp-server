Run MCP server integration tests that verify the server works correctly via MCP protocol.

## Steps

1. Build the shadowJar if it doesn't exist for the current version:
   ```
   ./gradlew shadowJar -q
   ```

2. Run the MCP server integration tests:
   ```
   ./gradlew test --tests "com.bifos.dooray.mcp.McpServerIntegrationTest"
   ```

3. Report results:
   - How many tests passed/failed
   - List any failures with details
   - Confirm all 16 tools are registered

## What these tests verify
- Server starts successfully with fake credentials
- All 16 MCP tools are registered (`dooray_wiki_*` × 5, `dooray_project_*` × 11)
- Each tool has a description and inputSchema
- MCP protocol handshake works correctly

## Notes
- Uses fake credentials (`DOORAY_BASE_URL=https://fake.dooray.test`, `DOORAY_API_KEY=fake-key`)
- No real Dooray API calls are made
- Tests are skipped automatically if shadowJar is not found
