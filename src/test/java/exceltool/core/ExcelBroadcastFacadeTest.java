package exceltool.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import exceltool.param.DataWithFormat;
import exceltool.param.ExcelHeaderFormat;
import exceltool.param.ExcelResource;
import junit.framework.TestCase;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static exceltool.core.ExcelBroadcastFacade.generateImageAndSend;


public class ExcelBroadcastFacadeTest extends TestCase {
    public void testGenerateImageAndSend() {
        // 标题
        String title = "测试标题";
        // 控制表头高亮
        ExcelHeaderFormat excelHeaderFormat = new ExcelHeaderFormat("#FFBA6B", 0, 1);
        // 配置静态资源
        ExcelResource excelResource = new ExcelResource(excelHeaderFormat, Collections.singletonMap("title", new DataWithFormat(title)));
        // 配置动态资源
        String jsonPath = "src/test/resources/json/report.json";
        List<Map<String, Object>> reportData = parseJsonFile(jsonPath);
        excelResource.putDynamicDataList("p", reportData);

        String webhookUrl = "https://open.feishu.cn/open-apis/bot/v2/hook/5eb7d973-cd22-451d-8a88-77082fb8c637";
        String templatePath = "/template/loan_report_country_template.xlsx";
        String outputDir = "src/main/resources/output/";
        String excelFileName = "测试文件.xlsx";

        generateImageAndSend(webhookUrl, templatePath, outputDir, excelResource, excelFileName);
    }

    // 核心业务代码（可放入方法中执行）
    private List<Map<String, Object>> parseJsonFile(String jsonPath) {
        // 声明变量存储解析结果
        List<Map<String, Object>> jsonDataList = null;
        try {
            // 读取JSON文件内容为UTF-8编码的字符串
            String jsonContent = new String(
                    Files.readAllBytes(Paths.get(jsonPath)),
                    StandardCharsets.UTF_8
            );
            // 创建ObjectMapper实例（Jackson核心类，用于JSON序列化/反序列化）
            ObjectMapper objectMapper = new ObjectMapper();
            // 接收解析后的返回值，存储到提前声明的变量中
            jsonDataList = objectMapper.readValue(
                    jsonContent,
                    new TypeReference<List<Map<String, Object>>>() {}
            );
        } catch (Exception e) {
            // 非空异常处理，打印异常信息便于排查问题
            System.err.println("读取或解析JSON文件失败！文件路径：" + jsonPath);
        }
        // 返回解析结果（成功返回数据列表，失败返回null）
        return jsonDataList;
    }
}