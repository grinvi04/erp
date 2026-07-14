import { describe, expect, it } from 'vitest'

import {
  assertCommercialUatIdentityTopology,
  COMMERCIAL_UAT_MUTATION_CONFIRMATION,
  createCommercialUatRunId,
  isCommercialUatEnabled,
  loadCommercialUatConfig,
} from './commercial-uat'

const validEnv = (): Record<string, string | undefined> => ({
  E2E_COMMERCIAL: '1',
  E2E_COMMERCIAL_MUTATION: COMMERCIAL_UAT_MUTATION_CONFIRMATION,
  E2E_COMMERCIAL_FRONTEND_URL: 'http://localhost:3000',
  E2E_COMMERCIAL_BACKEND_URL: 'http://127.0.0.1:8080',
  E2E_COMMERCIAL_KC_ISSUER: 'http://localhost:8180/realms/erp',
  E2E_CLIENT_SECRET: 'client-secret',
  AUTH_SECRET: 'auth-secret',
  E2E_COMMERCIAL_KC_ADMIN_USERNAME: 'local-kc-admin',
  E2E_COMMERCIAL_KC_ADMIN_PASSWORD: 'local-kc-password',
  E2E_COMMERCIAL_CREATOR_USERNAME: 'uat-creator',
  E2E_COMMERCIAL_CREATOR_PASSWORD: 'creator-password',
  E2E_COMMERCIAL_APPROVER_USERNAME: 'uat-approver',
  E2E_COMMERCIAL_APPROVER_PASSWORD: 'approver-password',
  E2E_COMMERCIAL_TENANT_B_USERNAME: 'uat-tenant-b',
  E2E_COMMERCIAL_TENANT_B_PASSWORD: 'tenant-b-password',
})

describe('commercial UAT environment contract', () => {
  it('enables mutation only for the exact opt-in value', () => {
    expect(isCommercialUatEnabled({ E2E_COMMERCIAL: '1' })).toBe(true)
    expect(isCommercialUatEnabled({ E2E_COMMERCIAL: 'true' })).toBe(false)
    expect(isCommercialUatEnabled({})).toBe(false)
  })

  it('loads a complete localhost-only configuration', () => {
    const config = loadCommercialUatConfig(
      validEnv(),
      new Date('2026-07-14T01:02:03.456Z'),
      () => '12345678-1234-1234-1234-123456789abc',
    )

    expect(config.frontendUrl).toBe('http://localhost:3000')
    expect(config.backendUrl).toBe('http://127.0.0.1:8080')
    expect(config.runId).toBe('uat-20260714t010203456z-12345678')
  })

  it.each([
    ['missing mutation confirmation', { E2E_COMMERCIAL_MUTATION: undefined }],
    ['wrong mutation confirmation', { E2E_COMMERCIAL_MUTATION: 'yes' }],
    ['missing credential', { E2E_COMMERCIAL_APPROVER_PASSWORD: undefined }],
    ['missing Keycloak admin credential', { E2E_COMMERCIAL_KC_ADMIN_PASSWORD: undefined }],
    ['remote backend', { E2E_COMMERCIAL_BACKEND_URL: 'https://erp.example.com' }],
    ['remote Keycloak', { E2E_COMMERCIAL_KC_ISSUER: 'https://login.example.com/realms/erp' }],
    ['URL credentials', { E2E_COMMERCIAL_BACKEND_URL: 'http://user:pass@localhost:8080' }],
  ])('rejects %s before mutation', (_label, override) => {
    expect(() => loadCommercialUatConfig({ ...validEnv(), ...override })).toThrow()
  })
})

describe('commercial UAT identity topology', () => {
  const creator = { subject: 'creator-sub', tenantId: '101' }
  const approver = { subject: 'approver-sub', tenantId: '101' }
  const tenantB = { subject: 'tenant-b-sub', tenantId: '202' }

  it('accepts distinct users in tenant A and a separate tenant B', () => {
    expect(() =>
      assertCommercialUatIdentityTopology(creator, approver, tenantB),
    ).not.toThrow()
  })

  it.each([
    ['same creator and approver', creator, creator, tenantB],
    ['approver in another tenant', creator, { ...approver, tenantId: '999' }, tenantB],
    ['tenant B matching tenant A', creator, approver, { ...tenantB, tenantId: '101' }],
    ['blank subject', { ...creator, subject: ' ' }, approver, tenantB],
  ])('rejects %s', (_label, a, b, c) => {
    expect(() => assertCommercialUatIdentityTopology(a, b, c)).toThrow()
  })
})

describe('commercial UAT run id', () => {
  it('contains UTC time and UUID entropy without secret data', () => {
    expect(
      createCommercialUatRunId(
        new Date('2026-07-14T01:02:03.456Z'),
        () => 'abcdef12-3456-7890-abcd-ef1234567890',
      ),
    ).toBe('uat-20260714t010203456z-abcdef12')
  })
})
