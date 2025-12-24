package exceltool.builder;

import exceltool.builder.strategy.CellFormatStrategy;
import exceltool.builder.strategy.RowHandleStrategy;
import exceltool.param.DataWithFormat;
import exceltool.param.ExcelHeaderFormat;
import exceltool.param.ExcelResource;
import exceltool.utils.ExcelCellStyleUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

class ExcelFormatter {
    // 处理全局格式（无需修改）
    public static void formatGlobalStyle(XSSFWorkbook workbook, XSSFSheet sheet, ExcelResource excelResource) {
        ExcelHeaderFormat headerFormat = excelResource.getExcelHeaderFormat();
        ExcelCellStyleUtils.replaceRowColor(workbook, sheet, headerFormat.getStart(), headerFormat.getEnd(), headerFormat.getHexColor());

        for (Row row : sheet) {
            for (Cell cell : row) {
                if (cell.getCellType() == CellType.STRING && IdentifierParser.containsDynamicKey(cell.getStringCellValue())) {
                    cell.setBlank();
                }
            }
        }
    }

    // 处理静态资源（无需修改，如需优化可后续适配策略模式）
    public static void formatStaticRow(XSSFWorkbook workbook, XSSFRow row, ExcelResource excelResource) {
        Map<String, DataWithFormat> staticResource = excelResource.getStaticResource();
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            XSSFCell cell = row.getCell(i);
            String cellValue = cell.getStringCellValue();
            if (cellValue != null) {
                String staticKey = IdentifierParser.getStaticKey(cellValue);
                handleCellStyle(workbook, cell, staticResource.get(staticKey));
            }
        }
    }

    // 处理动态row（大幅简化）
    public static void formatDynamicRow(XSSFWorkbook workbook, XSSFRow row, List<Map<String, DataWithFormat>> dataList, int index) {
        Map<String, DataWithFormat> currentRowData = dataList.get(index);
        List<CellFormatStrategy> strategies = ExcelAutoScanStrategyRegistry.getFormatStrategies();

        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            XSSFCell cell = row.getCell(i);
            String cellValue = cell.getStringCellValue();
            String dynamicKey = IdentifierParser.getDynamicKey(cellValue);
            DataWithFormat dataWithFormat = currentRowData.get(dynamicKey);

            if (Objects.isNull(dynamicKey) || Objects.isNull(dataWithFormat)) {
                continue;
            }

            // 根据行级别中指定的格式来刷新单元格的格式码
            RowHandleStrategy.handleRowFormatter(currentRowData);

            // 遍历所有自动加载的策略，执行判断+处理
            for (CellFormatStrategy strategy : strategies) {
                if (strategy.judge(dynamicKey, dataWithFormat, currentRowData)) {
                    XSSFCellStyle originalCellStyle = cell.getCellStyle();
                    strategy.handle(workbook, cell, dataWithFormat, originalCellStyle);
                    break; // 匹配一个策略后停止
                }
            }
        }
    }

    // 处理单元格样式（简化，静态资源可复用此逻辑）
    private static void handleCellStyle(XSSFWorkbook workbook, XSSFCell cell, DataWithFormat dataWithFormat) {
        if (Objects.isNull(dataWithFormat)) {
            return;
        }
        List<CellFormatStrategy> strategies = ExcelAutoScanStrategyRegistry.getFormatStrategies();
        // 静态资源可根据dataWithFormat的formatCode匹配策略（如需）
        for (CellFormatStrategy strategy : strategies) {
            if (strategy.getFormatCode().equals(dataWithFormat.getFormatCode())) {
                XSSFCellStyle originalCellStyle = cell.getCellStyle();
                strategy.handle(workbook, cell, dataWithFormat, originalCellStyle);
                break;
            }
        }
    }
}