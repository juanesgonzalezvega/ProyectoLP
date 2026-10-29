package com.example.proyectolp.repository;

import com.example.proyectolp.model.Attachment;
import com.example.proyectolp.model.enums.AttachmentOwnerType;
import com.example.proyectolp.service.SupabaseService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AttachmentRepository {
    private final SupabaseService db;
    public AttachmentRepository(SupabaseService db){this.db=db;}
    public Attachment insert(Attachment item)throws Exception{
        Map<String,Object> body=new LinkedHashMap<>();body.put("task_id",item.getTaskId());body.put("project_id",item.getProjectId());body.put("session_id",item.getSessionId());body.put("file_name",item.getFileName());body.put("mime_type",item.getMimeType());body.put("kind",item.getKind().name());body.put("size_bytes",item.getSizeBytes());body.put("storage_path",item.getStoragePath());
        List<Attachment> rows=db.postList("attachments",body,"return=representation",Attachment.class);return rows.isEmpty()?item:rows.get(0);
    }
    public List<Attachment> list(AttachmentOwnerType type,long ownerId)throws Exception{return db.getList(query(type,ownerId,"select=*&order=created_at.asc"),Attachment.class);}
    public int count(AttachmentOwnerType type,long ownerId)throws Exception{return db.getList(query(type,ownerId,"select=id"),Attachment.class).size();}
    public List<String> paths(AttachmentOwnerType type,long ownerId)throws Exception{return db.getList(query(type,ownerId,"select=storage_path"),Attachment.class).stream().map(Attachment::getStoragePath).toList();}
    public void delete(long attachmentId)throws Exception{db.delete("attachments?id=eq."+attachmentId);}
    public void deleteByOwner(AttachmentOwnerType type,long ownerId)throws Exception{db.delete("attachments?"+column(type)+"=eq."+ownerId);}
    private static String query(AttachmentOwnerType type,long id,String select){return "attachments?"+select+"&"+column(type)+"=eq."+id;}
    private static String column(AttachmentOwnerType type){return switch(type){case TASK->"task_id";case PROJECT->"project_id";case SESSION->"session_id";};}
}
