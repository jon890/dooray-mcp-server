# dooray-cli v0.16.0 기능 대응표

작성일: 2026-08-06 KST

이 문서는 `dooray-cli` `v0.16.0` 태그의 66개 말단 명령과 공통 실행 계약 15개를 `dooray-mcp-server`의 MCP 도구, 리소스, 서버 기능으로 옮기는 기준표다.

현재 상태는 2026-08-06 기준 `dooray-mcp-server` `main`의 실제 구현을 기준으로 적었다.
따라서 계획 단계에서 아직 구현하지 않은 항목은 `missing` 또는 `partial`로 표시한다.
최종 완료 전에는 모든 행이 `implemented` 또는 `mcp-native-equivalent`로 바뀌어야 한다.

## 기준

| 항목 | 값 |
| --- | --- |
| CLI 기준 | `jon890/dooray-cli` `v0.16.0`, 커밋 `660dc91f1bddb40542205101228c7ee5c83090b6` |
| MCP 기준 | `jon890/dooray-mcp-server` `main`, 커밋 `6ce4ec8` |
| CLI 기능 수 | 말단 명령 66개 |
| 공통 실행 계약 수 | 전역 옵션, 입력, 출력, 오류, 안전 정책 15개 |
| 현재 MCP 공개 도구 | legacy 20개 |
| 목표 도구 프로필 | `compact` 12개, `legacy` 20개, `all` 32개 |
| 독립 구현 원칙 | MCP 서버는 CLI process, CLI package, CLI source import, CLI Docker install에 의존하지 않는다. |

목표 `compact` 도구는 다음 12개다.

- `dooray_server`
- `dooray_project`
- `dooray_member`
- `dooray_post`
- `dooray_post_comment`
- `dooray_project_attachment`
- `dooray_wiki`
- `dooray_wiki_page`
- `dooray_wiki_attachment`
- `dooray_wiki_comment`
- `dooray_mail`
- `dooray_messenger`

`legacy` 20개는 현재 호환 도구 이름을 보존한다.
`all`은 `compact`와 `legacy`를 함께 등록한다.
프로필은 `DOORAY_MCP_TOOL_PROFILE=compact|legacy|all`로 고정하고, 잘못된 값은 서버 시작 오류로 처리한다.

## MCP 공통 대응 원칙

