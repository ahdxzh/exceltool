package exceltool.builder.strategy.impl.cust;

import cn.hutool.core.util.NumberUtil;
import exceltool.builder.strategy.CellFormatStrategy;
import exceltool.builder.strategy.annotation.ExcelFormatStrategy;
import exceltool.param.DataWithFormat;
import exceltool.utils.ExcelCellStyleUtils;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * 红色字体百分比格式统一策略
 */
@SuppressWarnings("unused")
@ExcelFormatStrategy(order = 2, formatCode = "redFontRate")
public class RedFontRateFormatStrategy implements CellFormatStrategy {
    private static final String FORMAT_CODE = "redFontRate";

    @Override
    public void handle(XSSFWorkbook workbook, XSSFCell cell, DataWithFormat dataWithFormat, XSSFCellStyle originalCellStyle) {
        // 处理逻辑：红色字体+百分比
        XSSFCellStyle newCellStyle = workbook.createCellStyle();
        newCellStyle.cloneStyleFrom(originalCellStyle);

        // 复制原始字体并设置红色
        Font originalFont = workbook.getFontAt(originalCellStyle.getFontIndex());
        Font newFont = ExcelCellStyleUtils.copyFont(workbook, originalFont);
        newFont.setColor(IndexedColors.RED.getIndex());
        newCellStyle.setFont(newFont);

        // 设置百分比格式
        newCellStyle.setDataFormat(workbook.createDataFormat().getFormat("0%"));
        cell.setCellStyle(newCellStyle);

        double cellValue = NumberUtil.toDouble((Number) dataWithFormat.getContent());
        cell.setCellValue(cellValue);
    }

    @Override
    public String getFormatCode() {
        return FORMAT_CODE;
    }
}