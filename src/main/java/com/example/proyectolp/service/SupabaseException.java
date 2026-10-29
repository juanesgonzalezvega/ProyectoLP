package com.example.proyectolp.service;

public class SupabaseException extends Exception {
    private final int statusCode;
    private final String body;
    public SupabaseException(int statusCode,String body){this(statusCode,body,message(statusCode));}
    public SupabaseException(int statusCode,String body,String message){super(message);this.statusCode=statusCode;this.body=body;}
    public int getStatusCode(){return statusCode;}
    public String getBody(){return body;}
    private static String message(int status){return status==401?"La sesión expiró o no es válida. Inicia sesión de nuevo.":status==403?"Acceso denegado por las políticas de seguridad (RLS).":status==404?"No se encontró el recurso solicitado. Revisa el esquema y sus políticas.":status>=500?"Supabase no está disponible en este momento.":"Supabase rechazó la operación (HTTP "+status+"). Revisa permisos y datos.";}
}
