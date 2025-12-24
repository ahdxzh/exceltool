package exceltool.builder.strategy.impl.base;

import cn.hutool.core.util.NumberUtil;
import exceltool.builder.strategy.CellFormatStrategy;
import exceltool.param.DataWithFormat;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 统一数字类型策略（整合Integer/Double/Long/BigDecimal，减少类冗余）
 */
@SuppressWarnings("unused")
public class NumberFormatStrategy implements CellFormatStrategy {
    private static final String FORMAT_CODE = "java.lang.Number";

    @Override
    public boolean judge(String dynamicKey, DataWithFormat dataWithFormat, Map<String, DataWithFormat> currentRowData) {
        // 自动判断content是否为数字类型，无需手动设置formatCode
        if (dataWithFormat != null && dataWithFormat.getContent() instanceof Number) {
            dataWithFormat.setFormatCode(FORMAT_CODE);
            return true;
        }
        return false;
    }

    @Override
    public void handle(XSSFWorkbook workbook, XSSFCell cell, DataWithFormat dataWithFormat, XSSFCellStyle originalCellStyle) {
        Object content = dataWithFormat.getContent();
        double cellValue = 0.0;

        // 统一处理所有数字类型
        if (content instanceof Integer) {
            cellValue = ((Integer) content).doubleValue();
        } else if (content instanceof Double) {
            cellValue = (double) content;
        } else if (content instanceof Long) {
            cellValue = ((Long) content).doubleValue();
        } else if (content instanceof BigDecimal) {
            cellValue = ((BigDecimal) content).doubleValue();
        } else if (content instanceof Number) {
            cellValue = NumberUtil.toDouble((Number) content);
        }

        cell.setCellValue(cellValue);
    }

    @Override
    public String getFormatCode() {
        return FORMAT_CODE;
    }
}