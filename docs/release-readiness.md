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
- [ ] `ci-gate`, `migration-safety`, `test-guard`, `commitlint`, `secret-scan`, `dependency-review`가 모두 통과했다.
- [ ] 모든 리뷰 스레드가 resolve됐고 프로젝트의 승인 정책을 충족했다.
- [ ] 신규 high/critical 취약 의존성이 없다.

## 3. 데이터·복구 게이트

- [ ] 모든 Flyway 파일이 forward-only이며 기존 적용 파일을 수정하지 않았다.
- [ ] [운영·복구 런북](operations-runbook.md)에 따라 격리 DB 복원 검증이 성공했다.
- [ ] 운영 백업의 생성 시각·보존 정책·복원 검증 시각을 운영 기록에 남겼다.
- [ ] 제품 소유자가 RPO·RTO·백업 주기·보존 기간을 확정했다.

## 4. 실스택 업무흐름 UAT

실 Keycloak 토큰, 실제 PostgreSQL, 후보 백엔드·프런트로 수행한다. 고객사 개통 절차와 승인 기록은 [유료 파일럿 온보딩 체크리스트](pilot-onboarding.md)를 함께 사용한다.

- [ ] 테넌트 A/B 토큰의 `tenant_id`가 서로 다르고 교차 데이터 노출이 0건이다.
- [ ] 회계기간 생성 → 전표 입력 → 결재 → 전기 → 시산표/손익/재무상태표 반영.
- [ ] 매입계산서 생성 → 결재 → 지급과 매출계산서 생성 → 결재 → 수금.
- [ ] 입고 → 창고간 이전 → 출고/조정 결재 후 재고 수량 일치.
- [ ] 부가세 신고 집계와 필터 적용 CSV 전체 내보내기 결과를 API/DB와 대조했다.
- [ ] 감사 로그에 수행자·대상·변경 전후·traceId가 확인된다.

로컬 실스택 렌더 E2E 실행 예시는 README의 `E2E_BACKEND=1` 명령을 따른다. 렌더 스모크 통과만으로 위 업무흐름 UAT를 대체하지 않는다.

## 5. 배포·관측성 게이트

- [ ] Railway·Vercel·Keycloak 운영 변수가 `docs/deployment.md`와 일치하며 시크릿은 플랫폼 변수에만 있다.
- [ ] 백엔드·프런트·Keycloak 헬스체크가 성공한다.
- [ ] Railway/Vercel 최신 배포 commit SHA가 릴리즈 SHA와 일치한다.
- [ ] ERROR 로그·5xx·지연·DB 연결 고갈 알림의 수신 채널과 담당자가 정해졌다.
- [ ] 장애 연락망·지원 채널·지원 시간이 고객 계약과 일치한다.

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
