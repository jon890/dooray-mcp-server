# Dooray MCP Server

> [!IMPORTANT]
> 이 저장소는 유지보수를 종료했으며 보관 상태로 전환되었습니다.
> 새로운 기능, 오류 수정, 보안 갱신, 새 Docker 이미지 배포는 제공하지 않습니다.
> Dooray 자동화에는 [dooray-cli](https://github.com/jon890/dooray-cli)를 사용해 주세요.

## 대체 프로젝트

`dooray-cli`는 Dooray 업무, 댓글, 위키, 메일, 메신저를 터미널과 AI 에이전트에서 사용할 수 있게 제공합니다.
보관 전환일인 2026년 8월 10일 기준 최신 버전은 `v0.16.0`입니다.

Node.js 20 이상에서 다음과 같이 설치합니다.

```bash
npm install -g @bifos/dooray-cli
dooray setup
dooray doctor
```

`dooray setup`은 Dooray API 주소와 인증 정보를 대화형으로 설정합니다.
인증 정보는 명령 인자나 공개 저장소에 기록하지 마세요.

## AI 에이전트에서 사용하기

CLI와 함께 제공되는 Claude Code 스킬을 설치하면 자연어 요청을 `dooray` 명령으로 처리할 수 있습니다.
다른 코딩 에이전트에서는 해당 에이전트의 지침 기능이나 명령 실행 기능을 통해 CLI를 사용할 수 있습니다.

```bash
dooray skill install
dooray skill status
```

CLI를 갱신한 뒤에는 스킬도 함께 갱신합니다.

```bash
npm install -g @bifos/dooray-cli@latest
dooray skill update
dooray doctor
```

설치가 끝나면 에이전트에게 다음과 같이 요청할 수 있습니다.

```text
내 프로젝트 목록을 보여줘.
백엔드 프로젝트에 업무를 만들고 담당자를 지정해줘.
42번 업무에 진행 상황 댓글을 추가해줘.
이번 주 회의록 위키 페이지를 만들어줘.
안 읽은 메일을 보여줘.
개발팀 대화방에 배포 완료 메시지를 보내줘.
```

## 터미널에서 직접 사용하기

```bash
dooray project list
dooray post list <project>
dooray post get <project> 42
dooray post create <project> --title "제목"
dooray post comment add <project> 42 --body "댓글"
dooray wiki pages <project>
dooray mail list --unread
```

전체 명령과 옵션은 각 단계의 도움말에서 확인합니다.

```bash
dooray --help
dooray post --help
dooray post create --help
```

자동화에서는 구조화된 결과를 위한 `--json`이나 식별자만 반환하는 `--quiet`를 사용할 수 있습니다.

```bash
dooray project list --json
POST_ID=$(dooray post create <project> --title "배포" --quiet)
dooray post comment add --id "$POST_ID" --body "시작합니다"
```

더 자세한 설치법과 전체 기능은 [dooray-cli README](https://github.com/jon890/dooray-cli#readme)를 참고해 주세요.

## 기존 MCP 사용자 안내

`dooray-cli`는 이 서버를 실행하거나 내부 코드를 가져오지 않는 독립 프로젝트입니다.
MCP 서버의 도구 호출을 그대로 대체하는 호환 계층은 제공하지 않습니다.

기존 사용자는 다음 순서로 이전할 수 있습니다.

1. MCP 클라이언트 설정에서 JAR 실행 명령이나 `bifos/dooray-mcp` Docker 실행 항목을 제거합니다.
2. `@bifos/dooray-cli`를 설치하고 `dooray setup`을 실행합니다.
3. AI 에이전트를 사용한다면 `dooray skill install`을 실행합니다.
4. 기존 자동화는 `dooray --help`와 각 하위 명령의 도움말을 기준으로 CLI 호출로 바꿉니다.
5. 변경 작업은 먼저 조회 명령으로 대상을 확인합니다.

## 이전 배포물

Docker Hub의 `bifos/dooray-mcp` 이미지와 기존 GitHub Release는 과거 동작을 재현하기 위해 남겨 둡니다.
이 배포물에는 새 기능이나 보안 수정이 제공되지 않으므로 신규 도입에는 사용하지 마세요.
`main` 브랜치에는 `v0.4.1` 이후 병합됐지만 별도 버전으로 배포되지 않은 변경도 포함되어 있습니다.

## 프로젝트 상태

- 최종 MCP 안정 버전: `v0.4.1`
- 권장 대체 프로젝트: [jon890/dooray-cli](https://github.com/jon890/dooray-cli)
- 보관 전환일 기준 CLI 버전: `v0.16.0`
- 유지보수 상태: 종료
- 저장소 상태: 읽기 전용 보관

소스 코드, 문서, 이슈, 기존 릴리스는 기록 보존을 위해 유지합니다.
