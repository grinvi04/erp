# 운영·복구 런북

## 목적과 완료 기준

유료 파일럿의 백업은 파일 생성만으로 성공으로 보지 않는다. 격리된 새 PostgreSQL 데이터베이스에 복원한 뒤 Flyway 이력, 테넌트, HR·Finance·Inventory·CRM 및 Keycloak 핵심 데이터가 원본과 일치해야 한다.

## 역할과 안전 경계

- 운영 담당자만 Railway/Vercel/Keycloak 변수와 고객 데이터에 접근한다.
- 운영 DB를 `RESTORE_DATABASE_URL`로 사용하지 않는다. 검증용 빈 DB를 별도로 만든다.
- 스크립트는 소스·대상 URL이 같으면 거부하며, `ALLOW_DESTRUCTIVE_RESTORE=true` 없이는 대상 DB를 변경하지 않는다.
- DB URL, 비밀번호, 토큰, 백업 파일은 로그·이슈·PR·저장소에 남기지 않는다.

## 백업·복원 검증

사전 조건: Docker, 원본 DB 읽기 권한, 삭제 가능한 검증 DB 관리자 권한.

```bash
SOURCE_DATABASE_URL='<원본 PostgreSQL URL>' \
RESTORE_DATABASE_URL='<격리된 검증 PostgreSQL URL>' \
ALLOW_DESTRUCTIVE_RESTORE=true \
./scripts/verify-backup-restore.sh
```

성공 출력은 `복원 검증 성공`이며 다음 행 수를 원본과 복원본에서 대조한다.

- `public.flyway_schema_history`
- `common.tenant`
- `hr.employee`
- `finance.journal_entry`
- `inventory.stock`
- `crm.account`
- `keycloak.user_entity`

성공 시 백업 내용 대신 다음 비식별 증거만 출력한다. `restore_total_seconds`는 기존 백업을
복원하고 핵심 집계를 대조하는 데 걸린 시간(`restore_seconds + verification_seconds`)이며,
`rehearsal_total_seconds`는 원본 집계와 백업 생성까지 포함한 전체 실행 시간이다.

- `backup_bytes`, `backup_sha256`
- `snapshot_seconds`, `backup_seconds`
- `restore_seconds`, `verification_seconds`, `restore_total_seconds`
- `rehearsal_total_seconds`

초 단위 값이 `0`인 단계는 생략이 아니라 1초 미만에 완료된 것이다. 운영 RTO 증거에는
실제 운영 규모 백업의 `restore_total_seconds`와 실행 환경을 기록하고, 로컬 소량 데이터 결과를
대신 사용하지 않는다.

## 장애 복구 순서

1. 쓰기 트래픽을 차단하고 사고 시작 시각·영향 테넌트·마지막 정상 배포 SHA를 기록한다.
2. 원본 DB를 변경하지 않은 채 최신 백업을 새 DB에 복원한다.
3. 위 검증 스크립트와 `./gradlew check`를 통과시킨다.
4. 스테이징 백엔드를 복원 DB에 연결해 헬스체크와 실 Keycloak E2E를 수행한다.
5. 데이터 손실 구간과 복구 시각을 확인한 뒤에만 연결 전환 여부를 승인받는다.
6. 복구 후 원인·영향·타임라인·재발방지를 저장소 이슈/사고 기록에 남긴다.

## 출시 전 필수 결정

첫 유료 계약 전에 제품 소유자가 [유료 파일럿 서비스 운영 정책](service-policy.md)의 RPO, RTO, 백업 주기, 보존 기간, 지원 채널·시간, 가용성 및 데이터 반출·파기 기한을 승인하고 계약·운영 설정과 일치시켜야 한다. 초안 상태이거나 실제 플랫폼 설정·복원 측정 증거가 없으면 유료 운영 출시를 승인하지 않는다.

## 저장소 보안 경보 운영

저장소 소유자가 책임자이며, 릴리즈 실행자가 매 릴리즈 후보 검증 때 실행한다. 정기 점검은 매주 수요일 10:00 KST에 수행한다. 휴일이면 다음 영업일 같은 시각으로 옮기되 생략하지 않는다.

