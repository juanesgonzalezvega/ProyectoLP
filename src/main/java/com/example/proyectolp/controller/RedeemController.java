package com.example.proyectolp.controller;

import com.example.proyectolp.config.AppConfig;
import com.example.proyectolp.model.*;
import com.example.proyectolp.repository.*;
import com.example.proyectolp.service.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class RedeemController {
    private static volatile List<Merchant> sessionCatalog;
    private static final Map<String,List<MerchantProduct>> sessionProducts=new java.util.concurrent.ConcurrentHashMap<>();
    private static volatile long catalogGeneration=-1;
    public static void clearSessionCache(){sessionCatalog=null;catalogGeneration=-1;sessionProducts.clear();}
    @FXML private Label balanceLabel,copLabel,statusLabel;
    @FXML private FlowPane merchantList,historyList;
    @FXML private VBox menuHost;
    @FXML private ScrollPane merchantPane,menuPane,historyPane;
    private final java.util.concurrent.ExecutorService executor=SessionTaskExecutor.get();
    private final Map<Long,String> requestIds=new HashMap<>();
    private List<Merchant> catalog;
    private Merchant selectedMerchant;
    private List<Redemption> history=List.of();
    private int balance;
    private boolean canjeEnCurso;
    private Runnable refreshCallback=()->{};
    private RedemptionRepository redemptionRepository;
    private CreditLedgerRepository creditRepository;
    private final long generation=SessionManager.getInstance().generation();

    @FXML private void initialize(){refresh();}
    public void setRefreshCallback(Runnable callback){refreshCallback=callback==null?()->{}:callback;}
    public void refresh(){statusLabel.setText("Cargando catálogo y saldo…");executor.submit(()->{try{SupabaseService db=new SupabaseService(AppConfig.load());MerchantRepository mr=new MerchantRepository(db);CreditLedgerRepository cr=new CreditLedgerRepository(db);RedemptionRepository rr=new RedemptionRepository(db);List<Merchant> ms=sessionCatalog!=null&&catalogGeneration==generation?sessionCatalog:mr.listActive();List<CreditEntry> entries=cr.list();List<Redemption> rs=rr.listOwn();int total=entries.stream().mapToInt(e->e.credits).sum();Platform.runLater(()->{if(!current())return;catalog=ms;sessionCatalog=ms;catalogGeneration=generation;history=rs;balance=total;creditRepository=cr;redemptionRepository=rr;balanceLabel.setText(total+" CRÉDITOS");copLabel.setText("≈ $"+String.format(Locale.forLanguageTag("es-CO"),"%,d",total*CreditRules.COP_PER_CREDIT)+" COP");renderMerchants();statusLabel.setText("");});}catch(Exception e){Platform.runLater(()->{if(current())statusLabel.setText("No se pudo cargar el catálogo. Revisa la conexión y el script de canje.");});}});}
    private boolean current(){return SessionManager.getInstance().generation()==generation&&SessionManager.getInstance().active();}
    private void renderMerchants(){merchantList.getChildren().clear();if(catalog==null)return;for(Merchant m:catalog)try{FXMLLoader l=new FXMLLoader(getClass().getResource("/com/example/proyectolp/ui/merchant-card.fxml"));Parent node=l.load();l.<MerchantMenuController>getController().configureCard(m,this);merchantList.getChildren().add(node);}catch(IOException ignored){}}
    public void openMerchant(Merchant merchant){selectedMerchant=merchant;statusLabel.setText("Cargando productos…");executor.submit(()->{try{List<MerchantProduct> products=sessionProducts.get(merchant.id);if(products==null){products=new MerchantRepository(new SupabaseService(AppConfig.load())).listProducts(merchant.id);sessionProducts.put(merchant.id,List.copyOf(products));}List<MerchantProduct> ready=products;Platform.runLater(()->{if(!current())return;try{FXMLLoader l=new FXMLLoader(getClass().getResource("/com/example/proyectolp/ui/merchant-menu.fxml"));Parent node=l.load();l.<MerchantMenuController>getController().configureMenu(merchant,ready,balance,this);menuHost.getChildren().setAll(node);show(menuPane);statusLabel.setText("");}catch(IOException e){showMessage("No se pudo abrir el menú.");}});}catch(Exception e){Platform.runLater(()->{if(current())showMessage("No se pudo cargar el menú del restaurante.");});}});}
    public void redeem(Merchant merchant,MerchantProduct product){if(!current())return;String request=requestIds.computeIfAbsent(product.id,k->UUID.randomUUID().toString());try{FXMLLoader l=new FXMLLoader(getClass().getResource("/com/example/proyectolp/ui/confirm-redemption.fxml"));Parent content=l.load();ConfirmRedemptionController dialogController=l.getController();dialogController.configure(merchant.name,product.name,product.creditCost,balance);Dialog<Void> dialog=new Dialog<>();dialog.getDialogPane().setContent(content);dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);dialog.getDialogPane().lookupButton(ButtonType.CLOSE).setVisible(false);dialog.getDialogPane().getStylesheets().add(getClass().getResource("/com/example/proyectolp/css/theme.css").toExternalForm());dialog.setHeaderText(null);dialog.showAndWait();if(!dialogController.isConfirmed()){openMerchant(merchant);return;}if(canjeEnCurso){openMerchant(merchant);return;}canjeEnCurso=true;statusLabel.setText("Procesando canje…");executor.submit(()->{try{RedemptionResult result=new RedemptionService(new SupabaseService(AppConfig.load())).redeem(product.id,product.creditCost,balance,request);Platform.runLater(()->{canjeEnCurso=false;if(!current())return;requestIds.remove(product.id);balance=result.balanceAfter;balanceLabel.setText(balance+" CRÉDITOS");copLabel.setText("≈ $"+String.format(Locale.forLanguageTag("es-CO"),"%,d",balance*CreditRules.COP_PER_CREDIT)+" COP");statusLabel.setText("");showCoupon(result);NotificationService.getInstance().show("−"+result.creditsSpent+" créditos por canje en "+result.merchantName);refreshCallback.run();refresh();if(selectedMerchant!=null)openMerchant(selectedMerchant);});}catch(Exception e){Platform.runLater(()->{canjeEnCurso=false;if(current()){String message=e instanceof RedemptionService.RedemptionException?e.getMessage():"No se pudo completar el canje. Inténtalo de nuevo.";openMerchant(merchant);showMessage(message);}});}});}catch(IOException e){openMerchant(merchant);showMessage("No se pudo abrir la confirmación.");}}
    private void showCoupon(RedemptionResult result){try{FXMLLoader l=new FXMLLoader(getClass().getResource("/com/example/proyectolp/ui/coupon-dialog.fxml"));Parent content=l.load();l.<CouponDialogController>getController().configure(result);Dialog<Void>d=new Dialog<>();d.getDialogPane().setContent(content);d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);d.getDialogPane().lookupButton(ButtonType.CLOSE).setVisible(false);d.getDialogPane().getStylesheets().add(getClass().getResource("/com/example/proyectolp/css/theme.css").toExternalForm());d.showAndWait();}catch(IOException e){showMessage("Canje realizado. Código: "+result.couponCode);}}
    @FXML private void showHistory(){historyList.getChildren().clear();DateTimeFormatter f=DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");for(Redemption r:history){Label label=new Label(r.createdAt.format(f)+" · "+r.merchantName+" · "+r.productName+" · −"+r.creditsSpent+" créditos · "+r.couponCode);label.getStyleClass().add("card");historyList.getChildren().add(label);}show(historyPane);}
    @FXML public void showMerchants(){show(merchantPane);}
    private void show(ScrollPane selected){for(ScrollPane pane:List.of(merchantPane,menuPane,historyPane)){boolean active=pane==selected;pane.setVisible(active);pane.setManaged(active);}}
    public void showMessage(String message){statusLabel.setText(message);}
}