| CLI 기능 | CLI 입력 | MCP 대응 | 도구·리소스 | 현재 상태 | 테스트 | 차이와 근거 |
| --- | --- | --- | --- | --- | --- | --- |
| 전역 `-V/--version` | 버전 문자열 출력 | MCP 초기화의 서버 버전과 버전 리소스에서 같은 정보를 제공한다. | 서버 `Implementation.version`, `dooray://server/version`, `dooray_server(operation=version)` | partial | 현재 초기화 연결 검사, plan014 버전 리소스 스냅숏 예정 | 현재 MCP 초기화에는 서버 버전이 있지만 버전 리소스와 구조화 진단 operation은 없다. |
| 전역 `--help`와 하위 명령 도움말 | 명령·옵션·기본값·설명 출력 | `tools/list`의 설명과 JSON Schema, 도구 계약 리소스가 호출 가능한 기능과 입력을 제공한다. | 모든 도구, `dooray://server/tool-contract` | partial | 현재 도구 목록·스키마 검사, plan014 계약 리소스 스냅숏 예정 | MCP에는 CLI 도움말 화면이 없다. 현재 legacy 도구 설명과 스키마는 있으나 compact operation 전체 계약 리소스는 없다. |
| 전역 `--json` | 모든 명령의 선택 출력 모드 | `structuredContent`와 `outputSchema`가 기본 구조화 결과를 제공한다. 호환용 `content`는 짧은 한국어 요약만 둔다. | 모든 compact 도구 | partial | plan003 스냅숏 테스트 예정 | CLI는 stdout JSON 모드다. MCP는 프로토콜 자체가 구조화 결과를 제공하므로 별도 옵션을 두지 않는다. 현재 legacy는 JSON 문자열 텍스트를 반환해 개선이 필요하다. |
| 전역 `--quiet` | 식별자만 stdout 출력 | `result_mode`는 `ids`, `compact`, `full`을 지원한다. `ids`는 식별자 전용 결과다. | 모든 compact 도구 | missing | plan003 결과 모드 테스트 예정 | CLI의 quiet 출력은 텍스트 압축이다. MCP에서는 구조화 결과 필드로 같은 사용자 결과를 제공한다. |
| 전역 `--json --quiet` 동시 입력 | 공용 formatter에서는 JSON 우선 | MCP는 별도 출력 모드 충돌을 만들지 않는다. `result_mode` 하나만 허용하고 구조화 결과는 항상 유지한다. | 모든 compact 도구 | missing | plan003 상호배타 입력과 구조화 결과 테스트 예정 | CLI의 명령별 편차를 복제하지 않고 하나의 열거형으로 모호성을 제거한다. |
| 전역 `--no-color`, `NO_COLOR` | ANSI 색상 비활성화 | MCP 결과에는 ANSI 색상 코드를 넣지 않는다. 로그도 터미널 색상에 의존하지 않는다. | 모든 도구와 서버 로그 | partial | plan003 ANSI escape 부재 테스트 예정 | MCP 클라이언트가 표시를 담당하므로 색상 옵션을 노출하지 않는다. 현재 결과는 색상 코드를 쓰지 않지만 계약 시험이 없다. |
| spinner와 진행 표시 | `--json` 또는 `--quiet`에서 비활성화 | STDIO stdout에는 MCP 프레임만 쓰고 진행 표시는 내보내지 않는다. 진단 로그는 stderr로 분리한다. | 서버 전송과 로깅 경계 | partial | 현재 MCP 연결 검사, plan003 stdout 오염 방지 테스트 예정 | MCP에서는 spinner가 프로토콜을 깨뜨릴 수 있어 항상 사용하지 않는다. |
| `stderr` | 진행 로그, 경고, 오류 문구 | 공개 결과의 `meta.warnings`, `error.details`, 서버 로그를 분리한다. 비밀 값과 stack trace는 공개 결과에 넣지 않는다. | `ToolExecutionBoundary` | partial | 현재 `toolHandler` 회귀 테스트와 plan003 오류 스냅숏 예정 | 현재 legacy는 일부 오류를 `content` JSON과 예외 메시지로 섞고 stack trace 노출 위험이 있다. |
| 종료 코드 | `0`, `1`, `2`, `3`, `4` | 안정적인 MCP 오류 코드와 `retryable`을 제공한다. `isError=true`를 외부 결과에 사용한다. | `DoorayMcpError`, 모든 compact 도구 | partial | plan003 오류 코드 테스트 예정 | MCP에는 process exit code가 없다. CLI exit code 의미를 `CONFIG_REQUIRED`, `AUTH_FAILED`, `INVALID_ARGUMENT` 같은 코드로 옮긴다. |
| `$EDITOR` | body가 없을 때 터미널 편집기 실행 | 대화형 편집기를 열지 않는다. `body`, `body_resource_uri`, `body_local_path`를 명시 입력으로 받는다. | `BodyInput`, MCP 리소스 | partial | plan003 입력 스키마 테스트 예정 | MCP 호출은 TTY를 전제하지 않는다. 현재 legacy는 명시 본문을 받지만 리소스·허용 경로 입력 계약은 없다. |
| `--dry-run` | 일부 생성·수정·피드백에서 변경 계획 출력 | 변경 operation의 `dry_run=true`가 대상 해석, 요청 본문, 영향, 확인 토큰을 반환한다. | 모든 변경 compact operation | missing | plan003, plan013 확인 토큰 테스트 예정 | 현재 legacy에는 공통 건조 실행 계약이 없다. MCP에서는 삭제와 손실 가능 수정의 기본 안전 장치로 확장한다. |
| 본문 입력 | `--body`, `--body-file`, `stdin -` | `BodyInput`이 `body`, `body_resource_uri`, `body_local_path` 중 하나를 받는다. 서버 경로는 허용 루트 안에서만 쓴다. | `BodyInput`, `dooray://resource/*` | partial | plan003, plan008 파일 경계 테스트 예정 | CLI 파일 경로는 사용자 터미널 기준이다. 현재 legacy는 문자열 본문만 지원하며 리소스와 허용 경로 입력은 없다. |
| 삭제 `-y/--yes` | TTY 확인 우회 | `prepare` operation이 삭제 계획과 짧은 만료의 `confirmation_token`을 반환하고, `confirm` operation이 같은 대상인지 검증한 뒤 실행한다. | 삭제 대상별 compact 도구 | missing | plan013 준비·확정·만료·대상 불일치 테스트 예정 | MCP는 TTY 확인을 사용할 수 없으므로 CLI의 `--yes`를 그대로 복제하지 않는다. 현재 legacy 삭제는 즉시 실행된다. |
| URL, ID, 코드 resolver | 업무·위키·프로젝트·멤버 대상 해석 | `PostTarget`, `WikiPageTarget`, `project_ref`, `member_ref` 같은 판별 입력을 둔다. 정확 일치, 부분 일치, 모호 후보를 구조화 오류로 반환한다. | `resolver` 서비스, `dooray_project`, `dooray_member`, `dooray_post`, `dooray_wiki_page` | partial | plan005 resolver 테스트 예정 | 현재 project ID와 일부 post ID 해석은 보완됐지만 CLI의 전체 resolver 정책과 후보 반환은 아직 없다. |
| 인증 정보 보호 | `config get`, `setup`, `mail logout` | 비밀 원문을 도구 결과, 오류, 로그, 리소스에 넣지 않는다. 메일 자격 증명은 환경변수나 secret 파일에서 읽고 도구 입력으로 받지 않는다. | `dooray_server`, `dooray_mail`, secret loader | partial | plan004, plan011 비밀 마스킹 테스트 예정 | 현재 API key는 환경변수로만 받지만 메일 자격 증명 구조는 아직 없다. |

