#!/usr/bin/env bash

set -euo pipefail

PROJECT_VERSION="${PROJECT_VERSION:-$(grep 'project.version=' gradle.properties | cut -d'=' -f2)}"
EVENT_NAME="${EVENT_NAME:-}"
INPUT_VERSION="${INPUT_VERSION:-}"
RELEASE_TAG="${RELEASE_TAG:-}"
RELEASE_PRERELEASE="${RELEASE_PRERELEASE:-false}"
PUSH_REQUESTED="${PUSH_REQUESTED:-false}"

if [[ -n "${INPUT_VERSION}" ]]; then
  VERSION="${INPUT_VERSION#v}"
elif [[ -n "${RELEASE_TAG}" ]]; then
  VERSION="${RELEASE_TAG#v}"
else
  VERSION="${PROJECT_VERSION}"
fi

if [[ "${PUSH_REQUESTED}" == "true" && "${VERSION}" != "${PROJECT_VERSION}" ]]; then
  echo "게시 버전 ${VERSION}이 project.version ${PROJECT_VERSION}과 일치하지 않습니다." >&2
  exit 1
fi

if [[ "${VERSION}" == *-* ]]; then
  IS_PRERELEASE="true"
else
  IS_PRERELEASE="false"
fi

PUSH_LATEST="false"
if [[ "${EVENT_NAME}" == "release" && "${PUSH_REQUESTED}" == "true" && "${IS_PRERELEASE}" == "false" && "${RELEASE_PRERELEASE}" != "true" ]]; then
  PUSH_LATEST="true"
fi

{
  echo "VERSION=${VERSION}"
  echo "PROJECT_VERSION=${PROJECT_VERSION}"
  echo "IS_PRERELEASE=${IS_PRERELEASE}"
  echo "PUSH_LATEST=${PUSH_LATEST}"
} >> "${GITHUB_OUTPUT:-/dev/stdout}"

echo "Build version: ${VERSION}"
echo "Project version: ${PROJECT_VERSION}"
echo "Prerelease: ${IS_PRERELEASE}"
echo "Push latest: ${PUSH_LATEST}"
