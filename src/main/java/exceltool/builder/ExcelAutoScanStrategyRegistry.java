package exceltool.builder;

import exceltool.builder.strategy.CellFormatStrategy;
import exceltool.builder.strategy.annotation.ExcelFormatStrategy;
import exceltool.config.ExcelToolConfig;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Excel格式策略注册表：自动扫描+注解order排序（低侵入版）
 */
@Slf4j
public class ExcelAutoScanStrategyRegistry {
    // 策略存放列表
    private static final List<CellFormatStrategy> FORMAT_STRATEGIES = new ArrayList<>();
    // 指定策略扫描包（可配置到yml中，此处硬编码为示例）
    private static final String[] STRATEGY_SCAN_PACKAGE_ARRAY = ExcelToolConfig.getStrategyScanPackage();
    // 默认排序值
    private static final int DEFAULT_ORDER = 100;

    // 静态代码块：项目启动时自动扫描并加载策略
    static {
        autoScanAndRegisterStrategies();
    }

    /**
     * 自动扫描指定包下所有ExcelUnifiedFormatStrategy实现类，实例化并按注解order排序
     */
    private static void autoScanAndRegisterStrategies() {
        try {
            // 1. 构建反射配置：仅保留setUrls，不做任何排除配置（按用户要求）
            ConfigurationBuilder configBuilder = new ConfigurationBuilder()
                    .setUrls(
                            Arrays.stream(STRATEGY_SCAN_PACKAGE_ARRAY)
                                    .flatMap(pkg -> ClasspathHelper.forPackage(pkg).stream())
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.toSet())
                    );

            // 2. 正确创建Reflections实例
            Reflections reflections = new Reflections(configBuilder);

            // 3. 获取所有策略接口的实现类
            Set<Class<? extends CellFormatStrategy>> strategyClasses =
                    reflections.getSubTypesOf(CellFormatStrategy.class);

            // 4. 临时列表存放实例化后的策略（用于排序）
            List<CellFormatStrategy> tempStrategyList = new ArrayList<>();

            // 5. 遍历实例化策略
            for (Class<? extends CellFormatStrategy> clazz : strategyClasses) {
                // 跳过接口和抽象类，只实例化具体实现类
                if (!clazz.isInterface() && !java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {
                    CellFormatStrategy strategy = clazz.getDeclaredConstructor().newInstance();
                    tempStrategyList.add(strategy);

                    // 读取注解信息，使用slf4j打印日志
                    ExcelFormatStrategy annotation = clazz.getAnnotation(ExcelFormatStrategy.class);
                    int order = annotation == null ? DEFAULT_ORDER : annotation.order();
                    String formatCode = strategy.getFormatCode();
                    log.info("自动加载Excel格式策略：{}（order：{}）", formatCode, order);
                }
            }

            // 6. 核心：按注解order排序（数值越小，优先级越高，执行越靠前）
            tempStrategyList.sort(Comparator.comparingInt(strategy -> {
                Class<?> clazz = strategy.getClass();
                ExcelFormatStrategy annotation = clazz.getAnnotation(ExcelFormatStrategy.class);
                return annotation == null ? DEFAULT_ORDER : annotation.order();
            }));

            // 7. 赋值给全局策略列表
            FORMAT_STRATEGIES.addAll(tempStrategyList);

            // 打印排序后的执行顺序
            List<String> sortedStrategyNames = FORMAT_STRATEGIES.stream()
                    .map(CellFormatStrategy::getFormatCode)
                    .collect(Collectors.toList());
            log.info("策略排序完成，执行顺序：{}", sortedStrategyNames);

        } catch (Exception e) {
            log.error("Excel格式策略自动扫描失败", e);
            throw new RuntimeException("Excel格式策略自动扫描失败", e);
        }
    }

    /**
     * 获取所有加载并排序后的格式策略
     */
    public static List<CellFormatStrategy> getFormatStrategies() {
        return new ArrayList<>(FORMAT_STRATEGIES); // 返回副本，避免外部修改策略列表
    }
}