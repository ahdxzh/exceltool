package exceltool.utils;

import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.util.StringUtil;
import org.apache.poi.xssf.usermodel.*;

import java.awt.Color;
import java.util.Iterator;

public class ExcelCellStyleUtils {
    // 复制字体格式
    public static Font copyFont(Workbook workbook, Font originalFont) {
        Font copiedFont = workbook.createFont();
        copiedFont.setFontName(originalFont.getFontName());
        copiedFont.setFontHeight(originalFont.getFontHeight());
        copiedFont.setBold(originalFont.getBold());
        copiedFont.setItalic(originalFont.getItalic());
        copiedFont.setUnderline(originalFont.getUnderline());
        return copiedFont;
    }

    public static void replaceRowColor(XSSFWorkbook workbook, int startRowIndex, int endRowIndex, String nm) {
        Iterator<Sheet> sheetIterator = workbook.sheetIterator();
        // 处理所有的sheet，注意被隐藏的sheet也会处理
        while (sheetIterator.hasNext()) {
            XSSFSheet sheet = (XSSFSheet) sheetIterator.next();
            replaceRowColor(workbook, sheet, startRowIndex, endRowIndex, nm);
        }
    }

    public static void replaceRowColor(XSSFWorkbook workbook, XSSFSheet sheet, int startRowIndex, int endRowIndex, String nm) {
        Color awtColor = Color.decode(nm);
        XSSFColor xssfColor = new XSSFColor(awtColor, new DefaultIndexedColorMap());

        for (int rowIndex = startRowIndex; rowIndex <= endRowIndex; rowIndex++) {
            XSSFRow row = sheet.getRow(rowIndex);
            for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
                XSSFCell cell = row.getCell(i);
                CellStyle originalStyle = cell.getCellStyle();

                XSSFCellStyle newStyle = workbook.createCellStyle(); // 创建新单元格格式用于接收处理原始格式
                newStyle.cloneStyleFrom(originalStyle); // 复制原始格式
                newStyle.setFillForegroundColor(xssfColor);
                newStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                cell.setCellStyle(newStyle); // 将样式应用到单元格
            }
        }
    }

    public static void setRowColor(XSSFWorkbook workbook, XSSFCell cell, XSSFCellStyle originalCellStyle, String rowBgColor, String cellBgColor) {
        int rowNum = cell.getRow().getRowNum();
        ExcelCellStyleUtils.replaceRowColor(workbook, rowNum, rowNum, rowBgColor);

        if (StringUtil.isNotBlank(cellBgColor)) {
            XSSFCellStyle newCellStyle = workbook.createCellStyle();
            newCellStyle.cloneStyleFrom(originalCellStyle);
            newCellStyle.setFillForegroundColor(new XSSFColor(Color.decode(cellBgColor), new DefaultIndexedColorMap()));
            newCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cell.setCellStyle(newCellStyle);
        }
    }
}
