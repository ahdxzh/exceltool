package exceltool.utils;

import org.apache.poi.ss.usermodel.*;

/**
 * Excel行操作工具类
 * 负责Excel行的复制、插入、字体样式复制等操作
 */
public class ExcelRowOperationUtils {

    // 工具类私有化构造方法，禁止实例化
    private ExcelRowOperationUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * 复制并插入指定数量的行到工作表中
     *
     * @param sheet          指定的工作表（不可为null）
     * @param sourceRowIndex 源行索引（基于0，需存在该行列）
     * @param num            要插入的复制行数量（需≥0）
     * @throws IllegalArgumentException 1.源行不存在 2.插入数量为负数 3.工作表为null
     */
    public static void copyAndInsertRows(Sheet sheet, int sourceRowIndex, int num) {
        // 补充空值校验和数量校验，增强健壮性
        if (sheet == null) {
            throw new IllegalArgumentException("Worksheet cannot be null.");
        }
        if (num < 0) {
            throw new IllegalArgumentException("The number of inserted rows cannot be negative.");
        }
        if (num == 0) {
            return; // 无需插入，直接返回
        }

        Row rowToCopy = sheet.getRow(sourceRowIndex);
        if (rowToCopy == null) {
            // 优化：异常信息携带具体索引，方便问题排查
            throw new IllegalArgumentException(String.format("The row to copy (index: %d) does not exist.", sourceRowIndex));
        }

        int lastRowNum = sheet.getLastRowNum(); // 获取当前最后一行索引

        for (int k = 0; k < num; k++) {
            int newRowIdx = sourceRowIndex + k + 1;

            // 只有当 newRowIdx <= lastRowNum 时才需要移动行，否则直接插入
            if (newRowIdx <= lastRowNum) {
                sheet.shiftRows(newRowIdx, lastRowNum, 1);
            }

            // 确保 newRow 不会为 null
            Row newRow = sheet.createRow(newRowIdx);
            copyRow(rowToCopy, newRow);
        }
    }

    /**
     * 复制行的内容和样式（私有方法，仅内部调用）
     *
     * @param sourceRow 源行
     * @param targetRow 目标行
     */
    private static void copyRow(Row sourceRow, Row targetRow) {
        if (sourceRow == null || targetRow == null) {
            return;
        }

        targetRow.setHeight(sourceRow.getHeight()); // 复制行高

        for (int i = 0; i < sourceRow.getLastCellNum(); i++) {
            Cell oldCell = sourceRow.getCell(i);
            Cell newCell = targetRow.createCell(i);

            if (oldCell == null) {
                continue;
            }

            // 复制单元格样式（深拷贝，创建新样式）
            Workbook workbook = targetRow.getSheet().getWorkbook();
            CellStyle newCellStyle = workbook.createCellStyle();
            newCellStyle.cloneStyleFrom(oldCell.getCellStyle());
            newCell.setCellStyle(newCellStyle);

            // 复制单元格值
            switch (oldCell.getCellType()) {
                case STRING:
                    newCell.setCellValue(oldCell.getStringCellValue());
                    break;
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(oldCell)) {
                        newCell.setCellValue(oldCell.getDateCellValue());
                    } else {
                        newCell.setCellValue(oldCell.getNumericCellValue());
                    }
                    break;
                case BOOLEAN:
                    newCell.setCellValue(oldCell.getBooleanCellValue());
                    break;
                case FORMULA:
                    newCell.setCellFormula(oldCell.getCellFormula());
                    break;
                case BLANK:
                    newCell.setBlank();
                    break;
                default:
                    break;
            }
        }
    }
}