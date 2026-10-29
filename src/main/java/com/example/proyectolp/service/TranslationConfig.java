package com.example.proyectolp.service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class TranslationConfig {
    private TranslationConfig(){}
    public static String email(){Properties p=new Properties();Path file=Path.of("config.properties");try{if(Files.isRegularFile(file)){try(InputStream in=Files.newInputStream(file)){p.load(in);}}else try(InputStream in=TranslationConfig.class.getClassLoader().getResourceAsStream("config.properties")){if(in!=null)p.load(in);}}catch(Exception ignored){}return p.getProperty("mymemory.email","").trim();}
}
