package exceltool.builder.strategy.annotation;

import java.lang.annotation.*;

/**
 * Excel策略注解：低侵入配置【排序order+策略名称+压制未使用告警】
 * 无需依赖接口常量，直接填写order数值即可
 */
@Target(ElementType.TYPE) // 仅作用于策略类
@Retention(RetentionPolicy.RUNTIME) // 运行时反射读取order和formatCode
@Documented
public @interface ExcelFormatStrategy {
    /**
     * 排序值（核心：数值越小，优先级越高，执行越靠前）
     * 默认值100，便于未配置的策略统一排在后面
     */
    int order() default 100;

    /**
     * 策略名称/格式编码
     * 默认空字符串，后续自动取类名
     */
    String formatCode() default "";
}