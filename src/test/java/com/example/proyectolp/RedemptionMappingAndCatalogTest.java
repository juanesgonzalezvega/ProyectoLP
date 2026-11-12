package com.example.proyectolp;

import com.example.proyectolp.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.regex.*;
import static org.junit.jupiter.api.Assertions.*;

class RedemptionMappingAndCatalogTest {
    private final ObjectMapper mapper=new ObjectMapper().findAndRegisterModules();
    @Test void snakeCaseMappingIgnoresUnknownFields()throws Exception{
        Merchant m=mapper.readValue("{\"id\":\"x\",\"website_url\":\"https://x.test\",\"sort_order\":2,\"user_id\":\"hidden\"}",Merchant.class);
        MerchantProduct p=mapper.readValue("{\"merchant_id\":\"x\",\"credit_cost\":7,\"sort_order\":1,\"extra\":true}",MerchantProduct.class);
        Redemption r=mapper.readValue("{\"merchant_name\":\"X\",\"credits_spent\":7,\"coupon_code\":\"PLP-X\",\"created_at\":\"2026-01-01T00:00:00Z\",\"expires_at\":\"2026-01-31T00:00:00Z\",\"user_id\":\"private\"}",Redemption.class);
        RedemptionResult result=mapper.readValue("{\"coupon_code\":\"PLP-X\",\"balance_after\":3,\"credits_spent\":7,\"merchant_name\":\"X\",\"product_name\":\"Y\",\"created_at\":\"2026-01-01T00:00:00Z\",\"expires_at\":\"2026-01-31T00:00:00Z\",\"other\":1}",RedemptionResult.class);
        assertEquals("https://x.test",m.websiteUrl);assertEquals(7,p.creditCost);assertEquals("X",r.merchantName);assertEquals(3,result.balanceAfter);
    }
    @Test void seedHasSixRestaurantsAndFiveDistinctPositiveProductsEach()throws Exception{
        String sql=Files.readString(Path.of("supabase/12_canje.sql"));
        Matcher merchant=Pattern.compile("\\('([a-z-]+)','(?:Frisby|Burger King|Little Caesars|Popsy|El Corral|McDonald)").matcher(sql);
        int merchants=0;while(merchant.find())merchants++;assertEquals(6,merchants);
        String seed=sql.substring(sql.indexOf("insert into public.merchant_products"),sql.indexOf("on conflict(merchant_id,name)"));
        Matcher row=Pattern.compile("\\('([^']+)','([^']+)','[^']*','[^']+',([0-9]+),[0-9]+\\)").matcher(seed);
        java.util.Map<String,java.util.Set<String>> names=new java.util.HashMap<>();int count=0;while(row.find()){count++;assertTrue(Integer.parseInt(row.group(3))>0);assertTrue(names.computeIfAbsent(row.group(1),k->new java.util.HashSet<>()).add(row.group(2)));}
        assertEquals(30,count);assertEquals(6,names.size());assertTrue(names.values().stream().allMatch(s->s.size()==5));
    }
}
