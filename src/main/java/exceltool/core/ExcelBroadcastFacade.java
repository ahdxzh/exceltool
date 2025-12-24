package exceltool.core;

import cn.hutool.core.io.FileUtil;
import exceltool.builder.ExcelBuilder;
import exceltool.config.AsposeLicenseConfig;
import exceltool.param.ExcelResource;
import exceltool.utils.ExcelConvertUtils;
import exceltool.utils.FeishuUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.UUID;

@Slf4j
public class ExcelBroadcastFacade {
    // 静态代码块中初始化aspose许可证
    static {
        AsposeLicenseConfig asposeLicenseConfig = new AsposeLicenseConfig();
        asposeLicenseConfig.initAsposeLicense();
    }

    public static String generateImageAndSend(String webhookUrl, String templatePath, String outputDir, ExcelResource excelResource, String excelFileName) {
        try {
            // 处理原文件名，移除可能存在的.xlsx后缀
            String baseFileName = excelFileName.replaceAll("\\.xlsx$", "");
            // 生成UUID（去除横线）作为随机后缀
            String uuidSuffix = UUID.randomUUID().toString().replace("-", "");
            // 拼接完整路径（格式：输出目录 + 原文件名（去后缀）_UUID.xlsx）
            String xlsxFilePath = outputDir + baseFileName + "_" + uuidSuffix + ".xlsx";
            String pngFilePath = xlsxFilePath.replace(".xlsx", ".png");

            long t1 = System.currentTimeMillis();
            ExcelBuilder.buildByTemplate(templatePath, xlsxFilePath, excelResource);
            long t2 = System.currentTimeMillis();
            // 优先使用Aspose生成图片，失败后再使用spire
            try {
                ExcelConvertUtils.excelToImageAspose(xlsxFilePath, pngFilePath);
                log.info("aspose渲染图片成功");
            } catch (Exception exception) {
                log.error("aspose渲染图片失败", exception);
            }
            long t3 = System.currentTimeMillis();
            FeishuUtils.sendImageToChat(webhookUrl, new File(pngFilePath));
            long t4 = System.currentTimeMillis();

            log.info("文件名称:{}, 生成excel耗时:{}ms, 生成图片耗时:{}ms, 发送图片耗时:{}ms", excelFileName, t2 - t1, t3 - t2, t4 - t3);
            FileUtil.del(xlsxFilePath);
            FileUtil.del(pngFilePath);
            return "发送成功";
        } catch (Exception e) {
            log.error("发送图片失败", e);
            return "发送失败";
        }
    }
}
