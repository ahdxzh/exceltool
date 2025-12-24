// String类型策略
package exceltool.builder.strategy.impl.base;

import exceltool.builder.strategy.CellFormatStrategy;
import exceltool.param.DataWithFormat;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.Map;

@SuppressWarnings("unused")
public class StringFormatStrategy implements CellFormatStrategy {
    private static final String FORMAT_CODE = "java.lang.String";

    @Override
    public boolean judge(String dynamicKey, DataWithFormat dataWithFormat, Map<String, DataWithFormat> currentRowData) {
        if (dataWithFormat != null && (FORMAT_CODE.equals(dataWithFormat.getFormatCode())
                || dataWithFormat.getContent() instanceof String)) {
            dataWithFormat.setFormatCode(FORMAT_CODE);
            return true;
        }
        return false;
    }

    @Override
    public void handle(XSSFWorkbook workbook, XSSFCell cell, DataWithFormat dataWithFormat, XSSFCellStyle originalCellStyle) {
        Object content = dataWithFormat.getContent();
        assert content instanceof String;
        cell.setCellValue((String) content);
    }

    @Override
    public String getFormatCode() {
        return FORMAT_CODE;
    }
}