package exceltool.utils;

import com.aspose.cells.*;

public class ExcelConvertUtils {
    public static void excelToImageAspose(String excelPath, String imgFullPathName) throws Exception {
        // 加载Excel文件
        Workbook wb = new Workbook(excelPath);

        // 设置图片选项
        ImageOrPrintOptions imgOpt = new ImageOrPrintOptions();

        // 设置输出图片类型为PNG
        imgOpt.setImageType(ImageType.PNG);

        // 设置输出图片的质量为100，确保高清
        imgOpt.setQuality(100);
        // 设置只输出有内容的区域
        imgOpt.setOnlyArea(true);
        // 设置只打印为一张图片
        imgOpt.setOnePagePerSheet(true);
        // 设置分辨率
        imgOpt.setVerticalResolution(300);
        imgOpt.setHorizontalResolution(300);

        // 获取第一个工作表
        Worksheet sheet = wb.getWorksheets().get(0);

        // 渲染工作表为图片
        SheetRender sr = new SheetRender(sheet, imgOpt);

        // 输出图片（第一个页面）
        sr.toImage(0, imgFullPathName);
    }
}
