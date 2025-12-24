package exceltool.builder.strategy;

import exceltool.builder.strategy.annotation.ExcelFormatStrategy;
import exceltool.param.DataWithFormat;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.Map;

/**
 * 统一Excel格式策略接口：整合「格式判断」和「样式处理」逻辑
 * 一个实现类对应一种Excel格式
 */
public interface CellFormatStrategy {
    /**
     * 判断当前单元格是否需要应用该格式
     *
     * @param dynamicKey     单元格动态key
     * @param dataWithFormat 单元格数据与格式信息
     * @param currentRowData 当前行完整数据
     * @return true-需要应用该格式，false-不需要
     */
    default boolean judge(String dynamicKey, DataWithFormat dataWithFormat, Map<String, DataWithFormat> currentRowData) {
        return dataWithFormat.getFormatCode().equals(getFormatCode());
    }

    /**
     * 处理该格式对应的单元格样式
     *
     * @param workbook          Excel工作簿
     * @param cell              待处理单元格
     * @param dataWithFormat    单元格数据与格式信息
     * @param originalCellStyle 单元格原始样式
     */
    void handle(XSSFWorkbook workbook, XSSFCell cell, DataWithFormat dataWithFormat, XSSFCellStyle originalCellStyle);

    /**
     * 获取策略名称/格式编码（默认从注解读取，无注解则返回类名）
     */
    default String getFormatCode() {
        ExcelFormatStrategy annotation = this.getClass().getAnnotation(ExcelFormatStrategy.class);
        if (annotation != null && !annotation.formatCode().isEmpty()) {
            return annotation.formatCode();
        }
        return this.getClass().getSimpleName();
    }
}