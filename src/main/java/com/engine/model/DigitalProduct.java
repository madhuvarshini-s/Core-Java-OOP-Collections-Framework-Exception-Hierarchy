package com.engine.model;

import com.engine.exception.ValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Concrete Product specialization representing intangible digital assets (software, ebooks, media).
 */
public class DigitalProduct extends Product {
    private String downloadUrl;
    private long fileSizeBytes;
    private String licenseKey;

    public DigitalProduct(String sku, String name, BigDecimal basePrice, int stockQuantity,
                          String downloadUrl, long fileSizeBytes, String licenseKey) {
        super(sku, name, basePrice, stockQuantity);
        this.downloadUrl = downloadUrl;
        this.fileSizeBytes = fileSizeBytes;
        this.licenseKey = licenseKey;
        validateDigitalAttributes();
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        if (downloadUrl == null || !downloadUrl.startsWith("http")) {
            throw new ValidationException("Valid download URL required");
        }
        this.downloadUrl = downloadUrl;
        touch();
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public String getLicenseKey() {
        return licenseKey;
    }

    public void setLicenseKey(String licenseKey) {
        if (licenseKey == null || licenseKey.isBlank()) {
            throw new ValidationException("License key cannot be blank");
        }
        this.licenseKey = licenseKey;
        touch();
    }

    private void validateDigitalAttributes() {
        if (downloadUrl == null || !downloadUrl.startsWith("http")) {
            throw new ValidationException("Valid HTTP/HTTPS download URL required: " + downloadUrl);
        }
        if (fileSizeBytes <= 0) {
            throw new ValidationException("File size must be strictly positive: " + fileSizeBytes);
        }
        if (licenseKey == null || licenseKey.isBlank()) {
            throw new ValidationException("License key cannot be blank");
        }
    }

    @Override
    public BigDecimal calculateFinalPrice() {
        // Digital delivery rebate/discount (5% discount for zero shipping footprint)
        BigDecimal discount = getBasePrice()
                .multiply(BigDecimal.valueOf(0.05))
                .setScale(2, RoundingMode.HALF_UP);
        return getBasePrice().subtract(discount).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String getProductType() {
        return "DIGITAL";
    }

    @Override
    public String toString() {
        return "DigitalProduct{" +
                "sku='" + getSku() + '\'' +
                ", name='" + getName() + '\'' +
                ", basePrice=" + getBasePrice() +
                ", stockQuantity=" + getStockQuantity() +
                ", downloadUrl='" + downloadUrl + '\'' +
                ", fileSizeBytes=" + fileSizeBytes +
                ", licenseKey='" + licenseKey + '\'' +
                '}';
    }
}
