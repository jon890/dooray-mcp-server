Clean build and run unit tests (CI mode, integration tests excluded).

Steps:
1. Run `CI=true ./gradlew clean build` and report results
2. If tests fail (beyond expected integration test failures), investigate and report the errors
3. Report final build status and test counts
