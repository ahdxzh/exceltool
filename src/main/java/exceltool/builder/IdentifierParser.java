package exceltool.builder;

import cn.hutool.core.util.ReUtil;

class IdentifierParser {
    private static final String STATIC_PATTERN = "\\{\\{(\\w+)\\}\\}";
    private static final String DYNAMIC_PATTERN = "\\{\\{(\\w+)\\.(\\w+)\\}\\}";

    // 对标记进行拆分。例如p.date，p是用来区分多组list的，date是单行数据的key
    public static String getIdentifier(String cellStr) {
        if (cellStr == null) {
            return null;
        }
        return ReUtil.get(DYNAMIC_PATTERN, cellStr, 1);
    }

    public static String getDynamicKey(String cellStr) {
        if (cellStr == null) {
            return null;
        }
        return ReUtil.get(DYNAMIC_PATTERN, cellStr, 2);
    }

    public static boolean containsDynamicKey(String cellStr) {
        if (cellStr == null) {
            return false;
        }
        return ReUtil.contains(DYNAMIC_PATTERN, cellStr);
    }

    public static String getStaticKey(String cellStr) {
        if (cellStr == null) {
            return null;
        }
        return ReUtil.get(STATIC_PATTERN, cellStr, 1);
    }
}
