package exceltool.param;

import lombok.Data;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Data
public class ExcelHeaderFormat {
    String hexColor;
    int start;
    int end;

    public ExcelHeaderFormat(String hexColor, int start, int end) {
        this.hexColor = hexColor;
        this.start = start;
        this.end = end;
    }

    public static String generateHexColorByIndex(Integer index) {
        int remainder = index % 7;

        switch (remainder) {
            case 0:
                return "#f4d0a6";
            case 1:
                return "#c9eac6";
            case 2:
                return "#cbb0f9";
            case 3:
                return "#FFBA6B";
            case 4:
                return "#F0BEBE";
            case 5:
                return "#B2E34C";
            case 6:
                return "#FF595E";
            default:
                return "#23A6FF";
        }
    }

    @SuppressWarnings("unused")
    public static String generateHexColorByStr(String str) {
        int hashedValue = getHashedValue(str);
        return generateHexColorByIndex(hashedValue);
    }

    public static int getHashedValue(String input) {
        try {
            // 使用 MD5 哈希算法
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(input.getBytes());

            // 将哈希字节数组转换为整数
            int hashCode = 0;
            for (int i = 0; i < Math.min(hashBytes.length, 4); i++) {  // 只取前4个字节
                hashCode = (hashCode << 8) | (hashBytes[i] & 0xFF);
            }

            // 取模运算得到一个[0, 1, 2, 3, 4, 5]之间的数字
            return Math.abs(hashCode) % 6;  // 结果限制为0到5
        } catch (NoSuchAlgorithmException e) {
            return -1;  // 出错时返回-1
        }
    }
}
