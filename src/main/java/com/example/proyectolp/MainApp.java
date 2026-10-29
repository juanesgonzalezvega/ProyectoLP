package com.example.proyectolp;

import com.example.proyectolp.config.AppConfig;
import com.example.proyectolp.controller.DashboardController;
import com.example.proyectolp.model.Session;
import com.example.proyectolp.service.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.application.HostServices;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainApp extends Application {
    private static Stage stage;
    private static HostServices hostServices;
    private static ExecutorService lifecycle=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"session-lifecycle");t.setDaemon(true);return t;});
    private static String pendingLoginMessage="";
    @Override public void start(Stage primaryStage) {
        stage=primaryStage;hostServices=getHostServices();stage.setTitle("ProyectoLP");stage.setMinWidth(900);stage.setMinHeight(640);
        SessionManager.getInstance().onExpired(message->Platform.runLater(()->showLogin(message)));
        showLogin("");
        lifecycle.submit(()->{try{Session restored=new AuthService(AppConfig.load(),SessionManager.getInstance(),new SessionStorage()).restore();if(restored!=null)Platform.runLater(MainApp::authenticated);}catch(Exception ignored){}});
    }
    public static void showLogin(String message){if(stage==null)return;pendingLoginMessage=message==null?"":message;try{FXMLLoader loader=new FXMLLoader(MainApp.class.getResource("/com/example/proyectolp/ui/login.fxml"));Parent root=loader.load();stage.setScene(new Scene(root,900,640));stage.setTitle("ProyectoLP · Iniciar sesión");stage.show();}catch(IOException e){throw new IllegalStateException("No se pudo abrir la pantalla de inicio de sesión.",e);}}
    public static String loginMessage(){return pendingLoginMessage;}
    public static boolean openLink(String url){if(hostServices==null||!("https://zenquotes.io/".equals(url)||"https://mymemory.translated.net/".equals(url)))return false;try{hostServices.showDocument(url);return true;}catch(Exception ignored){return false;}}
    public static void showRegister(){if(stage==null)return;try{Parent root=FXMLLoader.load(MainApp.class.getResource("/com/example/proyectolp/ui/register.fxml"));stage.getScene().setRoot(root);stage.setTitle("ProyectoLP · Crear cuenta");}catch(IOException e){showLogin("No se pudo abrir el registro.");}}
    public static void authenticated(){if(stage==null||!SessionManager.getInstance().active())return;Session session=SessionManager.getInstance().current();lifecycle.submit(()->{
        try {
            AppConfig config=AppConfig.load();SupabaseService db=new SupabaseService(config);
            String name=session.displayName()==null||session.displayName().isBlank()?session.email():session.displayName();
            db.post("profiles?on_conflict=user_id",Map.of("display_name",name),"resolution=merge-duplicates,return=minimal");
            db.post("user_stats?on_conflict=user_id",Map.of(),"resolution=ignore-duplicates,return=minimal");
            GamificationRuntime.initialize(db);
            SupabaseService.setWriteListener(new SupabaseService.WriteListener(){@Override public void beforeBusinessWrite(String table)throws Exception{GamificationRuntime.streak().beforeAction();}@Override public void afterBusinessWrite(String table)throws Exception{GamificationRuntime.streak().recordAction();}});
            GamificationRuntime.streak().checkPenalty();GamificationRuntime.service().dailyMissions();
            Platform.runLater(()->{if(SessionManager.getInstance().current()==null||!SessionManager.getInstance().current().userId().equals(session.userId()))return;try{FXMLLoader loader=new FXMLLoader(MainApp.class.getResource("/com/example/proyectolp/ui/dashboard.fxml"));Parent root=loader.load();stage.setScene(new Scene(root,1200,820));stage.setTitle("ProyectoLP · Dashboard · "+name);stage.show();}catch(IOException e){showLogin("No se pudo abrir el Dashboard.");}});
        }catch(Exception e){SessionManager.getInstance().clear();new SessionStorage().clearQuietly();SupabaseService.setWriteListener(null);GamificationRuntime.clear();Platform.runLater(()->showLogin("No se pudo inicializar tu espacio. Revisa las políticas y ejecuta los scripts de Fase 9."));}
    });}
    public static void logout(){
        Session captured=SessionManager.getInstance().current();
        if(stage!=null&&stage.getScene()!=null)stage.getScene().getRoot().setDisable(true);
        lifecycle.submit(()->{
            var requestLock=SessionManager.requestLock().writeLock();requestLock.lock();
            try{SessionManager.getInstance().clear();new SessionStorage().clearQuietly();SessionTaskExecutor.reset();try{AttachmentCache.getInstance().clear();}catch(IOException ignored){}SupabaseService.setWriteListener(null);NotificationService.getInstance().clear();GamificationRuntime.clear();}
            finally{requestLock.unlock();}
            Platform.runLater(()->showLogin(""));
            try{new AuthService(AppConfig.load(),SessionManager.getInstance(),new SessionStorage()).revoke(captured);}catch(Exception ignored){}
        });
    }
    @Override public void stop(){if(lifecycle!=null)lifecycle.shutdownNow();try{AttachmentCache.getInstance().clear();AttachmentCache.getInstance().dispose();}catch(IOException ignored){}SupabaseService.setWriteListener(null);}
}
