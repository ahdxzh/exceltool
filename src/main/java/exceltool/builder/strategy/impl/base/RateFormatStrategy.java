package exceltool.builder.strategy.impl.base;

import cn.hutool.core.util.NumberUtil;
import exceltool.builder.strategy.annotation.ExcelFormatStrategy;
import exceltool.builder.strategy.CellFormatStrategy;
import exceltool.param.DataWithFormat;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.Map;

/**
 * 百分比格式统一策略（判断+处理）
 */
@ExcelFormatStrategy(order = 3)
@SuppressWarnings("unused")
public class RateFormatStrategy implements CellFormatStrategy {
    private static final String FORMAT_CODE = "rate";

    @Override
    public boolean judge(String dynamicKey, DataWithFormat dataWithFormat, Map<String, DataWithFormat> currentRowData) {
        // 判断规则：key以rate结尾
        if (dynamicKey != null && dynamicKey.endsWith("rate")) {
            // 可在此处直接设置格式编码（可选）
            dataWithFormat.setFormatCode(FORMAT_CODE);
            return true;
        }
        return false;
    }

    @Override
    public void handle(XSSFWorkbook workbook, XSSFCell cell, DataWithFormat dataWithFormat, XSSFCellStyle originalCellStyle) {
        // 处理逻辑：设置百分比样式
        XSSFCellStyle newCellStyle = workbook.createCellStyle();
        newCellStyle.cloneStyleFrom(originalCellStyle);
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