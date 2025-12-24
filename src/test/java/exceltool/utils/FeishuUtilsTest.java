package exceltool.utils;

import junit.framework.TestCase;

import java.io.File;

public class FeishuUtilsTest extends TestCase {
    public void testSendImageToChat() {
        String webhookUrl = "https://open.feishu.cn/open-apis/bot/v2/hook/5eb7d973-cd22-451d-8a88-77082fb8c637";
        String imagePath = "src/exceltool.properties/resources/png/MX-IOS-009包市场总结_2025-12-02_6c166b0e49484958b31b97029512e333.png";
        File imageFile = new File(imagePath);

        FeishuUtils.sendImageToChat(webhookUrl, imageFile);
    }

}