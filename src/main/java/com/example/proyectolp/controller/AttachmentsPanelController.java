package com.example.proyectolp.controller;

import com.example.proyectolp.config.AppConfig;
import com.example.proyectolp.model.Attachment;
import com.example.proyectolp.model.enums.AttachmentOwnerType;
import com.example.proyectolp.service.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Duration;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class AttachmentsPanelController {
    @FXML private ListView<Attachment> attachmentList;
    @FXML private ImageView imagePreview;
    @FXML private MediaView mediaView;
    @FXML private ProgressIndicator busyIndicator;
    @FXML private Label messageLabel,previewStatus,mediaTime;
    @FXML private Button saveButton,deleteButton,uploadButton;
    @FXML private Slider mediaSlider;
    private AttachmentOwnerType ownerType;private long ownerId;private AttachmentService service;private long generation;private MediaPlayer player;private boolean changingSlider;private volatile boolean closed;

    @FXML private void initialize(){mediaView.setVisible(false);imagePreview.setVisible(false);attachmentList.setCellFactory(v->new ListCell<>(){@Override protected void updateItem(Attachment a,boolean empty){super.updateItem(a,empty);setText(empty||a==null?null:a.getKind()+" · "+a.getFileName()+" · "+humanSize(a.getSizeBytes())+" · "+a.getCreatedAt());}});attachmentList.getSelectionModel().selectedItemProperty().addListener((o,a,b)->{updateButtons();if(b!=null)preview(b);else clearPreview();});mediaSlider.valueProperty().addListener((o,a,b)->{if(player!=null&&!changingSlider&&mediaSlider.isValueChanging())player.seek(Duration.seconds(b.doubleValue()));});updateButtons();}
    public void configure(AttachmentOwnerType type,long id){ownerType=type;ownerId=id;generation=SessionManager.getInstance().generation();try{service=new AttachmentService(new SupabaseService(AppConfig.load()));loadList();}catch(Exception e){messageLabel.setText("No se pudieron preparar los adjuntos. Revisa la sesión y la configuración.");}}
    private void loadList(){runAsync(()->service.list(ownerType,ownerId),items->{attachmentList.getItems().setAll(items);messageLabel.setText(items.isEmpty()?"No hay adjuntos.":"");updateButtons();},"No se pudieron cargar los adjuntos.");}
    @FXML private void chooseFiles(){if(service==null)return;FileChooser chooser=new FileChooser();chooser.setTitle("Adjuntar archivos");chooser.getExtensionFilters().setAll(new FileChooser.ExtensionFilter("Imágenes","*.jpg","*.jpeg","*.png","*.gif","*.bmp"),new FileChooser.ExtensionFilter("Audio","*.mp3","*.wav","*.aif","*.aiff","*.m4a"),new FileChooser.ExtensionFilter("Video","*.mp4","*.m4v"),new FileChooser.ExtensionFilter("Formatos admitidos","*.jpg","*.jpeg","*.png","*.gif","*.bmp","*.mp3","*.wav","*.aif","*.aiff","*.m4a","*.mp4","*.m4v"));List<java.io.File> files=chooser.showOpenMultipleDialog(window());if(files==null||files.isEmpty())return;runAsync(()->{for(java.io.File file:files)service.upload(file.toPath(),ownerType,ownerId);return service.list(ownerType,ownerId);},items->{attachmentList.getItems().setAll(items);messageLabel.setText("Adjuntos guardados.");updateButtons();NotificationService.getInstance().show("Archivo adjuntado correctamente.");},"No se pudo adjuntar el archivo. Revisa el formato, tamaño y límite de 10 archivos.");}
    @FXML private void saveCopy(){Attachment item=attachmentList.getSelectionModel().getSelectedItem();if(item==null)return;FileChooser chooser=new FileChooser();chooser.setTitle("Guardar copia");chooser.setInitialFileName(item.getFileName());java.io.File target=chooser.showSaveDialog(window());if(target==null)return;runAsync(()->{Path cached=service.download(item);Files.copy(cached,target.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);return target.toPath();},path->messageLabel.setText("Copia guardada en "+path),"No se pudo descargar el adjunto.");}
    @FXML private void deleteSelected(){Attachment item=attachmentList.getSelectionModel().getSelectedItem();if(item==null)return;Alert confirm=new Alert(Alert.AlertType.CONFIRMATION,"¿Eliminar «"+item.getFileName()+"»?",ButtonType.CANCEL,ButtonType.OK);confirm.initOwner(window());if(confirm.showAndWait().orElse(ButtonType.CANCEL)!=ButtonType.OK)return;runAsync(()->{service.delete(item);return service.list(ownerType,ownerId);},items->{attachmentList.getItems().setAll(items);clearPreview();messageLabel.setText("Adjunto eliminado.");updateButtons();},"No se pudo eliminar el adjunto.");}
    private void preview(Attachment item){disposePlayer();imagePreview.setImage(null);mediaView.setMediaPlayer(null);mediaSlider.setDisable(true);previewStatus.setText("Cargando…");runAsync(()->service.download(item),path->{if(item.getKind()==com.example.proyectolp.model.enums.AttachmentKind.imagen){Image image=new Image(path.toUri().toString(),true);image.errorProperty().addListener((o,a,b)->{if(b)previewStatus.setText("No se pudo mostrar la imagen; guarda una copia para verla.");});imagePreview.setImage(image);mediaView.setVisible(false);imagePreview.setVisible(true);previewStatus.setText("");}else{imagePreview.setVisible(false);mediaView.setVisible(true);try{Media media=new Media(path.toUri().toString());player=new MediaPlayer(media);mediaView.setMediaPlayer(player);player.setOnReady(()->{mediaSlider.setMax(Math.max(1,player.getTotalDuration().toSeconds()));mediaSlider.setDisable(false);updateMediaTime();previewStatus.setText("");});player.currentTimeProperty().addListener((o,a,b)->{if(!mediaSlider.isValueChanging()){changingSlider=true;mediaSlider.setValue(b.toSeconds());changingSlider=false;}updateMediaTime();});player.setOnError(this::unplayable);media.setOnError(this::unplayable);}catch(Exception ex){unplayable();}}},"No se pudo cargar la vista previa.");}
    @FXML private void playMedia(){if(player!=null)player.play();} @FXML private void pauseMedia(){if(player!=null)player.pause();} @FXML private void stopMedia(){if(player!=null){player.stop();player.seek(Duration.ZERO);}}
    private void updateMediaTime(){if(player==null)return;mediaTime.setText(format(player.getCurrentTime())+" / "+format(player.getTotalDuration()));}
    private static String format(Duration duration){if(duration==null||duration.isUnknown()||duration.isIndefinite())return "00:00";long seconds=Math.max(0,(long)duration.toSeconds());return String.format(java.util.Locale.ROOT,"%02d:%02d",seconds/60,seconds%60);}
    private void unplayable(){previewStatus.setText("No se pudo reproducir en la app; guarda una copia para verlo");disposePlayer();}
    private void clearPreview(){disposePlayer();imagePreview.setImage(null);previewStatus.setText("Selecciona un adjunto para previsualizar.");mediaTime.setText("00:00 / 00:00");}
    private void disposePlayer(){if(player!=null){player.stop();player.dispose();player=null;}mediaView.setMediaPlayer(null);}
    public void close(){closed=true;disposePlayer();}
    private void updateButtons(){boolean selected=attachmentList!=null&&attachmentList.getSelectionModel().getSelectedItem()!=null;if(saveButton!=null)saveButton.setDisable(!selected);if(deleteButton!=null)deleteButton.setDisable(!selected);}
    private Window window(){return attachmentList.getScene()==null?null:attachmentList.getScene().getWindow();}
    private <T> void runAsync(Work<T> work,java.util.function.Consumer<T> success,String error){busyIndicator.setVisible(true);busyIndicator.setManaged(true);if(uploadButton!=null)uploadButton.setDisable(true);try{SessionTaskExecutor.get().submit(()->{try{T result=work.run();Platform.runLater(()->{if(closed||!SessionManager.getInstance().isCurrent(generation))return;success.accept(result);busyIndicator.setVisible(false);busyIndicator.setManaged(false);uploadButton.setDisable(false);});}catch(Exception ex){Platform.runLater(()->{if(closed||!SessionManager.getInstance().isCurrent(generation))return;messageLabel.setText(readable(ex,error));busyIndicator.setVisible(false);busyIndicator.setManaged(false);uploadButton.setDisable(false);});}});}catch(RuntimeException ex){busyIndicator.setVisible(false);busyIndicator.setManaged(false);uploadButton.setDisable(false);messageLabel.setText(error);}}
    @FunctionalInterface private interface Work<T>{T run()throws Exception;}
    private static String readable(Exception e,String fallback){if(e instanceof IllegalArgumentException)return e.getMessage();if(e instanceof SupabaseException s&&s.getStatusCode()==413)return "El archivo supera el tamaño permitido.";if(e instanceof java.net.http.HttpTimeoutException)return "La transferencia tardó demasiado.";return fallback;}
    private static String humanSize(long bytes){if(bytes<1024)return bytes+" B";if(bytes<1024*1024)return String.format(java.util.Locale.ROOT,"%.1f KB",bytes/1024.0);return String.format(java.util.Locale.ROOT,"%.1f MB",bytes/1048576.0);}
}
