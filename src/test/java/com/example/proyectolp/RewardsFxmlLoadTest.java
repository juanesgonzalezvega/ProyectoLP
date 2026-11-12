package com.example.proyectolp;

import com.example.proyectolp.controller.RewardsController;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class RewardsFxmlLoadTest {
    @Test void rewardsFxmlAndIncludedRedeemViewLoad() throws Exception {
        CountDownLatch started=new CountDownLatch(1);
        try { Platform.startup(started::countDown); } catch (IllegalStateException alreadyStarted) { started.countDown(); }
        assertTrue(started.await(10,TimeUnit.SECONDS));
        CountDownLatch loaded=new CountDownLatch(1);
        Throwable[] failure=new Throwable[1];
        Platform.runLater(()->{try{FXMLLoader loader=new FXMLLoader(getClass().getResource("/com/example/proyectolp/ui/rewards.fxml"));Parent root=loader.load();assertNotNull(root);assertNotNull(loader.getController());}catch(Throwable e){failure[0]=e;}finally{loaded.countDown();}});
        assertTrue(loaded.await(10,TimeUnit.SECONDS));
        if(failure[0]!=null)fail(failure[0]);
        Platform.exit();
    }
}
