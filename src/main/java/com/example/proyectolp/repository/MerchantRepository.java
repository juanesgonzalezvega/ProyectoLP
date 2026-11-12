package com.example.proyectolp.repository;

import com.example.proyectolp.model.Merchant;
import com.example.proyectolp.model.MerchantProduct;
import com.example.proyectolp.service.SupabaseService;
import java.util.List;

public final class MerchantRepository {
    private final SupabaseService db;
    public MerchantRepository(SupabaseService db) { this.db = db; }
    public List<Merchant> listActive() throws Exception {
        return db.getList("merchants?select=*&active=eq.true&order=sort_order.asc", Merchant.class);
    }
    public List<MerchantProduct> listProducts(String merchantId) throws Exception {
        String id = java.net.URLEncoder.encode(merchantId, java.nio.charset.StandardCharsets.UTF_8);
        return db.getList("merchant_products?select=*&merchant_id=eq."+id+"&active=eq.true&order=sort_order.asc", MerchantProduct.class);
    }
}
