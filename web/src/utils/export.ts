/**
 * Excel 导出工具
 * ============================================================
 *
 * 功能：
 * - 按需加载 write-excel-file 库，避免首屏加载变慢
 * - 支持自定义文件名和工作表名称
 * - 自动设置列宽以适应内容
 * - 转义可疑公式前缀，避免导出的用户数据触发 Excel 公式注入
 */

import type { Cell, SheetData } from 'write-excel-file/browser';

export type ExportXlsxOptions = {
  filename: string;
  sheetName?: string;
  headers: string[];
  rows: Array<Array<string | number | boolean | null>>;
  columnWidths?: number[];
};

const FORMULA_PREFIX_PATTERN = /^[=+\-@\t\r]/;

function sanitizeSheetName(sheetName: string): string {
  const normalized = sheetName.replace(/[\[\]:*?/\\]/g, ' ').trim();
  return (normalized || 'Sheet1').slice(0, 31);
}

function toSafeCellValue(value: string | number | boolean | null): Cell {
  if (value === null) {
    return '';
  }

  if (typeof value === 'string' && FORMULA_PREFIX_PATTERN.test(value)) {
    return `'${value}`;
  }

  return value;
}

/**
 * 导出数据为 Excel 文件
 * @param options 导出选项
 */
export async function exportToXlsx(options: ExportXlsxOptions): Promise<void> {
  const { filename, sheetName = 'Sheet1', headers, rows, columnWidths } = options;

  // 动态导入浏览器专用入口，避免首屏加载导出依赖。
  const { default: writeXlsxFile } = await import('write-excel-file/browser');
  const sheetData: SheetData = [
    headers.map(toSafeCellValue),
    ...rows.map(row => row.map(toSafeCellValue)),
  ];

  // 设置列宽：优先使用调用方传入值，否则按内容自动估算。
  const widths =
    columnWidths && columnWidths.length > 0
      ? columnWidths
      : headers.map((header, colIndex) => {
          const headerWidth = String(header).length;
          const maxDataWidth = Math.max(...rows.map(row => String(row[colIndex] ?? '').length));
          return Math.max(headerWidth, maxDataWidth, 10);
        });

  // 写入文件并触发下载
  await writeXlsxFile(sheetData, {
    sheet: sanitizeSheetName(sheetName),
    columns: widths.map(width => ({ width })),
  }).toFile(filename);
}
