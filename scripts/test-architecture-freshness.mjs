#!/usr/bin/env node
import { mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { spawnSync } from 'node:child_process'

const root = mkdtempSync(join(tmpdir(), 'erp-architecture-freshness-'))
const docs = join(root, 'docs')
const checker = join(import.meta.dirname, 'check-architecture-freshness.mjs')

function run() {
  return spawnSync(process.execPath, [checker, root], { encoding: 'utf8' })
}

function expectExit(expected, description) {
  const result = run()
  if (result.status !== expected) {
    console.error(`${description}: expected exit ${expected}, got ${result.status}`)
    console.error(result.stdout)
    console.error(result.stderr)
    process.exit(1)
  }
}

try {
  mkdirSync(docs)
  writeFileSync(
    join(docs, 'gen_arch_svg.py'),
    'from pathlib import Path\nPath("docs/architecture.svg").write_text("generated\\n")\n',
  )
  writeFileSync(join(docs, 'architecture.svg'), 'generated\n')
  expectExit(0, 'matching generated architecture')

  writeFileSync(join(docs, 'architecture.svg'), 'stale\n')
  expectExit(1, 'stale generated architecture')
  if (readFileSync(join(docs, 'architecture.svg'), 'utf8') !== 'stale\n') {
    console.error('checker must not modify the source worktree')
    process.exit(1)
  }

  writeFileSync(join(docs, 'gen_arch_svg.py'), 'raise RuntimeError("generation failed")\n')
  expectExit(1, 'generator failure')
} finally {
  rmSync(root, { recursive: true, force: true })
}

console.log('✓ 아키텍처 SVG 신선도 게이트 회귀 테스트 통과')
