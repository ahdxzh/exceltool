package exceltool.utils;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 飞书工具类 - 图片上传与聊天窗口发送
 * 功能：支持多账号故障转移、Token缓存、图片上传、WebHook图片发送
 */
@Slf4j
public class FeishuUtils {
    // ================================ 常量提取（消除硬编码，便于维护） ================================
    // 错误码常量
    private static final int ERROR_CODE_QUOTA_EXCEEDED = 99991403;
    private static final int ERROR_CODE_TOKEN_INVALID = 99991663;
    private static final int SUCCESS_CODE = 0;
    // 请求URL常量
    private static final String TOKEN_URL = "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal";
    private static final String UPLOAD_IMAGE_URL = "https://open.feishu.cn/open-apis/im/v1/images";
    // 请求头常量
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String HEADER_BEARER_PREFIX = "Bearer ";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_JSON = "application/json; charset=utf-8";
    // 请求参数常量
    private static final String FORM_KEY_IMAGE_TYPE = "image_type";
    private static final String FORM_VALUE_IMAGE_TYPE_MESSAGE = "message";
    private static final String FORM_KEY_IMAGE = "image";
    // JSON字段常量
    private static final String JSON_KEY_MSG = "msg";
    private static final String JSON_VALUE_OK = "ok";
    private static final String JSON_KEY_TENANT_ACCESS_TOKEN = "tenant_access_token";
    private static final String JSON_KEY_EXPIRE = "expire";
    private static final String JSON_KEY_CODE = "code";
    private static final String JSON_KEY_DATA_IMAGE_KEY = "data.image_key";
    private static final String JSON_KEY_MSG_TYPE = "msg_type";
    private static final String JSON_VALUE_MSG_TYPE_IMAGE = "image";
    private static final String JSON_KEY_CONTENT = "content";
    private static final String JSON_KEY_IMAGE_KEY = "image_key";
    // 其他常量
    private static final long TOKEN_EXPIRE_BUFFER = 60; // Token过期缓冲时间（秒），避免过期瞬间使用
    private static final int DEFAULT_TOKEN_EXPIRE = 3600; // 默认Token过期时间（秒）
    private static final int HTTP_TIMEOUT = 30000; // HTTP请求超时时间（毫秒）
    private static final String OP_DESC_GET_TOKEN = "获取租户AccessToken";
    private static final String OP_DESC_UPLOAD_IMAGE = "图片上传";
    private static final String OP_DESC_SEND_IMAGE = "图片发送到聊天窗口";

    // ================================ 账号配置（可后续迁移至配置文件） ================================
    @Data
    @AllArgsConstructor
    private static class Credential {
        String appId;
        String appSecret;
        String accessToken; // 缓存Token
        long expireAt;      // Token到期时间（秒）
    }

    // 账号列表（优先使用自建账户，避免影响公司业务）
    private static final List<Credential> CREDENTIALS = new ArrayList<>(Arrays.asList(
            new Credential("cli_a53a3449003d5013", "71wrAp0M1B61v9jeQc7BAc1FKfSss6ea", null, 0),
            new Credential("cli_a53a3bc70459d00e", "XZSzwAIH569OrjFVnrjsdgfu8Bc8GJCS", null, 0)
    ));

    private static int currentIndex = 0; // 当前使用的账号索引

    // ================================ 公共工具方法（消除代码冗余） ================================
    /**
     * 统一校验HTTP响应结果
     * @param resp HTTP响应对象
     * @param operationDesc 操作描述（用于精准提示异常信息）
     */
    private static void validateHttpResponse(HttpResponse resp, String operationDesc) {
        if (resp == null) {
            throw new RuntimeException(operationDesc + "失败：响应对象为空");
        }

        String respBody = resp.body();
        log.debug("{}响应内容：{}", operationDesc, respBody);

        // 校验响应体非空
        if (respBody == null || respBody.trim().isEmpty()) {
            throw new RuntimeException(operationDesc + "失败：响应体为空");
        }

        JSONObject json = JSONUtil.parseObj(respBody);
        Integer code = json.getInt(JSON_KEY_CODE);

        // Token获取接口特殊校验（先判断msg，再兼容code）
        if (OP_DESC_GET_TOKEN.equals(operationDesc)) {
            if (!JSON_VALUE_OK.equals(json.getStr(JSON_KEY_MSG))) {
                throw new RuntimeException(operationDesc + "失败：" + respBody);
            }
            return;
        }

        // 普通接口校验（code判断）
        if (code == null) {
            log.warn("{}响应无错误码，默认认为成功：{}", operationDesc, respBody);
            return;
        }

        if (code == ERROR_CODE_QUOTA_EXCEEDED) {
            String errMsg = operationDesc + "失败：账号配额超限";
            log.error(errMsg);
            throw new QuotaExceededException(errMsg);
        }
        if (code == ERROR_CODE_TOKEN_INVALID) {
            String errMsg = operationDesc + "失败：Token无效/已过期";
            log.error(errMsg);
            throw new TokenInvalidException(errMsg);
        }
        if (code != SUCCESS_CODE) {
            String errMsg = operationDesc + "失败：" + respBody;
            log.error(errMsg);
            throw new RuntimeException(errMsg);
        }
    }

