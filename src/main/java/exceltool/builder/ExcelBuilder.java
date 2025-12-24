package exceltool.builder;

import exceltool.param.DataWithFormat;
import exceltool.param.ExcelResource;
import exceltool.utils.ExcelRowOperationUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class ExcelBuilder {
    public static void buildByTemplate(String templatePath, String outPutPath, ExcelResource excelResource) {
        try {
            // 1. 初始化系统名称
            String osName = System.getProperty("os.name");
            // 2. 只做一次路径处理（抽离重复逻辑，消除多个匹配项提示）
            String processedTemplatePath = templatePath.startsWith("/") ? templatePath.substring(1) : templatePath;
            // 3. 声明输入流
            InputStream inputStream;

            // 适配本地测试环境
            if (osName.toLowerCase().contains("win")) {
                // 本地测试环境直接读取，使用预处理后的路径
                String localTemplatePath = String.format("src/main/resources/%s", processedTemplatePath);
                Path localPath = Paths.get(localTemplatePath);
                // 检查本地文件是否存在
                if (!Files.exists(localPath)) {
                    throw new FileNotFoundException("本地模板文件不存在，请检查路径：" + localTemplatePath);
                }
                // Paths.get()传入预处理后的本地路径，无重复提示
                inputStream = Files.newInputStream(localPath);
            } else {
                // 非Spring项目：JDK原生ClassLoader读取类路径资源，直接使用预处理后的路径
                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                inputStream = classLoader.getResourceAsStream(processedTemplatePath);

                // 检查资源是否存在
                if (inputStream == null) {
                    throw new FileNotFoundException("类路径下模板文件不存在，请检查打包是否包含：" + processedTemplatePath);
                }
            }

            // 替换静态数据，根据动态数据生成多行
            XSSFWorkbook workbook = buildByTemplate(inputStream, excelResource);
            save(workbook, Files.newOutputStream(Paths.get(outPutPath)));
        } catch (Exception exception) {
            log.error("An exception occurred", exception);
        }
    }

    public static XSSFWorkbook buildByTemplate(InputStream inputStream, ExcelResource excelResource) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            XSSFSheet sheet = workbook.getSheetAt(i);
            handleSheet(workbook, sheet, excelResource);
        }
        return workbook;
    }

    // 替换excel中的值，动态扩充list
    public static void handleSheet(XSSFWorkbook workbook, XSSFSheet sheet, ExcelResource excelResource) {
        for (int i = sheet.getFirstRowNum(); i <= sheet.getLastRowNum(); i++) {
            XSSFRow row = sheet.getRow(i);
            // 根据每一行第一个列的值来判断是否是动态数据。例如p.date则会被解析为p，去动态数据源map中寻找对应的dataList
            if (Objects.isNull(row.getCell(0))) {
                break;
            }
            String identifier = IdentifierParser.getIdentifier(row.getCell(0).getStringCellValue());
            Map<String, List<Map<String, DataWithFormat>>> dynamicResource = excelResource.getDynamicResource();

            if (dynamicResource.containsKey(identifier) && !dynamicResource.get(identifier).isEmpty()) {
                // 如果是动态数据，则扩充该行，替换内容，并从新的指针向下继续替换
                i = handleDynamicRows(workbook, sheet, i, dynamicResource.get(identifier));
            } else {
                // 如果为空说明是静态数据，则调用静态数据替换方法
                ExcelFormatter.formatStaticRow(workbook, row, excelResource);
            }
        }
        ExcelFormatter.formatGlobalStyle(workbook, sheet, excelResource);
    }

    // 假设动态数据会替换当前行之后的N行，N由dataList的大小决定
    private static int handleDynamicRows(XSSFWorkbook workbook, XSSFSheet sheet, int currentRowIndex, List<Map<String, DataWithFormat>> dataList) {
        // 根据dataList大小扩容模板excel
        int rowsToHandle = dataList.size();
        int copyRows = rowsToHandle - 1;
        int endIndex = currentRowIndex + copyRows;
        ExcelRowOperationUtils.copyAndInsertRows(sheet, currentRowIndex, copyRows);

        // 逐行设置单元格内容和格式
        int index = 0;
        for (int i = currentRowIndex; i <= endIndex; i++) {
            XSSFRow row = sheet.getRow(i);
            ExcelFormatter.formatDynamicRow(workbook, row, dataList, index++);
        }
        return endIndex + 1;
    }

    public static void save(Workbook workbook, OutputStream outputStream) throws IOException {
        workbook.write(outputStream);
        outputStream.flush();
        outputStream.close();
        workbook.close();
    }
}
