package exceltool.utils;

import java.util.regex.Pattern;

/**
 * Excel文件名安全处理工具类
 * 负责Excel文件名的非法字符过滤、默认值补充、后缀拼接等操作
 */
@SuppressWarnings("unused")
public class ExcelFileNameUtils {
    // 优化：常量命名更清晰，明确是“非法文件名匹配模式”
    private static final Pattern ILLEGAL_FILE_NAME_PATTERN = Pattern.compile("[\\\\/:*?\"<>|]");
    // 默认Excel文件名
    private static final String DEFAULT_EXCEL_TITLE = "无标题";
    // Excel后缀（xlsx格式）
    private static final String EXCEL_SUFFIX = ".xlsx";

    // 工具类私有化构造方法，禁止实例化（工具类最佳实践）
    private ExcelFileNameUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * 生成安全的Excel文件名（过滤非法字符，补充默认标题，拼接xlsx后缀）
     *
     * @param title 原始文件标题
     * @return 安全可用的Excel文件名（如：“报表统计.xlsx”）
     */
    public static String createSafeExcelFileName(String title) {
        // 处理原始标题为null的情况（原代码未处理，补充健壮性）
        String originalTitle = (title == null) ? "" : title.trim();
        
        // 过滤非法字符，用空字符串替换
        String safeTitle = ILLEGAL_FILE_NAME_PATTERN.matcher(originalTitle).replaceAll("");
        
        // 处理空标题的情况
        if (safeTitle.isEmpty()) {
            safeTitle = DEFAULT_EXCEL_TITLE;
        }
        
        return String.format("%s%s", safeTitle, EXCEL_SUFFIX);
    }
}