package com.example.proyectolp.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Archivos descargados privados en un directorio temporal propio de la sesión. */
public final class AttachmentCache {
    private static final class Holder { private static final AttachmentCache INSTANCE=create(); private static AttachmentCache create(){try{return new AttachmentCache();}catch(IOException e){throw new ExceptionInInitializerError(e);}} }
    @FunctionalInterface public interface Downloader{void download(Path target)throws Exception;}
    private final Path parent;
    private Path directory;
    private final Map<String,Path> entries=new HashMap<>();
    public AttachmentCache()throws IOException{this(Path.of(System.getProperty("java.io.tmpdir")));}
    public static AttachmentCache getInstance(){return Holder.INSTANCE;}
    public AttachmentCache(Path parent)throws IOException{this.parent=parent;Files.createDirectories(parent);this.directory=Files.createTempDirectory(parent,"session-");}
    public synchronized Path getOrDownload(String storagePath,String extension,Downloader downloader)throws Exception{
        Path cached=entries.get(storagePath);if(cached!=null&&Files.isRegularFile(cached))return cached;
        String ext=extension==null||extension.isBlank()?"bin":"."+extension.toLowerCase(java.util.Locale.ROOT);Path target=directory.resolve(UUID.randomUUID()+ext);
        try{downloader.download(target);entries.put(storagePath,target);return target;}catch(Exception ex){Files.deleteIfExists(target);throw ex;}
    }
    public synchronized Path directory(){return directory;}
    public synchronized void clear()throws IOException{deleteTree(directory);entries.clear();directory=Files.createTempDirectory(parent,"session-");}
    public synchronized void dispose()throws IOException{deleteTree(directory);entries.clear();}
    private static void deleteTree(Path root)throws IOException{if(!Files.exists(root))return;try(var paths=Files.walk(root)){for(Path path:paths.sorted(java.util.Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}}
}
