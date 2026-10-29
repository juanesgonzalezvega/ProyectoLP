package com.example.proyectolp.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Verifica el encabezado binario para rechazar archivos renombrados o incompletos. */
public final class FileSignatureChecker {
    private FileSignatureChecker(){}
    public static void validate(Path file,String extension)throws IOException{
        String ext=extension.toLowerCase(java.util.Locale.ROOT);AttachmentRules.kindForExtension(ext);byte[] b=new byte[(int)Math.min(32,Files.size(file))];try(var in=Files.newInputStream(file)){int n=in.read(b);if(n<0)n=0;if(n<b.length)b=java.util.Arrays.copyOf(b,n);}
        boolean valid=switch(ext){case "png"->starts(b,0x89,0x50,0x4e,0x47,0x0d,0x0a,0x1a,0x0a);case "jpg","jpeg"->starts(b,0xff,0xd8,0xff);case "gif"->ascii(b,0,"GIF8");case "bmp"->ascii(b,0,"BM");case "wav"->ascii(b,0,"RIFF")&&ascii(b,8,"WAVE");case "aif","aiff"->ascii(b,0,"FORM")&&ascii(b,8,"AIFF");case "mp3"->ascii(b,0,"ID3")||b.length>=2&&(b[0]&0xff)==0xff&&(b[1]&0xe0)==0xe0;case "mp4","m4v","m4a"->b.length>=12&&ascii(b,4,"ftyp");default->false;};
        if(!valid)throw new IllegalArgumentException("El contenido no coincide con el formato ."+ext+" o el archivo está truncado.");
    }
    private static boolean starts(byte[] data,int... signature){if(data.length<signature.length)return false;for(int i=0;i<signature.length;i++)if((data[i]&0xff)!=signature[i])return false;return true;}
    private static boolean ascii(byte[] data,int offset,String value){if(data.length<offset+value.length())return false;for(int i=0;i<value.length();i++)if(data[offset+i]!=(byte)value.charAt(i))return false;return true;}
}