## CLI 명령 대응표

| CLI 기능 | CLI 입력 | MCP 대응 | 도구·리소스 | 현재 상태 | 테스트 | 차이와 근거 |
| --- | --- | --- | --- | --- | --- | --- |
| `dooray setup` | 대화형 tenant, endpoint, API key, 메일 설정, 스킬 설치, `trackLastRun` | 대화형 마법사를 복제하지 않는다. 서버 환경변수, secret 파일, 설치 문서, `doctor` 결과로 같은 준비 상태를 만든다. | `dooray_server(operation=doctor)`, `dooray://server/install-guide` | partial | plan004 설정·진단 테스트 예정 | 현재 서버는 `DOORAY_API_KEY`, `DOORAY_BASE_URL`만 읽으며 doctor와 설치 리소스는 없다. CLI 저장소나 CLI setup을 실행하지 않는다. |
| `dooray skill status` | `--json`, `--quiet` | MCP 서버가 제공하는 리소스와 도구 프로필 상태를 진단한다. Claude skill 설치 상태를 직접 조작하지 않는다. | `dooray_server(operation=skill_status)`, `dooray://server/tool-contract` | missing | plan014 진단 테스트 예정 | MCP에서는 CLI skill 설치보다 클라이언트 설정 문서와 서버 리소스가 동등 결과지만 아직 구현되지 않았다. |
| `dooray skill install` | `--force`, `--json`, `--quiet` | 실제 설치 대신 MCP 클라이언트 설정 예시와 도구 계약 리소스를 제공한다. | `dooray://server/install-guide` | missing | plan014 리소스 스냅숏 예정 | MCP 서버가 호스트 파일 시스템의 Claude skill을 교체하는 것은 범위를 넘으며 설치 리소스는 아직 없다. |
| `dooray skill update` | install과 동일 | 설치 문서와 도구 계약 리소스의 버전 정보를 갱신한다. | `dooray://server/install-guide`, `dooray://server/version` | missing | plan014 리소스 스냅숏 예정 | CLI skill lifecycle은 MCP 리소스와 문서 갱신으로 대체하지만 아직 구현되지 않았다. |
| `dooray config set <key> <value>` | `api-key`, `base-url`, `tenant-name`, IMAP/SMTP 키, `track-last-run` | 비밀 값 쓰기는 도구로 받지 않는다. 서버 환경변수와 secret 파일을 진단하고, 비밀 없는 로컬 상태만 관리한다. | `dooray_server(operation=config_status)` | partial | plan004 비밀 마스킹 테스트 예정 | 현재 환경변수 설정은 읽지만 구조화된 상태 진단과 비밀 파일 계약은 없다. 인증 정보 원문을 MCP 도구 입력·결과로 노출하지 않는다. |
| `dooray config get [key]` | key 생략 또는 `api-key`, `base-url` | 설정 존재 여부와 마스킹된 값만 반환한다. | `dooray_server(operation=config_status)` | missing | plan004 설정 조회 테스트 예정 | CLI도 API key를 마스킹한다. MCP는 원문 조회 기능을 제공하지 않으며 현재 설정 상태 조회도 없다. |
| `dooray cache clear` | 옵션 없음 | 캐시 삭제 준비와 확정으로 구현한다. | `dooray_server(operation=cache_prepare_clear)`, `dooray_server(operation=cache_confirm_clear)` | missing | plan004, plan013 캐시 삭제 토큰 테스트 예정 | 현재 MCP에는 캐시 계층과 삭제 확인이 없다. |
| `dooray cache refresh` | 옵션 없음 | CLI와 같이 eager fetch가 아니라 캐시 삭제 후 다음 조회에서 갱신한다는 결과를 명시한다. | `dooray_server(operation=cache_refresh)` | missing | plan004 캐시 테스트 예정 | CLI v0.16.0의 실제 동작은 refresh가 전체 cache 삭제다. |
| `dooray doctor` | 옵션 없음 | API, 설정, 도구 프로필, 캐시, 메일 선택 진단을 구조화 결과로 제공한다. | `dooray_server(operation=doctor)`, `dooray://server/version` | missing | plan004 doctor 테스트 예정 | 현재 서버는 시작 시 필수 환경변수만 검사하고 진단 도구는 없다. |
| `dooray feedback` | `--title`, `--body`, `--body-file`, `--label`, `--last`, `--dry-run` | 기본은 preview만 반환한다. 실제 GitHub issue 생성은 관리자 설정과 외부 토큰이 있을 때만 별도 operation으로 제공한다. | `dooray_server(operation=feedback_preview)`, `dooray_server(operation=feedback_submit)` | missing | plan014 dry-run·비밀 제거 테스트 예정 | MCP 서버가 `gh` process를 실행하지 않는다. 외부 게시 전 preview 정책을 따르며 현재 기능은 없다. |
| `dooray project list` | `--search`, `--type public/private` | 프로젝트 목록과 검색을 `operation=list`로 제공한다. | `dooray_project(operation=list)`, legacy `dooray_project_list_projects` | partial | 현재 도구 테스트, plan005 필터 테스트 예정 | 현재 legacy는 page/size/type/scope/state는 있으나 CLI의 `search`와 cache 정책은 다르다. |
| `dooray project members <project>` | 프로젝트 코드 또는 ID | 프로젝트 멤버 조회를 제공하고 resolver 후보를 구조화한다. | `dooray_project(operation=members)`, legacy `dooray_project_list_members` | partial | 현재 도구 테스트, plan005 resolver 테스트 예정 | 현재 legacy는 멤버 목록만 제공하며 이름·이메일 해석과 모호 후보 계약이 부족하다. |
| `dooray project workflows <project>` | 프로젝트 코드 또는 ID | 업무 흐름 목록과 class/name 해석을 제공한다. | `dooray_project(operation=workflows)`, legacy `dooray_project_list_workflows` | partial | 현재 도구 테스트, plan005 workflow resolver 테스트 예정 | 현재 목록은 있으나 CLI의 class exact 우선과 모호성 계약은 별도 구현이 필요하다. |
| `dooray project groups <project>` | 프로젝트 코드 또는 ID | 멤버 그룹 목록과 그룹 담당자 resolver를 제공한다. | `dooray_project(operation=groups)` | missing | plan005 group resolver 테스트 예정 | 현재 MCP에는 group 조회 도구가 없다. |
| `dooray project tags <project>` | 프로젝트 코드 또는 ID | 태그 목록, 필수 태그, selectOne 검증 정보를 제공한다. | `dooray_project(operation=tags)` | missing | plan005 tag 테스트 예정 | 현재 MCP에는 tag 조회와 검증이 없다. |
| `dooray project templates <project>` | 프로젝트 코드 또는 ID | 업무 템플릿 목록과 생성 기본값 조회를 제공한다. | `dooray_project(operation=templates)` | missing | plan005 template 테스트 예정 | 현재 MCP에는 template 조회가 없다. |
| `dooray member get <member-id>` | organizationMemberId | 멤버 상세 조회를 제공한다. | `dooray_member(operation=get)` | missing | plan005 member 테스트 예정 | 현재 MCP는 프로젝트 멤버 목록만 있고 조직 멤버 단건 API가 없다. |
| `dooray member list <project>` | 프로젝트 코드 또는 ID | 프로젝트 기준 멤버 목록을 제공한다. | `dooray_member(operation=list)`, legacy `dooray_project_list_members` | partial | 현재 도구 테스트, plan005 멤버 목록 테스트 예정 | legacy 도구는 같은 사용자 결과를 일부 제공하지만 이름과 출력 계약이 다르다. |
| `dooray member search [keyword]` | 이름, `--email`, `--user-code`, `--user-code-exact`, `--page`, `--size` | 조직 검색과 exact/partial 후보 반환을 제공한다. | `dooray_member(operation=search)` | missing | plan005 exact·partial·ambiguous 테스트 예정 | 현재 MCP에는 조직 검색 resolver가 없다. |
| `dooray post list <project>` | `--subject`, `--all`, `--page`, `--size` | 업무 목록 조회와 제목 검색, pagination을 제공한다. | `dooray_post(operation=list)`, legacy `dooray_project_list_posts` | partial | 현재 도구 테스트, plan006 list 테스트 예정 | 현재 legacy는 다양한 필터가 있지만 CLI의 `--all`, quiet, 정확 출력 계약은 없다. |
| `dooray post search <project> <keyword>` | 프로젝트와 제목 keyword | 같은 도구의 검색 operation으로 통합한다. | `dooray_post(operation=search)`, legacy `dooray_project_list_posts` | partial | plan006 search 테스트 예정 | 별도 도구를 늘리지 않고 operation 파라미터로 처리한다. 현재 legacy는 `subjects` 필터가 있으나 CLI 검색 계약과 다르다. |
| `dooray post get [project] [number]` | `project number`, `--id`, `--url` | `PostTarget`으로 업무 상세를 조회한다. | `dooray_post(operation=get)`, legacy `dooray_project_get_post` | partial | 현재 도구 테스트, plan006 target resolver 테스트 예정 | 현재 legacy는 `project_id`, `post_id` 중심이고 URL·업무 번호 resolver가 부족하다. |
| `dooray post create <project>` | 제목, 본문, 담당자, 참조자, 그룹, 태그, 멘션, 링크, 부모, 템플릿, workflow, dry-run | 생성 operation에서 명시 입력, resolver, dry-run, 멱등 키를 지원한다. | `dooray_post(operation=create)`, legacy `dooray_project_create_post` | partial | 현재 도구 테스트, plan006 생성 계약 테스트 예정 | 현재 legacy는 group 일부와 parent는 있으나 `to_member_ids` required 스키마, template, mention, link, workflow 후처리, dry-run이 부족하다. |
| `dooray post edit [project] [number]` | 제목, 본문, 담당자/참조자 추가·삭제·clear, 그룹, 태그 추가·삭제·clear, parent, dry-run | 수정 operation이 기존 제목·본문·태그 보존, 참여자 단독 수정, 참조 손실 확인을 보장한다. | `dooray_post(operation=edit)`, legacy `dooray_project_update_post` | partial | 현재 보존 테스트, plan006 참여자·태그 테스트 예정 | 현재 legacy는 기존 필드 일부를 보존하지만 `--to-clear`, `--cc-clear`, tag add/remove, attachment reference 보호, URL 대상이 없다. |
| `dooray post done [project] [number]` | 업무 target | 완료 처리 operation을 제공한다. | `dooray_post(operation=set_done)`, legacy `dooray_project_set_post_done` | partial | 현재 도구 테스트, plan006 done 테스트 예정 | 현재 legacy는 post ID 기준이며 CLI target resolver와 구조화 오류 계약이 부족하다. |
| `dooray post workflow [project] [number] [workflow]` | 업무 target과 workflow 이름/class/ID | workflow resolver 후 상태 변경을 제공한다. | `dooray_post(operation=set_workflow)`, legacy `dooray_project_set_post_workflow` | partial | 현재 도구 테스트, plan006 workflow 테스트 예정 | 현재 legacy는 workflow ID 직접 입력 중심이다. CLI의 이름/class 해석과 모호성 오류가 필요하다. |
| `dooray post comment list` | 업무 target, `--page`, `--size`, `--sort`, `--reverse`, `--latest`, `--since`, `--from-author` | 댓글 목록 조회와 로컬 필터를 operation으로 제공한다. | `dooray_post_comment(operation=list)`, legacy `dooray_project_get_post_comments` | partial | 현재 도구 테스트, plan007 필터 테스트 예정 | 현재 legacy는 page/size/order만 제공한다. 최신 N개, since, author 필터가 없다. |
| `dooray post comment latest` | 업무 target, `-n/--count` | `operation=latest` 또는 `operation=list`의 `latest_count`로 제공한다. | `dooray_post_comment(operation=latest)` | missing | plan007 latest 테스트 예정 | 현재 MCP에는 별도 latest 동작이 없다. |
| `dooray post comment get` | 업무 target과 comment ID | 댓글 단건 상세를 제공한다. | `dooray_post_comment(operation=get)` | missing | plan007 get 테스트 예정 | 현재 MCP는 댓글 목록만 있고 단건 상세 도구가 없다. |
| `dooray post comment add` | 업무 target, 본문, 멘션, 링크, dry-run | 댓글 생성과 dry-run body preview를 제공한다. | `dooray_post_comment(operation=add)`, legacy `dooray_project_create_post_comment` | partial | 현재 도구 테스트, plan007 add 테스트 예정 | 현재 legacy는 본문 생성만 있고 멘션·링크·dry-run·URL target이 없다. |
| `dooray post comment edit` | 업무 target, comment ID, 본문, `--no-confirm`, 멘션, 링크, dry-run | 댓글 수정과 attachment reference 손실 확인을 제공한다. | `dooray_post_comment(operation=edit)`, legacy `dooray_project_update_post_comment` | partial | 현재 도구 테스트, plan007 reference 테스트 예정 | 현재 legacy는 본문 수정만 있고 기존 본문 seed, 멘션·링크, 참조 손실 확인이 없다. |
| `dooray post comment delete` | 업무 target, comment ID, `-y/--yes` | 삭제 준비·확정으로 제공한다. | `dooray_post_comment(operation=prepare_delete)`, `dooray_post_comment(operation=confirm_delete)`, legacy `dooray_project_delete_post_comment` | partial | 현재 도구 테스트, plan013 삭제 토큰 테스트 예정 | 현재 legacy는 즉시 삭제라서 MCP 안전 정책과 다르다. 최종 compact는 토큰 없이는 삭제하지 않는다. |
| `dooray post file list` | 업무 target | 업무 첨부 목록을 제공한다. | `dooray_project_attachment(operation=list, owner_kind=post)` | missing | plan008 파일 목록 테스트 예정 | 현재 MCP에는 업무 첨부 도구가 없다. |
| `dooray post file upload` | 업무 target, file path | `FileSource`로 업로드한다. path, base64, resource 입력을 지원한다. | `dooray_project_attachment(operation=upload, owner_kind=post)` | missing | plan008 업로드 경계 테스트 예정 | MCP 환경에서는 호출자 로컬 path를 그대로 신뢰할 수 없다. |
| `dooray post file download` | 업무 target, file ID, output dir | 기본은 MCP 리소스로 반환하고, 허용된 서버 path 저장은 선택으로 제공한다. | `dooray_project_attachment(operation=download, owner_kind=post)` | missing | plan008 다운로드 경계 테스트 예정 | CLI는 로컬 파일에 저장한다. MCP는 resource delivery가 기본이다. |
| `dooray post file download-all` | 업무 target, output dir | 여러 파일을 resource 묶음 또는 허용 서버 path로 반환한다. 부분 실패를 `steps[]`에 기록한다. | `dooray_project_attachment(operation=download_all, owner_kind=post)` | missing | plan008 일괄 다운로드 테스트 예정 | 현재 MCP에는 파일 리소스와 부분 실패 계약이 없다. |
| `dooray post file delete` | 업무 target, file ID, `-y/--yes` | 첨부 삭제 준비·확정으로 제공한다. | `dooray_project_attachment(operation=prepare_delete, owner_kind=post)`, `dooray_project_attachment(operation=confirm_delete, owner_kind=post)` | missing | plan013 삭제 토큰 테스트 예정 | CLI의 `--yes` 대신 확인 토큰이 필요하다. |
| `dooray post comment file list` | 업무 target, comment ID | 댓글 API에 노출되는 file reference만 반환하고 API 한계를 명시한다. | `dooray_project_attachment(operation=list, owner_kind=comment)` | missing | plan008 댓글 첨부 목록 테스트 예정 | Dooray 웹 직접 첨부가 댓글 API에 없으면 추정 보완하지 않는다. |
| `dooray post comment file upload` | 업무 target, comment ID, file path | 업무 파일 업로드 후 댓글 본문에 이미지 또는 일반 링크 reference를 추가한다. | `dooray_project_attachment(operation=upload, owner_kind=comment)` | missing | plan008 이미지·일반 링크 테스트 예정 | 이미지 확장자는 `![]`, 그 외와 확장자 없음은 일반 링크로 처리한다. |
| `dooray post comment file download` | 업무 target, comment ID, file ID, output | 업무 파일 다운로드를 댓글 첨부 맥락으로 제공한다. | `dooray_project_attachment(operation=download, owner_kind=comment)` | missing | plan008 다운로드 테스트 예정 | comment ID는 검증 맥락에 쓰고 파일 전송은 공통 업무 파일 API를 사용한다. |
| `dooray post comment file delete` | 업무 target, comment ID, file ID, `-y/--yes` | 댓글 body reference 제거와 파일 삭제를 준비·확정으로 처리한다. | `dooray_project_attachment(operation=prepare_delete, owner_kind=comment)`, `dooray_project_attachment(operation=confirm_delete, owner_kind=comment)` | missing | plan013 reference 제거 테스트 예정 | 이미지와 일반 링크 형식을 모두 안전하게 제거해야 한다. 부분 실패를 숨기지 않는다. |
| `dooray wiki list` | `--page`, `--size` | 접근 가능한 위키 목록을 조회한다. | `dooray_wiki(operation=list)`, legacy `dooray_wiki_list_projects` | partial | 현재 도구 테스트, plan009 wiki list 테스트 예정 | 현재 legacy 이름은 `projects`지만 위키 목록을 반환한다. pagination과 출력 계약 보강이 필요하다. |
| `dooray wiki pages <project>` | 프로젝트, `--parent` | 위키 페이지 직속 목록을 조회한다. | `dooray_wiki(operation=pages)`, legacy `dooray_wiki_list_pages` | partial | 현재 도구 테스트, plan009 pages 테스트 예정 | 현재 legacy는 wiki project ID 중심이고 project code resolver·parent 정책 보강이 필요하다. |
| `dooray wiki tree <project>` | 프로젝트, `--depth` | 페이지 트리를 BFS와 캐시로 조회한다. | `dooray_wiki(operation=tree)` | missing | plan009 tree 성능·캐시 테스트 예정 | 현재 MCP에는 tree 도구가 없다. 이슈 #2의 성능 요구와 함께 구현한다. |
| `dooray wiki page get <project> <page-id>` | 프로젝트와 page ID | 페이지 상세 조회를 제공한다. | `dooray_wiki_page(operation=get)`, legacy `dooray_wiki_get_page` | partial | 현재 도구 테스트, plan009 get 테스트 예정 | 현재 legacy는 ID 중심이고 URL, project code resolver, 구조화 오류가 부족하다. |
| `dooray wiki page create <project>` | 제목, parent, 본문 | root 보완, 명시 본문, 리소스 입력을 지원한다. | `dooray_wiki_page(operation=create)`, legacy `dooray_wiki_create_page` | partial | 현재 도구 테스트, plan009 create 테스트 예정 | 현재 legacy는 기본 생성은 있으나 CLI의 body 입력 변형과 parent 보완 계약을 모두 담지 못한다. |
| `dooray wiki page edit <project> <page-id>` | 제목, 본문, `$EDITOR` fallback | 제목만, 본문만, 전체 수정 분기를 명시 입력으로 제공한다. | `dooray_wiki_page(operation=edit)`, legacy `dooray_wiki_update_page` | partial | 현재 도구 테스트, plan009 edit 테스트 예정 | MCP는 `$EDITOR`를 쓰지 않는다. 현재 legacy는 일부 update만 제공한다. |
| `dooray wiki page delete` | page target, `-y/--yes` | 하위 페이지 영향 조회 후 삭제 준비·확정으로 제공한다. | `dooray_wiki_page(operation=prepare_delete)`, `dooray_wiki_page(operation=confirm_delete)` | missing | plan013 page 삭제 토큰·하위 페이지 테스트 예정 | 현재 MCP에는 위키 페이지 삭제가 없다. 하위 페이지 재부착 정책을 결과에 명시해야 한다. |
| `dooray wiki page file list` | page target | `files`와 `images`를 합쳐 반환한다. | `dooray_wiki_attachment(operation=list)` | missing | plan010 파일 목록 테스트 예정 | 현재 MCP에는 위키 첨부 도구가 없다. |
| `dooray wiki page file upload` | page target, file, `--type general/inline_image` | `FileSource` 업로드와 inline 이미지 snippet 반환을 제공한다. | `dooray_wiki_attachment(operation=upload)` | missing | plan010 업로드·snippet 테스트 예정 | multipart field 순서와 307 재시도를 독립 구현한다. |
| `dooray wiki page file download` | page target, file ID, output dir | 기본은 MCP 리소스로 반환한다. | `dooray_wiki_attachment(operation=download)` | missing | plan010 다운로드 경계 테스트 예정 | CLI의 로컬 output dir 대신 resource delivery가 기본이다. |
| `dooray wiki page file download-all` | page target, output dir | 일반 파일과 inline 이미지를 모두 다운로드한다. 부분 실패를 구조화한다. | `dooray_wiki_attachment(operation=download_all)` | missing | plan010 일괄 다운로드 테스트 예정 | 현재 MCP에는 파일 일괄 결과 계약이 없다. |
| `dooray wiki page file delete` | page target, file ID, `-y/--yes` | 삭제 준비·확정으로 제공한다. | `dooray_wiki_attachment(operation=prepare_delete)`, `dooray_wiki_attachment(operation=confirm_delete)` | missing | plan013 삭제 토큰 테스트 예정 | CLI 즉시 삭제는 MCP에서 허용하지 않는다. |
| `dooray wiki page comment list` | page target, `--size`, `--page`, `--latest` | 위키 페이지 댓글 목록을 제공한다. | `dooray_wiki_comment(operation=list)`, legacy `dooray_wiki_get_page_comments` | partial | 현재 도구 테스트, plan009 댓글 목록 테스트 예정 | 현재 legacy는 list만 있고 `latest` 우선 정책과 target resolver가 부족하다. |
| `dooray wiki page comment latest` | page target | 최신 댓글 1건을 반환한다. | `dooray_wiki_comment(operation=latest)` | missing | plan009 latest 테스트 예정 | 현재 MCP에는 latest operation이 없다. |
| `dooray wiki page comment get` | page target, comment ID | 위키 댓글 단건 상세를 반환한다. | `dooray_wiki_comment(operation=get)` | missing | plan009 get 테스트 예정 | 현재 MCP에는 위키 댓글 단건 조회가 없다. |
| `dooray wiki page comment add` | page target, 본문 | 위키 댓글 생성을 제공한다. | `dooray_wiki_comment(operation=add)` | missing | plan009 add 테스트 예정 | 현재 MCP에는 위키 댓글 생성이 없다. |
| `dooray wiki page comment edit` | page target, comment ID, 본문 | 위키 댓글 수정을 제공한다. | `dooray_wiki_comment(operation=edit)` | missing | plan009 edit 테스트 예정 | 현재 MCP에는 위키 댓글 수정이 없다. |
| `dooray wiki page comment delete` | page target, comment ID, `-y/--yes` | 삭제 준비·확정으로 제공한다. | `dooray_wiki_comment(operation=prepare_delete)`, `dooray_wiki_comment(operation=confirm_delete)` | missing | plan013 삭제 토큰 테스트 예정 | 현재 MCP에는 위키 댓글 삭제가 없다. |
| `dooray mail list` | `--unread`, `--search`, `--size` | IMAP INBOX 조회를 독립 구현한다. | `dooray_mail(operation=list)` | missing | plan011 IMAP 오류 분류 테스트 예정 | 현재 MCP에는 메일 기능이 없다. CLI 패키지를 호출하지 않고 Angus Mail 계열로 독립 구현한다. |
| `dooray mail get <uid>` | UID | IMAP source 파싱 결과를 구조화한다. | `dooray_mail(operation=get)` | missing | plan011 mailparser 대응 테스트 예정 | 현재 MCP에는 메일 상세 조회가 없다. |
| `dooray mail send` | 수신자, 제목, 본문, cc, bcc, html | SMTP 발송 결과를 accepted/rejected/messageId로 구조화한다. | `dooray_mail(operation=send)` | missing | plan011 SMTP 오류 분류 테스트 예정 | 인증 정보는 도구 입력으로 받지 않는다. |
| `dooray mail reply <uid>` | UID, 본문, cc, html | 원본 조회 후 `In-Reply-To`와 `References`를 설정해 발송한다. | `dooray_mail(operation=reply)` | missing | plan011 reply 테스트 예정 | 현재 MCP에는 메일 답장이 없다. |
| `dooray mail logout` | `-y/--yes` | 저장된 메일 자격 증명 제거를 준비·확정으로 제공한다. | `dooray_mail(operation=prepare_logout)`, `dooray_mail(operation=confirm_logout)` | missing | plan013 자격 증명 삭제 테스트 예정 | 도구 결과에 사용자명·비밀번호 원문을 넣지 않는다. |
| `dooray messenger send` | `--to memberId/email`, 본문 | 사용자 식별자를 해석해 direct message를 보낸다. | `dooray_messenger(operation=send_user)` | missing | plan012 대상 해석 테스트 예정 | 현재 MCP에는 메신저 기능이 없다. |
| `dooray messenger channel-send` | `--channel channelId/name`, 본문 | 채널 ID 또는 이름을 해석해 채널 메시지를 보낸다. | `dooray_messenger(operation=send_channel)` | missing | plan012 채널 모호성 테스트 예정 | 15자리 이상 ID pass-through와 이름 exact/partial 후보를 구조화해야 한다. |

## 목표 도구 수와 상태

| 프로필 | 목표 도구 수 | 설명 |
| --- | ---: | --- |
| `compact` | 12 | 66개 CLI 기능 전체를 domain tool의 `operation`으로 제공한다. |
| `legacy` | 20 | 현재 호환 도구 20개를 유지한다. 공개 이름과 기존 입력을 함부로 깨지 않는다. |
| `all` | 32 | `compact`와 `legacy`를 함께 노출해 전환 검증에 사용한다. |

현재 계획 문서 기준으로 CLI 말단 명령 66개와 공통 실행 계약 15개 가운데 `implemented`는 없다.
기존 legacy 도구가 있는 기능도 compact 계약, resolver, 출력, 오류, 삭제 안전 정책까지 완성되지 않았으면 `partial`로 표시했다.
