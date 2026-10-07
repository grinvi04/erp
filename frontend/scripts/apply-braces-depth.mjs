import assert from 'node:assert/strict'
import { createHash } from 'node:crypto'
import { existsSync, readFileSync, realpathSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

// GHSA-vfj7-8cjw-p6xm has no published fix. Pinned to braces PR #78 commit 97308a0.
// ERP npm3.0.3 source hashes were checked before adoption. Five patched files match that commit.
// parse.js keeps npm quote/comma behavior and applies only the depth edits from the PR.
const manifest = {
  package: 'braces',
  version: '3.0.3',
  commit: '97308a01d091b211cf015314a2d0696da28a5392',
  files: [
    {
      path: 'lib/compile.js',
      originalSha256: 'dc98f22eee3d511785d92a00758d5f0d48efed5f5813bdecc2de430c529b5c9f',
      patchedSha256: '88cf20f18b59c9b2741c2d9b0f0ac6b2967d8e174667edf817133ca57d76809e',
      edits: [
        {
          before:
            "const utils = require('./utils');\n\nconst compile = (ast, options = {}) => {\n  const walk = (node, parent = {}) => {\n    const invalidBlock = utils.isInvalidBrace(parent);\n    const invalidNode = node.invalid === true && options.escapeInvalid === true;\n    const invalid = invalidBlock === true || invalidNode === true;\n",
          after:
            "const utils = require('./utils');\n\nconst compile = (ast, options = {}) => {\n  const walk = (node, parent = {}, depth = 0) => {\n    utils.assertDepth(depth);\n    const invalidBlock = utils.isInvalidBrace(parent);\n    const invalidNode = node.invalid === true && options.escapeInvalid === true;\n    const invalid = invalidBlock === true || invalidNode === true;\n",
        },
        {
          before:
            '\n    if (node.nodes) {\n      for (const child of node.nodes) {\n        output += walk(child, node);\n      }\n    }\n\n',
          after:
            '\n    if (node.nodes) {\n      for (const child of node.nodes) {\n        output += walk(child, node, depth + 1);\n      }\n    }\n\n',
        },
      ],
    },
    {
      path: 'lib/constants.js',
      originalSha256: 'c18ac5adb57308f1ce42a28552da3a31f5d83709743ebd9a636336813a744d4b',
      patchedSha256: '8d0bfdb50bb1e5267facef720ba09179634761165e3d6e13ad512ed10d6c180c',
      edits: [
        {
          before:
            "\nmodule.exports = {\n  MAX_LENGTH: 10000,\n\n  // Digits\n  CHAR_0: '0', /* 0 */\n",
          after:
            "\nmodule.exports = {\n  MAX_LENGTH: 10000,\n  MAX_AST_DEPTH: 100,\n\n  // Digits\n  CHAR_0: '0', /* 0 */\n",
        },
      ],
    },
    {
      path: 'lib/expand.js',
      originalSha256: '41ccc196ebfa7b7781a634e721eb744e4e7bcb54cba427a7e3d6806a1b9e58f7',
      patchedSha256: '46a259d3f23c0cd441fa370f16d5d48fa71060dfe918eeba84f6e95d3a94d368',
      edits: [
        {
          before:
            'const expand = (ast, options = {}) => {\n  const rangeLimit = options.rangeLimit === undefined ? 1000 : options.rangeLimit;\n\n  const walk = (node, parent = {}) => {\n    node.queue = [];\n\n    let p = parent;\n',
          after:
            'const expand = (ast, options = {}) => {\n  const rangeLimit = options.rangeLimit === undefined ? 1000 : options.rangeLimit;\n\n  const walk = (node, parent = {}, depth = 0) => {\n    utils.assertDepth(depth);\n    node.queue = [];\n\n    let p = parent;\n',
        },
        {
          before:
            '      }\n\n      if (child.nodes) {\n        walk(child, node);\n      }\n    }\n\n',
          after:
            '      }\n\n      if (child.nodes) {\n        walk(child, node, depth + 1);\n      }\n    }\n\n',
        },
      ],
    },
    {
      path: 'lib/parse.js',
      originalSha256: 'e572166565f15fa6ad9865ae49d678218e32aabfd1b3720f6d0d43d39800d310',
      patchedSha256: 'adf3c108a16afaabe5379298d2227881e88f536cc7345b7df780a80d38dd01f9',
      edits: [
        {
          before:
            "'use strict';\n\nconst stringify = require('./stringify');\n\n/**\n * Constants\n",
          after:
            "'use strict';\n\nconst stringify = require('./stringify');\nconst utils = require('./utils');\n\n/**\n * Constants\n",
        },
        {
          before:
            "     */\n\n    if (value === CHAR_LEFT_PARENTHESES) {\n      block = push({ type: 'paren', nodes: [] });\n      stack.push(block);\n      push({ type: 'text', value });\n",
          after:
            "     */\n\n    if (value === CHAR_LEFT_PARENTHESES) {\n      utils.assertDepth(stack.length);\n      block = push({ type: 'paren', nodes: [] });\n      stack.push(block);\n      push({ type: 'text', value });\n",
        },
        {
          before:
            "     */\n\n    if (value === CHAR_LEFT_CURLY_BRACE) {\n      depth++;\n\n      const dollar = prev.value && prev.value.slice(-1) === '$' || block.dollar === true;\n",
          after:
            "     */\n\n    if (value === CHAR_LEFT_CURLY_BRACE) {\n      utils.assertDepth(stack.length);\n      depth++;\n\n      const dollar = prev.value && prev.value.slice(-1) === '$' || block.dollar === true;\n",
        },
      ],
    },
    {
      path: 'lib/stringify.js',
      originalSha256: '379f22d77bfa1478341ccd49c5e4267464aabcbba03558bab332aac23fc6f23a',
      patchedSha256: 'a3b8b5e7567ff9bfdbe155479fbdd92d5a924c3919adf05316f8f0c4f6330332',
      edits: [
        {
          before:
            "const utils = require('./utils');\n\nmodule.exports = (ast, options = {}) => {\n  const stringify = (node, parent = {}) => {\n    const invalidBlock = options.escapeInvalid && utils.isInvalidBrace(parent);\n    const invalidNode = node.invalid === true && options.escapeInvalid === true;\n    let output = '';\n",
          after:
            "const utils = require('./utils');\n\nmodule.exports = (ast, options = {}) => {\n  const stringify = (node, parent = {}, depth = 0) => {\n    utils.assertDepth(depth);\n    const invalidBlock = options.escapeInvalid && utils.isInvalidBrace(parent);\n    const invalidNode = node.invalid === true && options.escapeInvalid === true;\n    let output = '';\n",
        },
        {
          before:
            '\n    if (node.nodes) {\n      for (const child of node.nodes) {\n        output += stringify(child);\n      }\n    }\n    return output;\n',
          after:
            '\n    if (node.nodes) {\n      for (const child of node.nodes) {\n        output += stringify(child, {}, depth + 1);\n      }\n    }\n    return output;\n',
        },
      ],
    },
    {
      path: 'lib/utils.js',
      originalSha256: 'b5a7596aa67730412b3c029ef09e84e6b67b8e445cffd35d1d295549c89066c7',
      patchedSha256: 'bbdfdad69da36c1994a19006af254659586a20d2c00b2a936926b7905cfb3aaf',
      edits: [
        {
          before:
            "'use strict';\n\nexports.isInteger = num => {\n  if (typeof num === 'number') {\n    return Number.isInteger(num);\n",
          after:
            "'use strict';\n\nconst { MAX_AST_DEPTH } = require('./constants');\n\n// Bound parser nesting and direct AST callers before recursive walkers run.\nexports.assertDepth = depth => {\n  if (depth > MAX_AST_DEPTH) {\n    throw new SyntaxError('AST nesting depth exceeds the maximum of ' + MAX_AST_DEPTH);\n  }\n};\n\nexports.isInteger = num => {\n  if (typeof num === 'number') {\n    return Number.isInteger(num);\n",
        },
      ],
    },
  ],
}

const sha256 = (content) => createHash('sha256').update(content).digest('hex')
const installedRoot = fileURLToPath(new URL('../node_modules/braces/', import.meta.url))

function prepare(packageRoot) {
  const installed = JSON.parse(readFileSync(path.join(packageRoot, 'package.json'), 'utf8'))
  assert.equal(installed.name, manifest.package, 'unexpected package name')
  assert.equal(
    installed.version,
    manifest.version,
    'braces version changed; remove or review patch',
  )

  const writes = []
  for (const entry of manifest.files) {
    assert.match(entry.path, /^lib\/[a-z-]+\.js$/)
    const target = path.join(packageRoot, entry.path)
    let content = readFileSync(target, 'utf8')
    const current = sha256(content)
    if (current === entry.patchedSha256) continue
    assert.equal(current, entry.originalSha256, `unexpected source bytes: ${entry.path}`)
    for (const edit of entry.edits) {
      assert.equal(content.split(edit.before).length, 2, `non-unique patch context: ${entry.path}`)
      content = content.replace(edit.before, () => edit.after)
    }
    assert.equal(sha256(content), entry.patchedSha256, `unexpected patched bytes: ${entry.path}`)
    writes.push([target, content])
  }
  return writes
}

export function verifyBracesPatch(packageRoot = installedRoot) {
  const writes = prepare(packageRoot)
  assert.equal(writes.length, 0, 'braces depth patch is not installed')
}

export function applyBracesPatch(packageRoot = installedRoot) {
  // Validate every file before the first write. If an I/O write fails, rerun safely.
  const writes = prepare(packageRoot)
  for (const [target, content] of writes) writeFileSync(target, content)
  verifyBracesPatch(packageRoot)
  return writes.length
}

if (
  process.argv[1] &&
  existsSync(process.argv[1]) &&
  realpathSync(process.argv[1]) === realpathSync(fileURLToPath(import.meta.url))
) {
  if (process.argv[2] === '--check') {
    verifyBracesPatch()
    console.log(`braces depth patch verified (${manifest.version}, ${manifest.commit})`)
  } else {
    assert.equal(process.argv.length, 2, 'unsupported patch argument')
    const devOmitted = (process.env.npm_config_omit ?? '').split(/[\s,]+/).includes('dev')
    if (devOmitted && !existsSync(installedRoot)) {
      console.log('braces depth patch skipped: dev dependencies omitted and braces is absent')
    } else {
      const modified = applyBracesPatch()
      console.log(
        `braces depth patch applied to ${modified} files (${manifest.version}, ${manifest.commit})`,
      )
    }
  }
}
