package com.example.proyectolp.service;

import java.io.InputStream;
import java.util.Properties;

public final class TrmConfig {
    public static final String URL="https://www.datos.gov.co/resource/32sa-8pi3.json";
    private TrmConfig() { }
    public static String appToken() {
        Properties p=new Properties();
        try(InputStream in=TrmConfig.class.getClassLoader().getResourceAsStream("config.properties")){if(in!=null)p.load(in);}catch(Exception ignored){}
        return p.getProperty("trm.app.token", "").trim();
    }
}
