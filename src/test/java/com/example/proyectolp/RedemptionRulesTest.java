package com.example.proyectolp;

import com.example.proyectolp.service.RedemptionRules;
import com.example.proyectolp.service.CreditRules;
import com.example.proyectolp.model.CreditEntry;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RedemptionRulesTest {
    @Test void conversionAndShortfall(){assertEquals(10_000,RedemptionRules.copValue(5));assertEquals(3,RedemptionRules.missing(7,10));assertEquals(0,RedemptionRules.missing(10,10));}
    @Test void affordabilityIncludesExactBalance(){assertTrue(RedemptionRules.canPay(10,10));assertFalse(RedemptionRules.canPay(9,10));assertEquals(7,RedemptionRules.balanceAfter(10,3));}
    @Test void websiteRequiresSafeHttpsHost(){assertTrue(RedemptionRules.validWebsite("https://example.com/menu"));for(String url:new String[]{"http://example.com","javascript:alert(1)","file:///tmp/a","https://user:pass@example.com","https:///path"})assertFalse(RedemptionRules.validWebsite(url),url);}
    @Test void redemptionDoesNotCountAsCreditsEarned(){CreditEntry earned=new CreditEntry();earned.credits=50;CreditEntry redeemed=new CreditEntry();redeemed.entry_type="redencion";redeemed.credits=-12;assertEquals(50,CreditRules.earnedCredits(java.util.List.of(earned,redeemed)));}
}
