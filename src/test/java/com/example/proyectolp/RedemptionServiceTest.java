package com.example.proyectolp;

import com.example.proyectolp.model.RedemptionResult;
import com.example.proyectolp.service.RedemptionService;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class RedemptionServiceTest {
    @Test void insufficientBalanceDoesNotCallServer(){AtomicInteger calls=new AtomicInteger();RedemptionService service=new RedemptionService((id,request)->{calls.incrementAndGet();return new RedemptionResult();});Exception e=assertThrows(Exception.class,()->service.redeem(1,10,9,"same"));assertTrue(e.getMessage().contains("te faltan 1"));assertEquals(0,calls.get());}
    @Test void stableRequestIdCanBeRetriedAndServerErrorsAreTranslated() throws Exception {AtomicInteger calls=new AtomicInteger();RedemptionService service=new RedemptionService((id,request)->{assertEquals("stable-request",request);if(calls.getAndIncrement()==0)throw new IOException("network connection failed");return new RedemptionResult();});Exception first=assertThrows(Exception.class,()->service.redeem(4,2,2,"stable-request"));assertTrue(first.getMessage().contains("No hay conexión"));assertNotNull(service.redeem(4,2,2,"stable-request"));assertEquals(2,calls.get());}
    @Test void serverRejectionsHaveStableSpanishMessages(){for(String[] error:new String[][]{{"insufficient_credits","No tienes créditos suficientes"},{"product_unavailable","ya no está disponible"}}){RedemptionService service=new RedemptionService((id,request)->{throw new IOException(error[0]);});Exception e=assertThrows(Exception.class,()->service.redeem(1,1,1,"x"));assertTrue(e.getMessage().contains(error[1]));}}
    @Test void concurrentClicksMakeOneCall(){CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);AtomicInteger calls=new AtomicInteger();RedemptionService service=new RedemptionService((id,request)->{calls.incrementAndGet();entered.countDown();release.await();return new RedemptionResult();});ExecutorService pool=Executors.newSingleThreadExecutor();try{Future<?> first=pool.submit(()->{try{service.redeem(1,1,1,"x");}catch(Exception ignored){}});assertTrue(entered.await(2,TimeUnit.SECONDS));Exception second=assertThrows(Exception.class,()->service.redeem(1,1,1,"x"));assertTrue(second.getMessage().contains("en proceso"));release.countDown();first.get(2,TimeUnit.SECONDS);assertEquals(1,calls.get());}catch(Exception e){fail(e);}finally{release.countDown();pool.shutdownNow();}}
}

