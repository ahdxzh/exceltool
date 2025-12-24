package exceltool.param;

import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class ExcelResource {
    // 全局格式码，用于设置整体格式
    private String globalFormatCode;

    // 标题格式
    private ExcelHeaderFormat excelHeaderFormat;

    // 静态资源，如标题，配图等内容
    private Map<String, DataWithFormat> staticResource;

    // 动态资源，可变数据行，由标识符区别出不同的list，key为标识符
    private Map<String, List<Map<String, DataWithFormat>>> dynamicResource;

    public ExcelResource(ExcelHeaderFormat excelHeaderFormat, Map<String, DataWithFormat> staticResource) {
        this.globalFormatCode = "default";
        this.excelHeaderFormat = excelHeaderFormat;
        this.staticResource = staticResource;
        this.dynamicResource = new HashMap<>();
    }

    // 手动设增加动态资源
    public void putDynamicResource(String key, List<Map<String, DataWithFormat>> dynamicResource) {
        this.dynamicResource.put(key, dynamicResource);
    }

    // 没有配置格式的List<Map<String, Object>>，根据Object类型设置格式码，然后加入动态资源中
    public void putDynamicDataList(String key, List<Map<String, Object>> dataList) {
        List<Map<String, DataWithFormat>> collect = dataList.stream()
                .map(map -> map.entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> new DataWithFormat(entry.getValue())
                        ))
                )
                .collect(Collectors.toList());
        putDynamicResource(key, collect);
    }
}
