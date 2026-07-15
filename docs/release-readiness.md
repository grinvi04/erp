# 상용 릴리즈 준비 체크리스트

이 문서는 유료 파일럿/운영 릴리즈의 승인 기록 템플릿이다. 체크되지 않은 항목이 하나라도 있으면 `main` 배포 후보로 승인하지 않는다. 명령 통과와 실제 운영 증거를 구분하며, 실행 결과에는 시크릿·고객 개인정보를 붙이지 않는다.

## 1. 릴리즈 식별과 변경 범위

- [ ] 릴리즈 버전·후보 commit SHA·검증 시각·검증자를 기록했다.
- [ ] `develop`이 원격 최신 상태이며 작업트리에 의도하지 않은 변경이 없다.
- [ ] 포함 Issue/PR과 DB 마이그레이션 목록을 검토했다.
- [ ] 기능 플래그·롤백 또는 fix-forward 경로를 기록했다.

## 2. 자동 품질·보안 게이트

```bash
cd backend && ./gradlew check
cd frontend && npm run test
cd frontend && npm run type-check
cd frontend && npm run lint
cd frontend && npm run lint:design
cd frontend && npm run build
cd frontend && npm run test:e2e
```

- [ ] 위 명령이 모두 exit 0이다.
- [ ] `ci-gate`, `migration-safety`, `test-guard`, `commitlint`, `secret-scan`, `dependency-review`, CodeQL Java·JavaScript/TypeScript가 모두 통과했다.
- [ ] 모든 리뷰 스레드가 resolve됐고 프로젝트의 승인 정책을 충족했다.
- [ ] CodeQL default setup이 `configured`이고 `main`·`develop`에 high 이상 보안 경보 차단 규칙이 적용된다.
- [ ] 열린 Dependabot·CodeQL high/critical 경보가 0건이다. 예외가 있으면 아래 기준의 승인 이슈가 릴리즈에 연결돼 있다.

```bash
OWNER_REPO=$(gh repo view --json nameWithOwner --jq .nameWithOwner)
gh api "repos/$OWNER_REPO/code-scanning/default-setup"
for branch in main develop; do
  gh api "repos/$OWNER_REPO/rules/branches/$branch" \
    --jq '.[] | select(.type == "code_scanning")'
done
gh api "repos/$OWNER_REPO/code-scanning/alerts?state=open&per_page=100" --paginate \
  --jq '.[] | select(.rule.security_severity_level == "high" or .rule.security_severity_level == "critical") | {number, rule: .rule.id, severity: .rule.security_severity_level}'
gh api "repos/$OWNER_REPO/dependabot/alerts?state=open&per_page=100" --paginate \
  --jq '.[] | select(.security_advisory.severity == "high" or .security_advisory.severity == "critical") | {number, package: .dependency.package.name, severity: .security_advisory.severity}'
```

경보 예외는 GitHub Issue에 영향 범위, 반증 근거, 보완 통제, 담당자, 승인자, 만료일과 후속 릴리즈를 기록해야 한다. 만료됐거나 이 항목이 하나라도 없는 예외는 인정하지 않는다. 실행 책임·주기·증거 위치와 자동 보안 업데이트 정책은 [운영·복구 런북](operations-runbook.md)의 "저장소 보안 경보 운영"을 따른다.

## 3. 데이터·복구 게이트

- [ ] 모든 Flyway 파일이 forward-only이며 기존 적용 파일을 수정하지 않았다.
- [ ] [운영·복구 런북](operations-runbook.md)에 따라 격리 DB 복원 검증이 성공했다.
- [ ] 운영 백업의 생성 시각·보존 정책·복원 검증 시각을 운영 기록에 남겼다.
- [ ] 제품 소유자가 [서비스 운영 정책](service-policy.md)의 RPO·RTO·백업 주기·보존 기간을 승인했고 실제 설정과 일치한다.

## 4. 실스택 업무흐름 UAT

실 Keycloak 토큰, 실제 PostgreSQL, 후보 백엔드·프런트로 수행한다. 고객사 개통 절차와 승인 기록은 [유료 파일럿 온보딩 체크리스트](pilot-onboarding.md)를 함께 사용한다.

