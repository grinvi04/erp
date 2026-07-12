#!/usr/bin/env node
import { mkdtempSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { spawnSync } from 'node:child_process'

const root = mkdtempSync(join(tmpdir(), 'erp-ddl-gate-'))

function run(script) {
  return spawnSync(process.execPath, [join(import.meta.dirname, script), root], {
    encoding: 'utf8',
  })
}

function expectExit(script, expected, description) {
  const result = run(script)
  if (result.status !== expected) {
    console.error(`${description}: expected exit ${expected}, got ${result.status}`)
    console.error(result.stdout)
    console.error(result.stderr)
    process.exit(1)
  }
}

try {
  const alembic = join(root, 'migration.py')
  writeFileSync(
    alembic,
    "from alembic import op\nrevision = '1'\ndef upgrade(): op.drop_table('customer')\n",
  )
  expectExit(
    'check-alembic-destructive-ddl.mjs',
    1,
    'inline Alembic upgrade destructive operation',
  )

  writeFileSync(
    alembic,
    "from alembic import op\nrevision = '1'\ndef upgrade(): op.drop_table('customer')  # migration-safety: destructive-ok\n",
  )
  expectExit('check-alembic-destructive-ddl.mjs', 0, 'approved inline Alembic operation')

  writeFileSync(
    alembic,
    "from alembic import op\nrevision = '1'\ndef upgrade(_=(1, 2)): op.drop_table('customer')\n",
  )
  expectExit(
    'check-alembic-destructive-ddl.mjs',
    1,
    'inline Alembic operation after nested signature',
  )

  rmSync(alembic)
  const activeRecord = join(root, 'migration.rb')
  writeFileSync(
    activeRecord,
    'class Example < ActiveRecord::Migration[7.0]\n  def up = drop_table(:customer)\nend\n',
  )
  expectExit(
    'check-activerecord-destructive-ddl.mjs',
    1,
    'endless ActiveRecord destructive operation',
  )

  writeFileSync(
    activeRecord,
    'class Example < ActiveRecord::Migration[7.0]\n  def up = drop_table(:customer) # migration-safety: destructive-ok\nend\n',
  )
  expectExit(
    'check-activerecord-destructive-ddl.mjs',
    0,
    'approved endless ActiveRecord operation',
  )

  writeFileSync(
    activeRecord,
    'class Example < ActiveRecord::Migration[7.0]\n  def up(_ = nil) = drop_table(:customer)\nend\n',
  )
  expectExit(
    'check-activerecord-destructive-ddl.mjs',
    1,
    'endless ActiveRecord operation after parenthesized parameters',
  )
} finally {
  rmSync(root, { recursive: true, force: true })
}

console.log('✓ 파괴적 DDL 언어별 우회 회귀 테스트 통과')
