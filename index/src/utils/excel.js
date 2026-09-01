const MAX_IMPORT_ROWS = 10000

/**
 * 读取首个工作表，并以第一行为表头转换成普通对象数组。
 * 当前解析器只处理 OOXML（.xlsx）；限制行数可避免浏览器被超大文件拖垮。
 */
export const readExcelObjects = async (file) => {
  if (!file) {
    throw new Error('请选择 Excel 文件')
  }

  const { default: readXlsxFile } = await import('read-excel-file/browser')
  const data = await readXlsxFile(file)
  if (data.length < 1) {
    return []
  }

  if (data.length > MAX_IMPORT_ROWS + 1) {
    throw new Error(`单次最多导入 ${MAX_IMPORT_ROWS} 行数据`)
  }

  const headers = data[0].map(value => String(value || '').trim())
  return data.slice(1).reduce((rows, row) => {
    const item = {}
    headers.forEach((header, columnIndex) => {
      if (header) item[header] = String(row[columnIndex] ?? '').trim()
    })
    if (Object.values(item).some(value => value !== '')) rows.push(item)
    return rows
  }, [])
}

/** 生成并下载一个简单的 .xlsx 工作簿。 */
export const downloadExcel = async (rows, sheetName, fileName) => {
  const { default: writeXlsxFile } = await import('write-excel-file/browser')
  const data = rows.map((row, rowIndex) => row.map(value => ({
    value: value ?? '',
    type: typeof value === 'number' ? Number : String,
    fontWeight: rowIndex === 0 ? 'bold' : undefined
  })))
  await writeXlsxFile(data, { fileName, sheet: sheetName })
}
