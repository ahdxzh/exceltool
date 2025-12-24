package exceltool.builder.strategy.impl.cust;

import exceltool.builder.strategy.CellFormatStrategy;
import exceltool.builder.strategy.annotation.ExcelFormatStrategy;
import exceltool.param.DataWithFormat;
import exceltool.utils.ExcelCellStyleUtils;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * 黄色高亮格式统一策略
 */
@SuppressWarnings("unused")
@ExcelFormatStrategy(order = 1)
public class TotalSummaryFormatStrategy implements CellFormatStrategy {
    private static final String FORMAT_CODE = "totalSummary";
    private static final String ROW_BG_COLOR = "#FEF6DE";
    private static final String CELL_BG_COLOR = "#FBE395";

    @Override
    public void handle(XSSFWorkbook workbook, XSSFCell cell, DataWithFormat dataWithFormat, XSSFCellStyle originalCellStyle) {
        ExcelCellStyleUtils.setRowColor(workbook, cell, originalCellStyle, ROW_BG_COLOR, CELL_BG_COLOR);
        cell.setCellValue(dataWithFormat.getContent().toString());
    }

    @Override
    public String getFormatCode() {
        return FORMAT_CODE;
    }
}