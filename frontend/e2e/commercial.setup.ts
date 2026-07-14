import { expect, test as setup } from '@playwright/test'
import { spawnSync } from 'node:child_process'
import path from 'node:path'

import {
  assertCommercialUatIdentityTopology,
  loadCommercialUatConfig,
  type CommercialUatConfig,
  type CommercialUatIdentity,
} from '../src/lib/commercial-uat'

type KeycloakUser = {
  id: string
  username: string
  attributes?: Record<string, string[]>
}

type Role = { id: number; code: string }
type Envelope<T> = { success: boolean; data: T }

const config = loadCommercialUatConfig(process.env)
const realmUrl = new URL(config.keycloakIssuer)
const realm = realmUrl.pathname.split('/').filter(Boolean).at(-1)
if (!realm) throw new Error('[commercial.setup] Keycloak realm을 URL에서 확인할 수 없습니다.')
const keycloakBaseUrl = realmUrl.origin

setup('prepare isolated commercial UAT tenants and users', async () => {
  if (process.env.E2E_COMMERCIAL_DRY_RUN === '1') {
    expect(config.runId).toMatch(/^uat-[0-9]{8}t[0-9]{9}z-[0-9a-f]{8}$/)
    return
  }

  const adminToken = await keycloakAdminToken(config)
  const provisionerSecret = await keycloakClientSecret(adminToken, 'erp-provisioner')
  const creator = await ensureUser(adminToken, config.creatorUsername, config.creatorPassword)
  const approver = await ensureUser(adminToken, config.approverUsername, config.approverPassword)
  const tenantBAdmin = await ensureUser(
    adminToken,
    config.tenantBUsername,
    config.tenantBPassword,
  )

  const tenantAId =
    tenantIdOf(creator) ??
    (await provisionTenant(
      adminToken,
      creator.id,
      'UAT_A',
      'Commercial UAT A',
      provisionerSecret,
    ))
  await assignTenant(adminToken, approver, tenantAId)
  const tenantBId =
    tenantIdOf(tenantBAdmin) ??
    (await provisionTenant(
      adminToken,
      tenantBAdmin.id,
      'UAT_B',
      'Commercial UAT B',
      provisionerSecret,
    ))

  const creatorToken = await userToken(config.creatorUsername, config.creatorPassword)
  const approverToken = await userToken(config.approverUsername, config.approverPassword)
  const tenantBToken = await userToken(config.tenantBUsername, config.tenantBPassword)
  const creatorIdentity = identityFromToken(creatorToken)
  const approverIdentity = identityFromToken(approverToken)
  const tenantBIdentity = identityFromToken(tenantBToken)
  assertCommercialUatIdentityTopology(creatorIdentity, approverIdentity, tenantBIdentity)
  if (creatorIdentity.tenantId !== tenantAId || tenantBIdentity.tenantId !== tenantBId) {
    throw new Error('[commercial.setup] 프로비저닝 결과와 JWT tenant_id가 일치하지 않습니다.')
  }

  await ensureSuperAdmin(creatorToken, approverIdentity.subject)
  await setAccessProfile(creatorToken, creatorIdentity.subject)
  await setAccessProfile(creatorToken, approverIdentity.subject)
  await setAccessProfile(tenantBToken, tenantBIdentity.subject)

  await verifyPermissions(creatorToken)
  await verifyPermissions(approverToken)
  await verifyPermissions(tenantBToken)
})

