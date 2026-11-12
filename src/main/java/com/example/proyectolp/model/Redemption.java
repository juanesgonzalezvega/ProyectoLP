package com.example.proyectolp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Redemption {
    public long id;
    @JsonProperty("product_id") public Long productId;
    @JsonProperty("merchant_name") public String merchantName;
    @JsonProperty("product_name") public String productName;
    @JsonProperty("credits_spent") public int creditsSpent;
    @JsonProperty("coupon_code") public String couponCode;
    @JsonProperty("request_id") public String requestId;
    @JsonProperty("created_at") public OffsetDateTime createdAt;
    @JsonProperty("expires_at") public OffsetDateTime expiresAt;
    public Redemption() {}
}
