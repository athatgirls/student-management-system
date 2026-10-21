const { test } = require('node:test')
const assert = require('node:assert/strict')
const { loadBindings } = require('./helpers/sourceHarness')

const { privateUploadUrl } = loadBindings('utils/privateUploadUrl.js', ['privateUploadUrl'], { URL })
const origin = 'http://localhost:3100'
const base = '/SCSE@hbut/msi'

test('private attachment uses the same-origin protected upload path', () => {
  const path = '/SCSE@hbut/uploads/d113c030-4399-4998-b0ff-fa4257d54cd8.jpg'
  assert.equal(privateUploadUrl(path, origin, base), path)
  assert.equal(privateUploadUrl(origin + path, origin, base), path)
  assert.equal(privateUploadUrl('/SCSE@hbut/uploads/file.pdf', origin, base), '/SCSE@hbut/uploads/file.pdf')
})

test('private attachment cannot send an authorization token to another origin or endpoint', () => {
  assert.equal(privateUploadUrl('https://example.org/SCSE@hbut/uploads/file.jpg', origin, base), '')
  assert.equal(privateUploadUrl('//example.org/SCSE@hbut/uploads/file.jpg', origin, base), '')
  assert.equal(privateUploadUrl('/SCSE@hbut/msi/admin/all', origin, base), '')
  assert.equal(privateUploadUrl('/SCSE@hbut/uploads/../msi/admin/all', origin, base), '')
  assert.equal(privateUploadUrl('/SCSE@hbut/uploads/file.jpg?redirect=elsewhere', origin, base), '')
})

test('private attachment fetch carries the bearer token without adding it to the URL', async () => {
  let requested
  const { fetchFile } = loadBindings('components/PrivateAttachments.vue', ['fetchFile'], {
    privateUploadUrl,
    getFileUrl: url => url,
    window: { location: { origin } },
    request: { defaults: { baseURL: base } },
    localStorage: { getItem: () => 'test-token' },
    AbortController,
    controllers: [],
    fetch: (url, options) => {
      requested = { url, options }
      return Promise.resolve({ ok: true, blob: () => Promise.resolve('image-data') })
    }
  })
  assert.equal(await fetchFile('/SCSE@hbut/uploads/file.jpg'), 'image-data')
  assert.equal(requested.url, '/SCSE@hbut/uploads/file.jpg')
  assert.equal(requested.options.headers.Authorization, 'Bearer test-token')
  assert.equal(requested.options.credentials, 'same-origin')
})

test('Word attachments download through a temporary blob URL', async () => {
  let clicked = false
  let released = false
  let downloadName
  const link = { click: () => { clicked = true; downloadName = link.download }, remove: () => {} }
  const { download } = loadBindings('components/PrivateAttachments.vue', ['download'], {
    fetchFile: async () => 'protected-pdf', active: true,
    URL: { createObjectURL: () => 'blob:protected-file', revokeObjectURL: () => { released = true } },
    document: { createElement: () => link, body: { appendChild: () => {} } },
    setTimeout: callback => callback(),
    ElMessage: { error: message => { throw new Error(message) } }
  })
  await download({ url: '/SCSE@hbut/uploads/file.docx', name: '成果.docx' })
  assert.equal(link.href, 'blob:protected-file')
  assert.equal(downloadName, '成果.docx')
  assert.equal(clicked, true)
  assert.equal(released, true)
})

test('Word clicks download without opening a preview tab', async () => {
  let downloaded = false
  const { openFile, isPreviewable } = loadBindings('components/PrivateAttachments.vue', ['openFile', 'isPreviewable'], {
    download: async () => { downloaded = true },
    window: { open: () => { throw new Error('Word must not open a preview tab') } }
  })
  assert.equal(isPreviewable('report.doc'), false)
  assert.equal(isPreviewable('report.docx'), false)
  await openFile({ url: 'report.docx' })
  assert.equal(downloaded, true)
})

test('images and PDF open a new tab with a previewable MIME type', async () => {
  for (const [name, type] of [['image.jpg', 'image/jpeg'], ['image.png', 'image/png'], ['proof.pdf', 'application/pdf']]) {
    const order = []
    let navigated, createdBlob, released = false, onClose
    const tab = { opener: {}, closed: false, location: { replace: url => { navigated = url; order.push('navigate') } }, close: () => {} }
    const window = {
      open: () => { order.push('open'); return tab },
      setInterval: fn => { onClose = fn; return 1 }, clearInterval: () => {}
    }
    const { openFile, isPreviewable } = loadBindings('components/PrivateAttachments.vue', ['openFile', 'isPreviewable'], {
      window, active: true,
      fetchFile: async () => { order.push('fetch'); return new Blob(['fixture'], { type: 'application/octet-stream' }) },
      download: () => { throw new Error('preview must not download') },
      URL: { createObjectURL: blob => { createdBlob = blob; return 'blob:preview' }, revokeObjectURL: () => { released = true } },
      Blob, ElMessage: { error: message => { throw new Error(message) } }
    })
    assert.equal(isPreviewable(name), true)
    await openFile({ url: name })
    assert.equal(createdBlob.type, type)
    assert.equal(navigated, 'blob:preview')
    assert.equal(tab.opener, null)
    assert.equal(order.join(','), 'open,fetch,navigate')
    tab.closed = true
    onClose()
    assert.equal(released, true)
  }
})
