import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const require = createRequire(import.meta.url)
const frontend = fileURLToPath(new URL('../', import.meta.url))
const braces = require('braces')
const { getRootDirs } = require('@next/eslint-plugin-next/dist/utils/get-root-dirs')

function boundedProbe(source) {
  const result = spawnSync(process.execPath, ['--stack_size=512', '-e', source], {
    cwd: frontend,
    encoding: 'utf8',
    timeout: 2000,
  })
  assert.equal(result.error, undefined)
  assert.equal(result.signal, null)
  assert.equal(result.status, 0, result.stderr)
  return JSON.parse(result.stdout.trim())
}

test('ordinary brace syntax and the Next ESLint root directory stay usable', () => {
  assert.equal(braces.compile('a/{x,y}/b'), 'a/(x|y)/b')
  assert.deepEqual(braces.expand('v{1..3}'), ['v1', 'v2', 'v3'])
  assert.deepEqual(braces.expand(String.raw`a\{x,y\}`), ['a{x,y}'])
  assert.equal(getRootDirs({ cwd: frontend, settings: { next: { rootDir: frontend } } }).length, 1)
})

test('nested patterns are rejected before recursive stack exhaustion', () => {
  const result = boundedProbe(`
    const braces = require('braces');
    const pattern = '{'.repeat(2000) + 'a,b' + '}'.repeat(2000);
    const report = {};
    for (const operation of ['parse', 'compile', 'expand', 'stringify']) {
      try { braces[operation](pattern); report[operation] = 'accepted'; }
      catch (error) { report[operation] = error.name; }
    }
    console.log(JSON.stringify(report));
  `)
  assert.deepEqual(result, {
    parse: 'SyntaxError',
    compile: 'SyntaxError',
    expand: 'SyntaxError',
    stringify: 'SyntaxError',
  })
})

test('direct AST input cannot bypass the traversal bound', () => {
  const result = boundedProbe(`
    const braces = require('braces');
    const report = {};
    for (const operation of ['compile', 'expand', 'stringify']) {
      let ast = { type: 'text', value: 'x' };
      for (let i = 0; i < 2000; i++) ast = { type: 'root', nodes: [ast] };
      try { braces[operation](ast); report[operation] = 'accepted'; }
      catch (error) { report[operation] = error.name; }
    }
    console.log(JSON.stringify(report));
  `)
  assert.deepEqual(result, {
    compile: 'SyntaxError',
    expand: 'SyntaxError',
    stringify: 'SyntaxError',
  })
})

test('Next ESLint rootDir passes the depth guard at its real consumer', () => {
  const result = boundedProbe(`
    const { getRootDirs } = require('@next/eslint-plugin-next/dist/utils/get-root-dirs');
    const pattern = '{'.repeat(2000) + 'a,b' + '}'.repeat(2000);
    try { getRootDirs({ cwd: process.cwd(), settings: { next: { rootDir: pattern } } });
      console.log(JSON.stringify({ outcome: 'accepted' })); }
    catch (error) { console.log(JSON.stringify({ outcome: error.name })); }
  `)
  assert.deepEqual(result, { outcome: 'SyntaxError' })
})

test('shadcn resolved glob keeps known ERP TypeScript inputs', () => {
  const shadcnRequire = createRequire(
    new URL('../node_modules/shadcn/package.json', import.meta.url),
  )
  const glob = shadcnRequire('fast-glob')
  const files = glob.sync('src/{lib,types}/**/*.ts', { cwd: frontend })
  assert.ok(files.includes('src/lib/api.ts'))
  assert.ok(files.includes('src/lib/auth.ts'))
  assert.ok(files.every((file) => /^src\/(lib|types)\//.test(file)))
})