    /**
     * 校验参数有效性
     * @param file 图片文件（可为null，仅上传/发送时校验）
     * @param webhookUrl WebHook地址（可为null，仅发送时校验）
     */
    private static void validateParams(File file, String webhookUrl) {
        // 图片文件校验（非空时）
        if (file != null) {
            if (!file.exists()) {
                throw new IllegalArgumentException("图片文件不存在：" + file.getAbsolutePath());
            }
            if (!file.isFile()) {
                throw new IllegalArgumentException("指定路径不是有效文件：" + file.getAbsolutePath());
            }
            if (file.length() <= 0) {
                throw new IllegalArgumentException("图片文件为空（大小为0）：" + file.getAbsolutePath());
            }
        }

        // WebHook地址校验（非空时）
        if (webhookUrl != null && webhookUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("WebHook地址不能为空或空白字符串");
        }
    }

    // ================================ Token相关方法 ================================
    /**
     * 获取租户AccessToken（带缓存，同步锁保证线程安全）
     * @param credential 账号认证信息
     * @return 有效AccessToken
     */
    private static synchronized String getTenantAccessToken(Credential credential) {
        if (credential == null) {
            throw new IllegalArgumentException("账号认证信息不能为空");
        }

        long now = System.currentTimeMillis() / 1000;

        // 缓存有效，直接返回
        if (credential.getAccessToken() != null && credential.getExpireAt() > now + TOKEN_EXPIRE_BUFFER) {
            log.debug("使用缓存的AccessToken（前10位脱敏）：{}****", credential.getAccessToken().substring(0, Math.min(credential.getAccessToken().length(), 10)));
            return credential.getAccessToken();
        }

        log.info("缓存AccessToken失效/不存在，开始重新获取");
        // 构造请求体
        JSONObject requestBody = JSONUtil.createObj()
                .set("app_id", credential.getAppId())
                .set("app_secret", credential.getAppSecret());

        // 执行请求（try-with-resources自动关闭响应流，避免资源泄漏）
        try (HttpResponse resp = HttpRequest.post(TOKEN_URL)
                .header(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON)
                .body(requestBody.toString())
                .timeout(HTTP_TIMEOUT)
                .execute()) {

            // 统一响应校验
            validateHttpResponse(resp, OP_DESC_GET_TOKEN);

            // 解析Token和过期时间
            JSONObject respJson = JSONUtil.parseObj(resp.body());
            String tenantToken = respJson.getStr(JSON_KEY_TENANT_ACCESS_TOKEN);
            int expireSeconds = respJson.getInt(JSON_KEY_EXPIRE, DEFAULT_TOKEN_EXPIRE);

            // 校验Token有效性
            if (tenantToken == null || tenantToken.trim().isEmpty()) {
                throw new RuntimeException(OP_DESC_GET_TOKEN + "失败：解析到空的AccessToken");
            }

            // 更新缓存
            credential.setAccessToken(tenantToken);
            credential.setExpireAt(now + expireSeconds);
            log.info("AccessToken重新获取成功，过期时间：{}秒后", expireSeconds);
            return tenantToken;

        } catch (Exception e) {
            log.error(OP_DESC_GET_TOKEN + "执行异常", e);
            throw e;
        }
    }

    // ================================ 多账号故障转移执行器 ================================
    @FunctionalInterface
    private interface FailJob<T> {
        T call() throws Exception;
    }

