package com.example.proyectolp.service;

import com.example.proyectolp.model.Attachment;
import com.example.proyectolp.model.Session;
import com.example.proyectolp.model.enums.AttachmentKind;
import com.example.proyectolp.model.enums.AttachmentOwnerType;
import com.example.proyectolp.repository.AttachmentRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/** Flujo único de validación, subida, metadatos, descarga y borrado de adjuntos. */
public class AttachmentService {
    private final SupabaseService db;private final AttachmentRepository repository;private final AttachmentCache cache;private final String userId;
    public AttachmentService(SupabaseService db){this(db,new AttachmentRepository(db),AttachmentCache.getInstance(),currentUserId());}
    public AttachmentService(SupabaseService db,AttachmentRepository repository,AttachmentCache cache,String userId){this.db=db;this.repository=repository;this.cache=cache;this.userId=userId;}
    private static String currentUserId(){Session session=SessionManager.getInstance().current();return session==null?null:session.userId();}
    public List<Attachment> list(AttachmentOwnerType owner,long id)throws Exception{return repository.list(owner,id);}
    public synchronized Attachment upload(Path file,AttachmentOwnerType owner,long ownerId)throws Exception{
        int count=repository.count(owner,ownerId);AttachmentRules.validateCount(count);String extension=AttachmentRules.extension(file);AttachmentKind kind=AttachmentRules.kindForExtension(extension);long size=Files.size(file);AttachmentRules.validateSize(kind,size);FileSignatureChecker.validate(file,extension);
        if(userId==null)throw new IllegalStateException("Inicia sesión para adjuntar archivos.");String path=AttachmentRules.storagePath(userId,owner,ownerId,UUID.randomUUID(),extension);String mime=AttachmentRules.mimeType(extension);String name=AttachmentRules.sanitizeName(file.getFileName().toString());
        Long taskId=owner==AttachmentOwnerType.TASK?ownerId:null,projectId=owner==AttachmentOwnerType.PROJECT?ownerId:null,sessionId=owner==AttachmentOwnerType.SESSION?ownerId:null;
        Attachment metadata=new Attachment(taskId,projectId,sessionId,name,mime,kind,size,path);
        db.storageUpload(path,file,mime);
        try{return repository.insert(metadata);}catch(Exception insertFailure){try{db.storageDelete(path);}catch(Exception cleanupFailure){insertFailure.addSuppressed(cleanupFailure);throw new IllegalStateException("No se guardaron los datos del adjunto y no se pudo retirar el archivo temporal del bucket.",insertFailure);}throw new IllegalStateException("No se pudieron guardar los datos del adjunto; se retiró el archivo subido.",insertFailure);}
    }
    public void delete(Attachment item)throws Exception{try{db.storageDelete(item.getStoragePath());}catch(Exception e){throw new IllegalStateException("No se pudo borrar el archivo del bucket; el adjunto permanece en la lista.",e);}repository.delete(item.getId());}
    public Path download(Attachment item)throws Exception{return cache.getOrDownload(item.getStoragePath(),AttachmentRules.extension(Path.of(item.getFileName())),target->db.storageDownload(item.getStoragePath(),target));}
    public void deleteAllFor(AttachmentOwnerType owner,long id)throws Exception{List<String> paths=repository.paths(owner,id);if(paths.isEmpty())return;try{db.storageDeleteMany(paths);}catch(Exception e){throw new IllegalStateException("No se pudieron borrar todos los archivos adjuntos. No se eliminó el elemento.",e);}repository.deleteByOwner(owner,id);}
}