- [ ] 테넌트 A/B 토큰의 `tenant_id`가 서로 다르고 교차 데이터 노출이 0건이다.
- [ ] 회계기간 생성 → 전표 입력 → 결재 → 전기 → 시산표/손익/재무상태표 반영.
- [ ] 매입계산서 생성 → 결재 → 지급과 매출계산서 생성 → 결재 → 수금.
- [ ] 입고 → 창고간 이전 → 출고/조정 결재 후 재고 수량 일치.
- [ ] 부가세 신고 집계와 필터 적용 CSV 전체 내보내기 결과를 API/DB와 대조했다.
- [ ] 감사 로그에 수행자·대상·변경 전후·traceId가 확인된다.
- [ ] 동일 테넌트 무권한 사용자의 업무·관리 메뉴 미노출과 보호 API 403/C005를 확인했다.
- [ ] 만료 세션의 refresh 실패가 `/login` 복귀로 끝나며 세션 응답에 토큰이 노출되지 않는다.

로컬 실스택 렌더 E2E 실행 예시는 README의 `E2E_BACKEND=1` 명령을 따른다. 렌더 스모크 통과만으로 위 업무흐름 UAT를 대체하지 않는다.

### 로컬 후보 검증 명령

README의 상용 UAT 전제를 준비한 뒤 다음을 순서대로 실행한다. `--dry-run`은 변경 없이 안전 계약만, `--setup-only`는 전용 UAT 신분·테넌트·권한(무권한 사용자의 역할 회수 포함)을, `--all`은 실제 업무 데이터 변경과 CSV·권한·세션 브라우저 readback까지 수행한다.

```bash
E2E_COMMERCIAL=1 E2E_COMMERCIAL_MUTATION=LOCAL_MUTATION_ACCEPTED \
  ./scripts/commercial-uat.sh --dry-run
E2E_COMMERCIAL=1 E2E_COMMERCIAL_MUTATION=LOCAL_MUTATION_ACCEPTED \
  ./scripts/commercial-uat.sh --setup-only
E2E_COMMERCIAL=1 E2E_COMMERCIAL_MUTATION=LOCAL_MUTATION_ACCEPTED \
  ./scripts/commercial-uat.sh --all
```

로컬 기본 포트와 다른 후보 서버를 검증할 때만 `E2E_COMMERCIAL_BACKEND_URL`/`E2E_COMMERCIAL_FRONTEND_URL`에 자격증명 없는 HTTP loopback URL을 지정한다. 결과는 릴리즈 후보 commit SHA·검증 시각·검증자·통과 건수만 6절 표에 남기고, 토큰·비밀번호·고객 데이터는 첨부하지 않는다.

## 5. 배포·관측성 게이트

- [ ] [개인정보·법률 준비 기준](privacy-legal-readiness.md)의 법률 문서·국외 처리·하위처리자·HR 제외 게이트를 모두 충족했다.
- [ ] Railway·Vercel·Keycloak 운영 변수가 `docs/deployment.md`와 일치하며 시크릿은 플랫폼 변수에만 있다.
- [ ] 백엔드·프런트·Keycloak 헬스체크가 성공한다.
- [ ] Railway/Vercel 최신 배포 commit SHA가 릴리즈 SHA와 일치한다.
- [ ] ERROR 로그·5xx·지연·DB 연결 고갈 알림의 수신 채널과 담당자가 정해졌다.
- [ ] 장애 연락망·지원 채널·지원 시간·고객 통지 기준이 승인된 서비스 운영 정책과 고객 계약에 일치한다.
- [ ] 데이터 반출·운영 데이터 삭제·백업 순환 삭제 절차를 실제 테넌트 사본으로 리허설했다.

## 6. 출시 승인

| 항목 | 값 |
|---|---|
| 버전 | |
| commit SHA | |
| 검증 환경 | |
| 복원 검증 시각 | |
| UAT 증거 위치 | |
| 잔여 위험·승인자 | |
| 출시 승인 시각 | |

승인 후에도 배포 신선도와 핵심 헬스체크가 불일치하면 즉시 중단하고 이전 정상 배포 유지 또는 fix-forward를 선택한다.
