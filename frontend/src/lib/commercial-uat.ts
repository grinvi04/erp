export const COMMERCIAL_UAT_MUTATION_CONFIRMATION = 'LOCAL_MUTATION_ACCEPTED'

type CommercialUatEnvironment = Readonly<Record<string, string | undefined>>

export type CommercialUatIdentity = {
  subject: string
  tenantId: string
}

export type CommercialUatConfig = {
  frontendUrl: string
  backendUrl: string
  keycloakIssuer: string
  runId: string
  clientSecret: string
  authSecret: string
  keycloakAdminUsername: string
  keycloakAdminPassword: string
  creatorUsername: string
  creatorPassword: string
  approverUsername: string
  approverPassword: string
  tenantBUsername: string
  tenantBPassword: string
}

export function isCommercialUatEnabled(env: CommercialUatEnvironment): boolean {
  return env.E2E_COMMERCIAL === '1'
}

export function loadCommercialUatConfig(
  env: CommercialUatEnvironment,
  now: Date = new Date(),
  uuid: () => string = () => crypto.randomUUID(),
): CommercialUatConfig {
  if (!isCommercialUatEnabled(env)) {
    throw new Error('[commercial-uat] E2E_COMMERCIAL=1 명시적 활성화가 필요합니다.')
  }
  if (env.E2E_COMMERCIAL_MUTATION !== COMMERCIAL_UAT_MUTATION_CONFIRMATION) {
    throw new Error(
      `[commercial-uat] E2E_COMMERCIAL_MUTATION=${COMMERCIAL_UAT_MUTATION_CONFIRMATION} 확인값이 필요합니다.`,
    )
  }

  const frontendUrl = requireLocalUrl(
    env.E2E_COMMERCIAL_FRONTEND_URL ?? 'http://localhost:3000',
    'E2E_COMMERCIAL_FRONTEND_URL',
  )
  const backendUrl = requireLocalUrl(
    env.E2E_COMMERCIAL_BACKEND_URL ?? 'http://localhost:8080',
    'E2E_COMMERCIAL_BACKEND_URL',
  )
  const keycloakIssuer = requireLocalUrl(
    env.E2E_COMMERCIAL_KC_ISSUER ?? 'http://localhost:8180/realms/erp',
    'E2E_COMMERCIAL_KC_ISSUER',
  )
  const creatorUsername = requireEnv(env, 'E2E_COMMERCIAL_CREATOR_USERNAME')
  const approverUsername = requireEnv(env, 'E2E_COMMERCIAL_APPROVER_USERNAME')
  if (creatorUsername === approverUsername) {
    throw new Error('[commercial-uat] 작성자와 결재자 사용자명이 달라야 합니다.')
  }

  return {
    frontendUrl,
    backendUrl,
    keycloakIssuer,
    runId: createCommercialUatRunId(now, uuid),
    clientSecret: requireEnv(env, 'E2E_CLIENT_SECRET'),
    authSecret: requireEnv(env, 'AUTH_SECRET'),
    keycloakAdminUsername: requireEnv(env, 'E2E_COMMERCIAL_KC_ADMIN_USERNAME'),
    keycloakAdminPassword: requireEnv(env, 'E2E_COMMERCIAL_KC_ADMIN_PASSWORD'),
    creatorUsername,
    creatorPassword: requireEnv(env, 'E2E_COMMERCIAL_CREATOR_PASSWORD'),
    approverUsername,
    approverPassword: requireEnv(env, 'E2E_COMMERCIAL_APPROVER_PASSWORD'),
    tenantBUsername: requireEnv(env, 'E2E_COMMERCIAL_TENANT_B_USERNAME'),
    tenantBPassword: requireEnv(env, 'E2E_COMMERCIAL_TENANT_B_PASSWORD'),
  }
}

export function assertCommercialUatIdentityTopology(
  creator: CommercialUatIdentity,
  approver: CommercialUatIdentity,
  tenantB: CommercialUatIdentity,
): void {
  for (const [label, identity] of [
    ['작성자', creator],
    ['결재자', approver],
    ['테넌트 B 사용자', tenantB],
  ] as const) {
    if (!identity.subject.trim() || !identity.tenantId.trim()) {
      throw new Error(`[commercial-uat] ${label} 토큰의 subject와 tenant_id가 필요합니다.`)
    }
  }
  if (creator.subject === approver.subject) {
    throw new Error('[commercial-uat] 작성자와 결재자 subject가 달라야 합니다.')
  }
  if (creator.tenantId !== approver.tenantId) {
    throw new Error('[commercial-uat] 작성자와 결재자는 동일한 테넌트 A여야 합니다.')
  }
  if (tenantB.tenantId === creator.tenantId) {
    throw new Error('[commercial-uat] 테넌트 B는 테넌트 A와 달라야 합니다.')
  }
}

export function createCommercialUatRunId(now: Date, uuid: () => string): string {
  const timestamp = now.toISOString().replace(/[-:.]/g, '').toLowerCase()
  const entropy = uuid().replaceAll('-', '').toLowerCase()
  if (!/^[0-9a-f]{32}$/.test(entropy)) {
    throw new Error('[commercial-uat] 실행 ID 생성을 위한 UUID가 유효하지 않습니다.')
  }
  return `uat-${timestamp}-${entropy.slice(0, 8)}`
}

function requireEnv(env: CommercialUatEnvironment, name: string): string {
  const value = env[name]
  if (value == null || !value.trim()) {
    throw new Error(`[commercial-uat] 필수 환경변수 ${name}가 필요합니다.`)
  }
  return value
}

function requireLocalUrl(value: string, name: string): string {
  let url: URL
  try {
    url = new URL(value)
  } catch {
    throw new Error(`[commercial-uat] ${name}는 유효한 로컬 URL이어야 합니다.`)
  }
  const loopbackHosts = new Set(['localhost', '127.0.0.1', '[::1]'])
  if (
    url.protocol !== 'http:' ||
    !loopbackHosts.has(url.hostname) ||
    url.username ||
    url.password
  ) {
    throw new Error(`[commercial-uat] ${name}는 자격증명 없는 HTTP loopback URL이어야 합니다.`)
  }
  return value
}
