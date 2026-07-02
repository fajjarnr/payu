package id.payu.api.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payu.openapi")
public class OpenApiProperties {
    private String title;
    private String description;
    private String version = "1.0.0";
    private String contactName = "PayU Engineering";
    private String contactEmail = "engineering@payu.fajjjar.my.id";
    private String licenseName = "Proprietary";
    private String licenseUrl = "https://payu.fajjjar.my.id/license";
    private boolean addBearerAuth = true;

    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public String getLicenseName() { return licenseName; }
    public void setLicenseName(String licenseName) { this.licenseName = licenseName; }
    public String getLicenseUrl() { return licenseUrl; }
    public void setLicenseUrl(String licenseUrl) { this.licenseUrl = licenseUrl; }
    public boolean isAddBearerAuth() { return addBearerAuth; }
    public void setAddBearerAuth(boolean addBearerAuth) { this.addBearerAuth = addBearerAuth; }
}
