/**
 * Excel 导出工具
 * ============================================================
 *
 * 功能：
 * - 按需加载 xlsx 库，避免首屏加载变慢
 * - 支持自定义文件名和工作表名称
 * - 自动设置列宽以适应内容
 */

export type ExportXlsxOptions = {
  filename: string;
  sheetName?: string;
  headers: string[];
  rows: Array<Array<string | number | boolean | null>>;
  columnWidths?: number[];
};

/**
 * 导出数据为 Excel 文件
 * @param options 导出选项
 */
export async function exportToXlsx(options: ExportXlsxOptions): Promise<void> {
  const { filename, sheetName = 'Sheet1', headers, rows, columnWidths } = options;

  // 动态导入 xlsx 库，避免首屏加载
  const XLSX = await import('xlsx');

  // 将表头和数据行组合成二维数组
  const aoa = [headers, ...rows];

  // 创建工作表
  const worksheet = XLSX.utils.aoa_to_sheet(aoa);

  // 设置列宽（如果提供）
  if (columnWidths && columnWidths.length > 0) {
    worksheet['!cols'] = columnWidths.map(width => ({ wch: width }));
  } else {
    // 自动计算列宽
    const colWidths = headers.map((header, colIndex) => {
      const headerWidth = String(header).length;
      const maxDataWidth = Math.max(...rows.map(row => String(row[colIndex] ?? '').length));
      return Math.max(headerWidth, maxDataWidth, 10); // 最小宽度 10
    });
    worksheet['!cols'] = colWidths.map(width => ({ wch: width }));
  }

  // 创建工作簿
  const workbook = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(workbook, worksheet, sheetName);

  // 写入文件并触发下载
  XLSX.writeFile(workbook, filename, { compression: true });
}
