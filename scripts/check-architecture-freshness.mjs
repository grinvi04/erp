#!/usr/bin/env node
import {
  copyFileSync,
  existsSync,
  mkdirSync,
  mkdtempSync,
  readFileSync,
  rmSync,
} from 'node:fs'
import { tmpdir } from 'node:os'
import { join, resolve } from 'node:path'
import { spawnSync } from 'node:child_process'

const root = resolve(process.argv[2] ?? process.cwd())
const generator = join(root, 'docs', 'gen_arch_svg.py')
const committed = join(root, 'docs', 'architecture.svg')

if (!existsSync(generator) || !existsSync(committed)) {
  console.error('docs/gen_arch_svg.py와 docs/architecture.svg가 모두 필요합니다.')
  process.exit(1)
}

const scratch = mkdtempSync(join(tmpdir(), 'erp-architecture-check-'))
try {
  const scratchDocs = join(scratch, 'docs')
  mkdirSync(scratchDocs)
  copyFileSync(generator, join(scratchDocs, 'gen_arch_svg.py'))

  const generated = spawnSync('python3', ['docs/gen_arch_svg.py'], {
    cwd: scratch,
    encoding: 'utf8',
  })
  if (generated.status !== 0) {
    console.error('아키텍처 SVG 생성기가 실패했습니다.')
    process.stderr.write(generated.stderr)
    process.exit(1)
  }

  const expected = readFileSync(join(scratchDocs, 'architecture.svg'))
  const actual = readFileSync(committed)
  if (!expected.equals(actual)) {
    console.error('docs/architecture.svg가 생성기 결과와 다릅니다.')
    console.error('python3 docs/gen_arch_svg.py 실행 후 변경을 커밋하세요.')
    process.exit(1)
  }

  console.log('✓ 아키텍처 SVG가 생성기 결과와 일치합니다.')
} finally {
  rmSync(scratch, { recursive: true, force: true })
}
