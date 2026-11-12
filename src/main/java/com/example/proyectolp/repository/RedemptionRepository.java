package com.example.proyectolp.repository;

import com.example.proyectolp.model.Redemption;
import com.example.proyectolp.model.RedemptionResult;
import com.example.proyectolp.service.SupabaseService;
import java.util.List;
import java.util.Map;

public final class RedemptionRepository {
    private final SupabaseService db;
    public RedemptionRepository(SupabaseService db) { this.db = db; }
    public List<Redemption> listOwn() throws Exception {
        return db.getList("redemptions?select=*&order=created_at.desc", Redemption.class);
    }
    public RedemptionResult redeem(long productId, String requestId) throws Exception {
        return db.post("rpc/redeem_product", Map.of("p_product_id", productId, "p_request_id", requestId), RedemptionResult.class);
    }
}