    /**
     * 多账号故障转移执行方法
     * @param job 待执行的业务逻辑
     * @param <T> 返回值类型
     * @return 业务逻辑执行结果
     */
    private static <T> T executeWithFailover(FailJob<T> job) {
        int triedCount = 0;
        int totalAccount = CREDENTIALS.size();

        if (totalAccount == 0) {
            throw new RuntimeException("飞书账号列表为空，无法执行故障转移");
        }

        while (triedCount < totalAccount) {
            try {
                log.debug("使用第{}个账号执行业务逻辑（索引：{}）", triedCount + 1, currentIndex);
                return job.call();
            } catch (QuotaExceededException qe) {
                // 配额超限，切换下一个账号
                log.error("第{}个账号配额超限，开始切换下一个账号", triedCount + 1);
                currentIndex = (currentIndex + 1) % totalAccount;
                triedCount++;
            } catch (TokenInvalidException te) {
                // Token无效，不切换账号，重新尝试（内部会刷新Token）
                log.error("当前账号Token无效，将重新刷新Token并重试");
            } catch (Exception e) {
                throw new RuntimeException("业务逻辑执行失败：" + e.getMessage(), e);
            }
        }

        // 所有账号均尝试失败
        throw new RuntimeException("所有" + totalAccount + "个飞书账号均不可用（配额超限或异常），无法执行业务逻辑");
    }

    // ================================ 图片上传相关方法 ================================
    /**
     * 上传图片（带多账号故障转移）
     * @param file 图片文件
     * @return 图片唯一标识image_key
     */
    private static String uploadImage(File file) {
        validateParams(file, null);
        log.info("开始上传图片：{}", file.getAbsolutePath());

        return executeWithFailover(() -> doUploadImage(CREDENTIALS.get(currentIndex), file));
    }

    /**
     * 实际执行图片上传逻辑
     * @param credential 账号认证信息
     * @param file 图片文件
     * @return 图片唯一标识image_key
     */
    private static String doUploadImage(Credential credential, File file) {
        // 获取有效Token
        String token = getTenantAccessToken(credential);

        // 执行上传请求
        try (HttpResponse resp = HttpRequest.post(UPLOAD_IMAGE_URL)
                .header(HEADER_AUTHORIZATION, HEADER_BEARER_PREFIX + token)
                .form(FORM_KEY_IMAGE_TYPE, FORM_VALUE_IMAGE_TYPE_MESSAGE)
                .form(FORM_KEY_IMAGE, file)
                .timeout(HTTP_TIMEOUT)
                .execute()) {

            // 统一响应校验
            validateHttpResponse(resp, OP_DESC_UPLOAD_IMAGE);

            // 解析image_key并校验
            JSONObject respJson = JSONUtil.parseObj(resp.body());
            String imageKey = respJson.getByPath(JSON_KEY_DATA_IMAGE_KEY, String.class);
            if (imageKey == null || imageKey.trim().isEmpty()) {
                throw new RuntimeException(OP_DESC_UPLOAD_IMAGE + "成功，但未获取到有效image_key：" + resp.body());
            }

            log.info("图片上传成功，image_key：{}", imageKey);
            return imageKey;

        } catch (Exception e) {
            log.error(OP_DESC_UPLOAD_IMAGE + "执行异常", e);
            throw e;
        }
    }

    // ================================ 图片发送相关方法 ================================
    /**
     * 发送图片到飞书聊天窗口（WebHook方式）
     * @param webhookUrl 聊天窗口WebHook地址
     * @param file 图片文件
     */
    public static void sendImageToChat(String webhookUrl, File file) {
        validateParams(file, webhookUrl);
        log.info("开始发送图片到聊天窗口，WebHook地址：{}，图片路径：{}", webhookUrl, file.getAbsolutePath());

        // 1. 上传图片获取image_key
        String imageKey = uploadImage(file);

        // 2. 构造发送请求体
        JSONObject content = JSONUtil.createObj()
                .set(JSON_KEY_IMAGE_KEY, imageKey);
        JSONObject requestBody = JSONUtil.createObj()
                .set(JSON_KEY_MSG_TYPE, JSON_VALUE_MSG_TYPE_IMAGE)
                .set(JSON_KEY_CONTENT, content);
        log.debug("图片发送请求体：{}", requestBody.toString());

        // 3. 执行发送请求
        try (HttpResponse resp = HttpRequest.post(webhookUrl)
                .header(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON)
                .body(requestBody.toString())
                .timeout(HTTP_TIMEOUT)
                .execute()) {

            // 统一响应校验（修复原异常信息错误）
            validateHttpResponse(resp, OP_DESC_SEND_IMAGE);
            log.info("图片发送到聊天窗口成功，响应内容：{}", resp.body());

        } catch (Exception e) {
            log.error(OP_DESC_SEND_IMAGE + "执行异常", e);
            throw e;
        }
    }

    // ================================ 自定义异常 ================================
    private static class QuotaExceededException extends RuntimeException {
        public QuotaExceededException(String msg) {
            super(msg);
        }
    }

    private static class TokenInvalidException extends RuntimeException {
        public TokenInvalidException(String msg) {
            super(msg);
        }
    }
}