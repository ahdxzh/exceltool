package exceltool.builder.strategy;

import exceltool.config.ExcelToolConfig;
import exceltool.param.DataWithFormat;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Excel行数据格式处理策略类
 * 负责扫描自定义格式策略并批量处理行内单元格格式
 */
@Slf4j
public class RowHandleStrategy {
    // 自定义策略扫描包路径（提取为常量，便于配置修改）
    private static final String CUST_STRATEGY_SCAN_PACKAGE = ExcelToolConfig.getCustStrategyScanPackage();
    // 格式编码Map（存储List类型格式编码映射，LinkedHashMap保证有序）
    private static final Map<String, String> FORMAT_CODE_MAP;
    // List后缀常量（语义化命名，便于理解）
    private static final String LIST_SUFFIX = "List";
    // 逗号分隔符常量（语义化命名，便于维护）
    private static final String COMMA_SEPARATOR = ",";

    // 静态代码块：项目启动时初始化格式映射表（饿汉式加载，线程安全）
    static {
        FORMAT_CODE_MAP = Collections.unmodifiableMap(autoScanCustStrategies());
    }

    /**
     * 自动扫描自定义格式策略并构建格式编码映射
     *
     * @return 不可修改的格式编码映射Map
     */
    private static Map<String, String> autoScanCustStrategies() {
        // 初始容量设置（避免LinkedHashMap扩容损耗，默认16，根据实际策略数量调整）
        Map<String, String> formatCodeMap = new LinkedHashMap<>(16);
        try {
            // 构建Reflections配置，指定扫描包
            ConfigurationBuilder configBuilder = new ConfigurationBuilder()
                    .setUrls(ClasspathHelper.forPackage(CUST_STRATEGY_SCAN_PACKAGE))
                    .setParallel(true); // 开启并行扫描，提升扫描效率（多模块场景下更明显）

            Reflections reflections = new Reflections(configBuilder);
            // 获取所有CellFormatStrategy的实现类
            Set<Class<? extends CellFormatStrategy>> strategyClasses = reflections.getSubTypesOf(CellFormatStrategy.class);

            // 遍历实现类，过滤接口和抽象类，构建映射关系
            for (Class<? extends CellFormatStrategy> strategyClass : strategyClasses) {
                boolean isConcreteClass = !strategyClass.isInterface() && !Modifier.isAbstract(strategyClass.getModifiers());
                if (isConcreteClass) {
                    CellFormatStrategy strategy = strategyClass.getDeclaredConstructor().newInstance();
                    String formatCode = strategy.getFormatCode();
                    // 过滤空格式编码，避免无效映射
                    if (Objects.nonNull(formatCode) && !formatCode.trim().isEmpty()) {
                        String key = formatCode + LIST_SUFFIX;
                        // 避免重复key覆盖（若有重复策略，打印日志提醒）
                        if (formatCodeMap.containsKey(key)) {
                            log.warn("Excel格式策略重复，key:{} 已存在，当前策略类:{} 将被忽略",
                                    key, strategyClass.getName());
                            continue;
                        }
                        formatCodeMap.put(key, formatCode);
                    } else {
                        log.warn("Excel格式策略类:{} 的formatCode为空，已忽略", strategyClass.getName());
                    }
                }
            }
            log.info("Excel自定义格式策略扫描完成，共加载{}个有效策略", formatCodeMap.size());
        } catch (Exception exception) {
            log.error("Excel格式策略自动扫描失败", exception);
            // 抛出运行时异常，快速失败，避免后续无效操作
            throw new RuntimeException("Excel格式策略自动扫描失败", exception);
        }
        return formatCodeMap;
    }

    /**
     * 处理行数据的单元格格式（批量应用自定义策略）
     *
     * @param currentRowData 待处理的行数据（key：列标识，value：数据+格式信息）
     */
    public static void handleRowFormatter(Map<String, DataWithFormat> currentRowData) {
        // 前置校验：避免空指针异常
        if (Objects.isNull(currentRowData) || currentRowData.isEmpty() || FORMAT_CODE_MAP.isEmpty()) {
            return;
        }

        // 遍历格式映射，批量处理单元格格式
        for (Map.Entry<String, String> entry : FORMAT_CODE_MAP.entrySet()) {
            String key = entry.getKey();
            String formatCode = entry.getValue();
            handleSingleStrategyFormatter(currentRowData, key, formatCode);
        }
    }

    /**
     * 处理单个格式策略的行数据格式化
     *
     * @param currentRowData   待处理的行数据
     * @param listKey          格式策略的List类型key（如：redFontList）
     * @param targetFormatCode 目标格式编码
     */
    private static void handleSingleStrategyFormatter(Map<String, DataWithFormat> currentRowData,
                                                      String listKey,
                                                      String targetFormatCode) {
        // 获取当前策略对应的列列表数据
        DataWithFormat listDataWithFormat = currentRowData.get(listKey);
        if (Objects.isNull(listDataWithFormat)) {
            return;
        }

        // 处理列列表字符串（避免空指针、空字符串）
        Object contentObj = listDataWithFormat.getContent();
        if (Objects.isNull(contentObj)) {
            return;
        }
        String columnListStr = contentObj.toString().trim();
        if (columnListStr.isEmpty()) {
            return;
        }

        // 拆分列标识并批量设置格式
        String[] targetColumns = columnListStr.split(COMMA_SEPARATOR);
        for (String column : targetColumns) {
            String columnTrimmed = column.trim();
            // 过滤空列标识，避免无效操作
            if (columnTrimmed.isEmpty()) {
                continue;
            }
            DataWithFormat columnData = currentRowData.get(columnTrimmed);
            if (Objects.nonNull(columnData)) {
                columnData.setFormatCode(targetFormatCode);
            } else {
                // 调试日志，便于排查列标识不存在的问题
                log.debug("Excel行数据中未找到列标识:{}，跳过格式设置", columnTrimmed);
            }
        }
    }
}