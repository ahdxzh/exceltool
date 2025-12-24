package exceltool.param;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DataWithFormat {
    private Object content;
    private String formatCode;

    // 如果不传入格式码就使用类名作为格式码
    public DataWithFormat(Object content) {
        this.content = content;
        this.formatCode = content.getClass().getName();
    }
}
