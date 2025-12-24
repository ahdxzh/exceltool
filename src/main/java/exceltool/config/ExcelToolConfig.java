package exceltool.config;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.setting.dialect.Props;
import exceltool.model.feishu.FeishuCredential;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 配置加载工具类（非Spring环境，兼容Hutool旧版本）
 */
public class ExcelToolConfig {
    private static final Props PROPS;
    private static final String CONFIG_FILE = "exceltool.properties";

    static {
        // 兼容旧版本Hutool：通过getResource判断配置文件是否存在
        if (ResourceUtil.getResource(CONFIG_FILE) != null) {
            PROPS = new Props(CONFIG_FILE);
        } else {
            throw new RuntimeException("配置文件不存在: " + CONFIG_FILE + "（请放置在resources目录下）");
        }
    }

    /**
     * 获取策略扫描包路径
     *
     * @return 扫描包路径，默认值：exceltool.builder.strategy.impl
     */
    public static String getStrategyScanPackage() {
        return PROPS.getStr("excel.strategy.scan.package", "exceltool.builder.strategy.impl");
    }

    public static String getCustStrategyScanPackage() {
        return PROPS.getStr("excel.strategy.scan.cust.package", "exceltool.builder.strategy.impl.cust");
    }

    /**
     * 获取飞书账号配置列表
     *
     * @return 飞书账号信息列表
     * @throws RuntimeException 配置为空或格式错误时抛出异常
     */
    public static List<FeishuCredential> getFeishuCredentials() {
        String credentialsStr = PROPS.getStr("feishu.credentials");
        if (Objects.isNull(credentialsStr) || credentialsStr.trim().isEmpty()) {
            throw new RuntimeException("飞书账号配置不能为空（配置项：feishu.credentials）");
        }

        List<FeishuCredential> credentials = new ArrayList<>();
        String[] credentialArray = credentialsStr.split(",");
        for (String cred : credentialArray) {
            String[] appInfo = cred.split(":");
            if (appInfo.length != 2) {
                throw new RuntimeException("飞书账号配置格式错误: " + cred + "（正确格式：appId:appSecret，多个用逗号分隔）");
            }
            // 引用独立的FeishuCredential类
            credentials.add(new FeishuCredential(appInfo[0].trim(), appInfo[1].trim()));
        }
        return credentials;
    }
}