async function keycloakAdminToken(value: CommercialUatConfig): Promise<string> {
  const response = await fetch(`${keycloakBaseUrl}/realms/master/protocol/openid-connect/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      grant_type: 'password',
      client_id: 'admin-cli',
      username: value.keycloakAdminUsername,
      password: value.keycloakAdminPassword,
    }),
  })
  return tokenFrom(response, 'Keycloak 관리자 인증')
}

async function keycloakClientSecret(adminToken: string, clientId: string): Promise<string> {
  const clients = await keycloakJson<Array<{ id: string }>>(
    `${keycloakBaseUrl}/admin/realms/${realm}/clients?clientId=${encodeURIComponent(clientId)}`,
    adminToken,
    '프로비저닝 클라이언트 조회',
  )
  if (clients.length !== 1) {
    throw new Error('[commercial.setup] erp-provisioner 클라이언트를 하나만 찾을 수 있어야 합니다.')
  }
  const secret = await keycloakJson<{ value?: string }>(
    `${keycloakBaseUrl}/admin/realms/${realm}/clients/${clients[0].id}/client-secret`,
    adminToken,
    '프로비저닝 클라이언트 시크릿 조회',
  )
  if (!secret.value) throw new Error('[commercial.setup] 프로비저닝 클라이언트 시크릿이 없습니다.')
  return secret.value
}

async function ensureUser(
  adminToken: string,
  username: string,
  password: string,
): Promise<KeycloakUser> {
  let user = await findUser(adminToken, username)
  if (!user) {
    await keycloakRequest(
      `${keycloakBaseUrl}/admin/realms/${realm}/users`,
      adminToken,
      '사용자 생성',
      {
        method: 'POST',
        body: JSON.stringify({ username, enabled: true, emailVerified: true }),
      },
    )
    user = await findUser(adminToken, username)
  }
  if (!user) throw new Error('[commercial.setup] 생성한 Keycloak 사용자를 찾을 수 없습니다.')
  const profile = {
    ...user,
    email: `${username.toLowerCase().replace(/[^a-z0-9.-]/g, '-') || 'uat-user'}@uat.erp.local`,
    emailVerified: true,
    firstName: 'Commercial',
    lastName: 'UAT',
    enabled: true,
    requiredActions: [],
  }
  await keycloakRequest(
    `${keycloakBaseUrl}/admin/realms/${realm}/users/${user.id}`,
    adminToken,
    '사용자 프로필 보정',
    { method: 'PUT', body: JSON.stringify(profile) },
  )
  await keycloakRequest(
    `${keycloakBaseUrl}/admin/realms/${realm}/users/${user.id}/reset-password`,
    adminToken,
    '사용자 비밀번호 설정',
    {
      method: 'PUT',
      body: JSON.stringify({ type: 'password', value: password, temporary: false }),
    },
  )
  return profile
}

async function findUser(adminToken: string, username: string): Promise<KeycloakUser | undefined> {
  const users = await keycloakJson<KeycloakUser[]>(
    `${keycloakBaseUrl}/admin/realms/${realm}/users?exact=true&username=${encodeURIComponent(username)}`,
    adminToken,
    '사용자 조회',
  )
  return users.find((user) => user.username === username)
}

async function assignTenant(adminToken: string, user: KeycloakUser, tenantId: string) {
  const current = tenantIdOf(user)
  if (current === tenantId) return
  if (current) throw new Error('[commercial.setup] 사용자가 이미 다른 테넌트에 속해 있습니다.')
  await keycloakRequest(
    `${keycloakBaseUrl}/admin/realms/${realm}/users/${user.id}`,
    adminToken,
    '사용자 테넌트 배정',
    {
      method: 'PUT',
      body: JSON.stringify({ ...user, attributes: { ...user.attributes, tenant_id: [tenantId] } }),
    },
  )
}

function tenantIdOf(user: KeycloakUser): string | undefined {
  const values = user.attributes?.tenant_id
  return values?.length === 1 && values[0] ? values[0] : undefined
}

async function provisionTenant(
  adminToken: string,
  adminUserId: string,
  code: string,
  name: string,
  provisionerSecret: string,
): Promise<string> {
  const result = spawnSync('./gradlew', ['provisionTenant'], {
    cwd: path.resolve(process.cwd(), '..', 'backend'),
    encoding: 'utf8',
    env: {
      ...process.env,
      ERP_PROVISION_TENANT_CODE: code,
      ERP_PROVISION_TENANT_NAME: name,
      ERP_PROVISION_TENANT_PLAN: 'STANDARD',
      ERP_PROVISION_ADMIN_USER_ID: adminUserId,
      ERP_PROVISIONED_BY: 'commercial-uat-setup',
      ERP_KEYCLOAK_BASE_URL: keycloakBaseUrl,
      ERP_KEYCLOAK_REALM: realm,
      ERP_KEYCLOAK_PROVISIONING_CLIENT_ID: 'erp-provisioner',
      ERP_KEYCLOAK_PROVISIONING_CLIENT_SECRET: provisionerSecret,
    },
  })
  if (result.status !== 0) {
    throw new Error(`[commercial.setup] ${code} 프로비저닝 실패(exit=${result.status ?? 'signal'}).`)
  }
  const refreshed = await keycloakJson<KeycloakUser>(
    `${keycloakBaseUrl}/admin/realms/${realm}/users/${adminUserId}`,
    adminToken,
    '프로비저닝 사용자 재조회',
  )
  const tenantId = tenantIdOf(refreshed)
  if (!tenantId) throw new Error(`[commercial.setup] ${code} tenant_id를 확인할 수 없습니다.`)
  return tenantId
}

async function userToken(username: string, password: string): Promise<string> {
  const response = await fetch(`${config.keycloakIssuer}/protocol/openid-connect/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      grant_type: 'password',
      client_id: process.env.E2E_CLIENT_ID ?? 'erp-frontend',
      client_secret: config.clientSecret,
      username,
      password,
      scope: 'openid',
    }),
  })
  return tokenFrom(response, 'UAT 사용자 인증')
}

