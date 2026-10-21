const { readFileSync } = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const { parseSync } = require('@babel/core')

const sourceRoot = path.join(__dirname, '../../src')
const readSource = file => readFileSync(path.join(sourceRoot, file), 'utf8')

// Run production handlers without mounting Vue or making real API requests.
function loadBindings(file, names, doubles = {}) {
  const fileSource = readSource(file)
  const source = file.endsWith('.vue') ? fileSource.split('<script setup>')[1].split('</script>')[0] : fileSource
  const ast = parseSync(source, { configFile: false, babelrc: false })
  const declarations = ast.program.body.map(node => node.declaration || node).filter(node =>
    (node.type === 'VariableDeclaration' && node.declarations.some(item => names.includes(item.id.name))) ||
    (node.type === 'FunctionDeclaration' && names.includes(node.id.name)))
  const context = vm.createContext(doubles)
  vm.runInContext(declarations.map(node => source.slice(node.start, node.end)).join('\n') +
    '\nglobalThis.bindings = {' + names.join(',') + '}', context)
  return context.bindings
}

module.exports = { sourceRoot, readSource, loadBindings }
