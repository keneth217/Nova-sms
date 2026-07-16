export function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = filename
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  URL.revokeObjectURL(url)
}

export function stampFilename(prefix: string, ext: string) {
  const stamp = new Date().toISOString().slice(0, 10)
  return `${prefix}-${stamp}.${ext}`
}
