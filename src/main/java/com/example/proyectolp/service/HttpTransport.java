package com.example.proyectolp.service;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

/** Transporte mínimo intercambiable para pruebas sin red. */
public interface HttpTransport {
    record Response(int statusCode, String body) {}
    Response send(String method, URI uri, Map<String, String> headers, String body) throws IOException, InterruptedException;
}