1. [릴리즈 준비 체크리스트](release-readiness.md)의 CodeQL default setup·브랜치 규칙·열린 high/critical 조회 명령을 실행한다.
2. 새 경보는 2영업일 안에 재현 가능성, 노출 경로, 영향 테넌트와 수정 버전을 분류한다. 테넌트 간 노출·인증 우회·데이터 무결성 위험은 즉시 P1 절차로 전환한다.
3. high/critical은 수정 PR을 만들고 `develop`에서 전체 CI와 CodeQL을 통과시키기 전까지 출시를 차단한다. medium 이하는 실제 노출 가능성과 공급망 범위를 근거로 우선순위를 정한다.
4. 오탐·테스트 전용 경보는 GitHub 경보 화면에 구체적 근거를 남겨 dismiss한다. 위험 수용은 영향 범위, 반증 근거, 보완 통제, 담당자, 승인자, 만료일, 후속 릴리즈가 있는 GitHub Issue만 인정한다.
5. 점검 증거는 고객 데이터나 시크릿 없이 점검 시각, 검사한 commit SHA, 열린 경보 수, 관련 Issue/PR 링크를 `paid-pilot-launch` 마일스톤 Issue에 남긴다. GitHub Security의 원본 경보와 Actions 실행 이력이 상세 증거다.

Dependabot 자동 보안 수정 PR은 현재 비활성 상태로 유지한다. 기본 브랜치 `main`이 릴리즈 때만 `develop`을 받는 git-flow에서 자동 PR이 이미 수정된 `develop`과 중복되거나 역방향 변경을 만들 수 있기 때문이다. GitHub Actions·배포 Docker base image의 주간 예약 갱신 PR은 `.github/dependabot.yml`이 다음 정식 릴리즈로 기본 브랜치 `main`에 반영된 뒤 활성화되며, `develop`을 대상으로 명시하고 자동 머지는 사용하지 않는다. Gradle 예약 버전 PR은 새 체크섬을 만들 수 없으므로 활성화하지 않고, Dependabot 경보와 주간 점검에서 변경 필요성을 확인한 사람이 아래 절차로 의존성과 체크섬을 같은 브랜치에서 갱신한다. Dependabot 경보, PR `dependency-review`, 위 정기 점검을 유지하고 첫 운영 릴리즈 후 다음 조건을 모두 확인해 자동 보안 수정 PR 활성화를 재평가한다.

예약 갱신 PR의 Action commit SHA와 Docker digest는 공급자의 공식 릴리즈·레지스트리에서 태그 대응 관계를 다시 확인한다. 전체 CI와 해당 Docker build가 통과한 변경만 병합하며, 장애 시 새 핀만 임의 값으로 바꾸지 않고 직전 정상 핀으로 되돌리는 PR을 같은 게이트로 통과시킨다. PR에는 이전 핀, 새 핀, 원래 버전 태그와 검증 출처를 남긴다.

- 자동 PR 대상이 `develop` 흐름과 충돌하지 않는다.
- 생성 PR이 백엔드·프런트 전체 품질 게이트와 CodeQL을 통과한다.
- 락파일 변경이 직접·전이 의존성의 의도한 보안 버전만 포함한다.
- 릴리즈 실행자가 변경 로그와 런타임 호환성을 확인하며 자동 머지는 계속 금지한다.

### Gradle 의존성 체크섬 갱신

`backend/gradle/verification-metadata.xml`은 빌드 플러그인을 포함한 Gradle 아티팩트의 SHA-256 허용 목록이다. 의존성을 변경한 PR에서만 다음 명령으로 메타데이터를 갱신한다.

```bash
cd backend
./gradlew --write-verification-metadata sha256 check
git diff -- gradle/verification-metadata.xml
./gradlew check
```

생성 결과를 그대로 승인하지 않는다. diff가 의도한 의존성·버전과 전이 의존성에 한정되는지 확인하고, 출처를 설명할 수 없는 컴포넌트나 기존 체크섬 교체는 병합하지 않는다. 의도하지 않은 변경이면 의존성 변경과 메타데이터 변경을 함께 되돌린다. 메타데이터에 없는 아티팩트나 체크섬이 다른 아티팩트는 Gradle이 빌드 전에 거부해야 한다.

## 검증 기록

- 2026-07-13: 로컬 PostgreSQL 16의 전체 ERP+Keycloak DB를 별도 임시 DB에 custom-format으로 복원하고, Flyway 및 6개 핵심 데이터 집계를 원본과 대조해 통과. 임시 DB는 검증 직후 삭제했다. 이 결과는 스크립트 동작 증거이며 운영 백업·RPO/RTO 충족 증거는 아니다.
- 2026-07-16: develop `027c98fb0b02bb40619ef53e95bd5355a1804e17`의 로컬 PostgreSQL 16 ERP+Keycloak DB를 격리 임시 DB에 복원해 대조 통과. 백업 420,764 bytes, 복원 2초, 검증 1초 미만, 복원+검증 2초, 전체 리허설 4초였으며 임시 DB는 즉시 삭제했다. 로컬 합성·UAT 데이터 결과이므로 운영 용량 RTO 증거가 아니다.
