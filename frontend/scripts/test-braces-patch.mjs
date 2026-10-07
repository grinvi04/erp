import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import { cpSync, mkdtempSync, readFileSync, rmSync, symlinkSync, writeFileSync } from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'
import test from 'node:test'
import { applyBracesPatch, verifyBracesPatch } from './apply-braces-depth.mjs'

const installedRoot = fileURLToPath(new URL('../node_modules/braces/', import.meta.url))

function withPackageCopy(run) {
  const directory = mkdtempSync(path.join(os.tmpdir(), 'drivetree-braces-'))
  const copy = path.join(directory, 'braces')
  try {
    cpSync(installedRoot, copy, { recursive: true })
    run(copy)
  } finally {
    rmSync(directory, { recursive: true, force: true })
  }
}

test('patched bytes are verified and a second application changes no files', () => {
  withPackageCopy((copy) => {
    verifyBracesPatch(copy)
    assert.equal(applyBracesPatch(copy), 0)
  })
})

test('unexpected source bytes fail before any other installed file changes', () => {
  withPackageCopy((copy) => {
    const target = path.join(copy, 'lib/utils.js')
    writeFileSync(target, readFileSync(target, 'utf8') + '\n// unexpected change\n')
    const before = readFileSync(path.join(copy, 'lib/parse.js'))
    assert.throws(() => applyBracesPatch(copy), /unexpected source bytes/)
    assert.deepEqual(readFileSync(path.join(copy, 'lib/parse.js')), before)
  })
})

test('a new package version is refused instead of silently patching it', () => {
  withPackageCopy((copy) => {
    const target = path.join(copy, 'package.json')
    const pkg = JSON.parse(readFileSync(target, 'utf8'))
    writeFileSync(target, JSON.stringify({ ...pkg, version: '3.0.4' }))
    assert.throws(() => applyBracesPatch(copy), /braces version changed/)
  })
})

test('a symlinked invocation performs its explicit integrity check', () => {
  const directory = mkdtempSync(path.join(os.tmpdir(), 'drivetree-braces-link-'))
  try {
    const link = path.join(directory, 'patch.mjs')
    symlinkSync(fileURLToPath(new URL('./apply-braces-depth.mjs', import.meta.url)), link)
    const result = spawnSync(process.execPath, [link, '--check'], {
      encoding: 'utf8',
      timeout: 2000,
    })
    assert.equal(result.status, 0, result.stderr)
    assert.match(result.stdout, /braces depth patch verified/)
  } finally {
    rmSync(directory, { recursive: true, force: true })
  }
})

test('only an omitted dev dependency may skip the install patch', () => {
  const directory = mkdtempSync(path.join(os.tmpdir(), 'drivetree-braces-omit-'))
  try {
    const script = path.join(directory, 'apply-braces-depth.mjs')
    cpSync(fileURLToPath(new URL('./apply-braces-depth.mjs', import.meta.url)), script)
    const run = (args, omit) =>
      spawnSync(process.execPath, [script, ...args], {
        encoding: 'utf8',
        env: { ...process.env, npm_config_omit: omit },
        timeout: 2000,
      })
    const omitted = run([], 'dev')
    assert.equal(omitted.status, 0, omitted.stderr)
    assert.match(omitted.stdout, /dev dependencies omitted and braces is absent/)
    assert.notEqual(run([], '').status, 0)
    assert.notEqual(run(['--check'], 'dev').status, 0)
  } finally {
    rmSync(directory, { recursive: true, force: true })
  }
})

test('stdin module import does not execute or change installed source', () => {
  const patchFile = fileURLToPath(new URL('./apply-braces-depth.mjs', import.meta.url))
  const sourceFiles = ['parse', 'compile', 'expand', 'stringify', 'utils', 'constants'].map(
    (name) => path.join(installedRoot, `lib/${name}.js`),
  )
  const before = sourceFiles.map((file) => readFileSync(file))
  const result = spawnSync(process.execPath, ['--input-type=module', '-'], {
    input: `process.argv[1] = '-'; await import(${JSON.stringify(pathToFileURL(patchFile).href)});`,
    encoding: 'utf8',
    timeout: 2000,
  })
  assert.equal(result.status, 0, result.stderr)
  assert.equal(result.stdout, '')
  assert.equal(result.stderr, '')
  assert.deepEqual(
    sourceFiles.map((file) => readFileSync(file)),
    before,
  )
})