async function tokenFrom(response: Response, label: string): Promise<string> {
  if (!response.ok) throw new Error(`[commercial.setup] ${label} 실패(HTTP ${response.status}).`)
  const body = (await response.json()) as { access_token?: string }
  if (!body.access_token) throw new Error(`[commercial.setup] ${label} 응답에 access token이 없습니다.`)
  return body.access_token
}

function identityFromToken(token: string): CommercialUatIdentity {
  const encoded = token.split('.')[1]
  if (!encoded) throw new Error('[commercial.setup] JWT payload가 없습니다.')
  const claims = JSON.parse(Buffer.from(encoded, 'base64url').toString('utf8')) as {
    sub?: string
    tenant_id?: string | number
  }
  return { subject: claims.sub ?? '', tenantId: String(claims.tenant_id ?? '') }
}

async function ensureSuperAdmin(adminToken: string, userId: string) {
  const roles = await backendJson<Role[]>(adminToken, '/api/iam/roles')
  const superAdmin = roles.find((role) => role.code === 'SUPER_ADMIN')
  if (!superAdmin) throw new Error('[commercial.setup] SUPER_ADMIN 역할이 없습니다.')
  const assigned = await backendJson<Role[]>(
    adminToken,
    `/api/iam/users/${encodeURIComponent(userId)}/roles`,
  )
  if (assigned.some((role) => role.id === superAdmin.id)) return
  await backendRequest(
    adminToken,
    `/api/iam/users/${encodeURIComponent(userId)}/roles/${superAdmin.id}`,
    { method: 'POST' },
  )
}

async function setAccessProfile(token: string, userId: string) {
  await backendJson(
    token,
    `/api/iam/users/${encodeURIComponent(userId)}/access-profile`,
    {
      method: 'PUT',
      body: JSON.stringify({ dataScope: 'ALL', departmentId: null, approvalLimit: '1000000000' }),
    },
  )
}

async function verifyPermissions(token: string) {
  const permissions = await backendJson<string[]>(token, '/api/me/permissions')
  for (const required of ['iam:read', 'iam:write', 'audit:read']) {
    if (!permissions.includes(required)) {
      throw new Error(`[commercial.setup] 준비된 사용자에게 ${required} 권한이 없습니다.`)
    }
  }
}

async function backendJson<T>(token: string, pathname: string, init: RequestInit = {}): Promise<T> {
  const response = await backendRequest(token, pathname, init)
  const envelope = (await response.json()) as Envelope<T>
  if (!envelope.success) throw new Error('[commercial.setup] 백엔드 응답 envelope가 실패했습니다.')
  return envelope.data
}

async function backendRequest(token: string, pathname: string, init: RequestInit = {}) {
  const response = await fetch(`${config.backendUrl}${pathname}`, {
    ...init,
    headers: {
      Authorization: `Bearer ${token}`,
      ...(init.body ? { 'Content-Type': 'application/json' } : {}),
      ...init.headers,
    },
  })
  if (!response.ok) {
    throw new Error(`[commercial.setup] 백엔드 요청 실패(HTTP ${response.status}, path=${pathname}).`)
  }
  return response
}

async function keycloakJson<T>(
  url: string,
  token: string,
  label: string,
  init: RequestInit = {},
): Promise<T> {
  const response = await keycloakRequest(url, token, label, init)
  return (await response.json()) as T
}

async function keycloakRequest(
  url: string,
  token: string,
  label: string,
  init: RequestInit = {},
) {
  const response = await fetch(url, {
    ...init,
    headers: {
      Authorization: `Bearer ${token}`,
      ...(init.body ? { 'Content-Type': 'application/json' } : {}),
      ...init.headers,
    },
  })
  if (!response.ok) {
    throw new Error(`[commercial.setup] ${label} 실패(HTTP ${response.status}).`)
  }
  return response
}
