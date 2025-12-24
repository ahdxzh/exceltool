package exceltool.config;

import com.aspose.cells.License;
import lombok.extern.slf4j.Slf4j;

/**
 * Aspose License 配置
 */
@Slf4j
public class AsposeLicenseConfig {
    public void initAsposeLicense() {
        try {
            // 创建 License 对象
            License license = new License();
            license.setLicense(AsposeLicenseConfig.class.getResourceAsStream("../license/aspose_license.xml"));

            boolean isLicenseValid = License.isLicenseSet();
            if (isLicenseValid) {
                log.info("License is valid");
            } else {
                log.error("License is not valid");
            }
        } catch (Exception e) {
            log.error("Failed to set Aspose License", e);
        }
    }
}
