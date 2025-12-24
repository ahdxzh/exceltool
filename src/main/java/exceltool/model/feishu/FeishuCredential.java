package exceltool.model.feishu;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FeishuCredential {
    String appId;
    String appSecret;
    String accessToken;
    long expireAt;

    public FeishuCredential(String appId, String appSecret) {
        this.appId = appId;
        this.appSecret = appSecret;
    }
}
