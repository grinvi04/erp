# ERP

> Multi-tenant SaaS ERP — HR · Finance · Inventory · CRM

[![ci-gate](https://github.com/grinvi04/erp/actions/workflows/ci-gate.yml/badge.svg)](https://github.com/grinvi04/erp/actions/workflows/ci-gate.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## Overview

수백 개 기업(테넌트) × 수백 명 사용자 규모의 실제 상용 SaaS ERP.

| 모듈 | 핵심 기능 |
|------|-----------|
| HR/HCM | 조직·직원·계약·휴가·결재·이력 |
| Finance | 계정과목·전표(GL)·AP/AR·예산·회계기간 |
| Inventory | 품목·창고·재고원장·입출이동·로트/시리얼 |
| CRM | 고객사·담당자·리드→영업기회·파이프라인·활동 |

## Architecture

```
backend/   Spring Boot 3.4 · Java 21 · Gradle · PostgreSQL 16 · Flyway · Keycloak
frontend/  Next.js 15 (App Router) · TypeScript · next-auth v5 · Tailwind · shadcn/ui
```

**Multi-tenancy**: Row-level isolation — `tenant_id` on every table + Hibernate `@TenantId` auto-filter.  
**Auth**: Keycloak (single realm) — `tenant_id` as JWT custom claim → `TenantContext` (ThreadLocal).  
**Audit**: `BaseEntity` — `version` (optimistic lock) + `deleted_at` (soft delete) + audit fields.

## Getting Started

```bash
# 1. 로컬 인프라 실행
docker compose up -d

# 2. 백엔드 실행
cd backend && ./gradlew bootRun

# 3. 프론트엔드 실행
cd frontend && npm install && npm run dev
```

백엔드: http://localhost:8081  
프론트엔드: http://localhost:3000  
Keycloak Admin: http://localhost:8080 (admin/admin)

## Development

```bash
# 백엔드 전체 품질 검증 (lint + test)
cd backend && ./gradlew check

# 프론트엔드 타입 체크 + 린트 + 빌드
cd frontend && npm run type-check && npm run lint && npm run build
```

## License

MIT © grinvi04
