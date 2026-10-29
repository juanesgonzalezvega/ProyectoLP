package com.example.proyectolp.service;

import com.example.proyectolp.model.enums.AttachmentKind;
import com.example.proyectolp.model.enums.AttachmentOwnerType;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Validaciones compartidas por todos los adjuntos. */
public final class AttachmentRules {
    public static final long IMAGE_MAX_BYTES=10L*1024*1024;
    public static final long AUDIO_MAX_BYTES=25L*1024*1024;
    public static final long VIDEO_MAX_BYTES=45L*1024*1024;
    public static final long BUCKET_MAX_BYTES=50L*1024*1024;
    public static final int MAX_ATTACHMENTS=10;
    public static final String SUPPORTED_FORMATS="jpg, jpeg, png, gif, bmp, mp3, wav, aif, aiff, m4a, mp4, m4v";
    private static final Map<String,AttachmentKind> KINDS=Map.ofEntries(
            Map.entry("jpg",AttachmentKind.imagen),Map.entry("jpeg",AttachmentKind.imagen),Map.entry("png",AttachmentKind.imagen),Map.entry("gif",AttachmentKind.imagen),Map.entry("bmp",AttachmentKind.imagen),
            Map.entry("mp3",AttachmentKind.audio),Map.entry("wav",AttachmentKind.audio),Map.entry("aif",AttachmentKind.audio),Map.entry("aiff",AttachmentKind.audio),Map.entry("m4a",AttachmentKind.audio),Map.entry("mp4",AttachmentKind.video),Map.entry("m4v",AttachmentKind.video));
    private AttachmentRules(){}
    public static String extension(Path path){String name=path.getFileName().toString();int dot=name.lastIndexOf('.');return dot<0?"":name.substring(dot+1).toLowerCase(Locale.ROOT);}
    public static AttachmentKind kindForExtension(String extension){AttachmentKind kind=KINDS.get(extension==null?"":extension.toLowerCase(Locale.ROOT));if(kind==null)throw new IllegalArgumentException("Formato no admitido. Usa: "+SUPPORTED_FORMATS+".");return kind;}
    public static long maxBytes(AttachmentKind kind){return switch(kind){case imagen->IMAGE_MAX_BYTES;case audio->AUDIO_MAX_BYTES;case video->VIDEO_MAX_BYTES;};}
    public static String mimeType(String ext){return switch(ext.toLowerCase(Locale.ROOT)){case "jpg","jpeg"->"image/jpeg";case "png"->"image/png";case "gif"->"image/gif";case "bmp"->"image/bmp";case "mp3"->"audio/mpeg";case "wav"->"audio/wav";case "aif","aiff"->"audio/aiff";case "m4a"->"audio/mp4";case "mp4"->"video/mp4";case "m4v"->"video/x-m4v";default->throw new IllegalArgumentException("Formato no admitido. Usa: "+SUPPORTED_FORMATS+".");};}
    public static void validateSize(AttachmentKind kind,long size){if(size<=0)throw new IllegalArgumentException("El archivo está vacío o no se puede leer.");if(size>maxBytes(kind))throw new IllegalArgumentException("El archivo supera el máximo permitido para "+kind.name()+".");}
    public static void validateCount(int current){if(current>=MAX_ATTACHMENTS)throw new IllegalArgumentException("Cada elemento admite como máximo 10 adjuntos.");}
    public static String sanitizeName(String name){if(name==null)name="";String[] parts=name.replace('\\','/').split("/");String base=parts.length==0?"":parts[parts.length-1];StringBuilder clean=new StringBuilder();base.codePoints().filter(cp->!Character.isISOControl(cp)&&cp!='/'&&cp!='\\').limit(120).forEach(clean::appendCodePoint);String result=clean.toString().trim();return result.isEmpty()?"archivo":result;}
    public static String storagePath(String userId,AttachmentOwnerType type,long ownerId,UUID uuid,String extension){if(userId==null||!userId.matches("[0-9a-fA-F-]{36}"))throw new IllegalArgumentException("La sesión no contiene un identificador de usuario válido.");if(ownerId<=0)throw new IllegalArgumentException("El elemento todavía no se ha guardado.");String owner=switch(type){case TASK->"task";case PROJECT->"project";case SESSION->"session";};String ext=extension.toLowerCase(Locale.ROOT);kindForExtension(ext);return userId+"/"+owner+"/"+ownerId+"/"+uuid+"."+ext;}
}
