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

## 장애 복구 순서

1. 쓰기 트래픽을 차단하고 사고 시작 시각·영향 테넌트·마지막 정상 배포 SHA를 기록한다.
2. 원본 DB를 변경하지 않은 채 최신 백업을 새 DB에 복원한다.
3. 위 검증 스크립트와 `./gradlew check`를 통과시킨다.
4. 스테이징 백엔드를 복원 DB에 연결해 헬스체크와 실 Keycloak E2E를 수행한다.
5. 데이터 손실 구간과 복구 시각을 확인한 뒤에만 연결 전환 여부를 승인받는다.
6. 복구 후 원인·영향·타임라인·재발방지를 저장소 이슈/사고 기록에 남긴다.

## 출시 전 필수 결정

첫 유료 계약 전에 제품 소유자가 RPO, RTO, 백업 주기, 보존 기간, 지원 채널·시간을 확정해 계약·운영 설정과 일치시켜야 한다. 값이 정해지지 않은 상태에서는 유료 운영 출시를 승인하지 않는다.

## 검증 기록

- 2026-07-13: 로컬 PostgreSQL 16의 전체 ERP+Keycloak DB를 별도 임시 DB에 custom-format으로 복원하고, Flyway 및 6개 핵심 데이터 집계를 원본과 대조해 통과. 임시 DB는 검증 직후 삭제했다. 이 결과는 스크립트 동작 증거이며 운영 백업·RPO/RTO 충족 증거는 아니다.
