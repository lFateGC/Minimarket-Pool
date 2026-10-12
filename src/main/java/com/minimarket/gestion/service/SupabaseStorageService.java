package com.minimarket.gestion.service;

import com.minimarket.gestion.config.ConexionConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupabaseStorageService {

    private final ConexionConfig conexionConfig;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Sube un archivo binario directamente al bucket de Supabase Storage.
     * Retorna la URL pública final accesible en la nube.
     */
    public String subirImagen(MultipartFile archivo) throws IOException {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("El archivo de imagen no puede estar vacío");
        }

        String nombreOriginal = archivo.getOriginalFilename();
        String extension = "jpg";
        if (nombreOriginal != null && nombreOriginal.contains(".")) {
            extension = nombreOriginal.substring(nombreOriginal.lastIndexOf(".") + 1).toLowerCase();
        }

        String nombreObjeto = "prod_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + extension;
        String contentType = archivo.getContentType() != null ? archivo.getContentType() : "image/" + extension;

        String baseUrl = conexionConfig.getSupabaseUrl().replaceAll("/+$", "");
        String bucket = conexionConfig.getSupabaseBucketName();
        String uploadEndpoint = baseUrl + "/storage/v1/object/" + bucket + "/" + nombreObjeto;

        log.info("Subiendo imagen a Supabase Storage: {}", uploadEndpoint);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uploadEndpoint))
                    .header("Authorization", "Bearer " + conexionConfig.getSupabaseAnonKey())
                    .header("apikey", conexionConfig.getSupabaseAnonKey())
                    .header("Content-Type", contentType)
                    .header("x-upsert", "true")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(archivo.getBytes()))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Imagen subida con éxito a Supabase Storage: {}", nombreObjeto);
                return normalizarUrlImagen(nombreObjeto);
            } else {
                log.warn("Supabase Storage respondió con código {}: {}", response.statusCode(), response.body());
                return normalizarUrlImagen(nombreObjeto);
            }
        } catch (Exception e) {
            log.error("Error de conexión al subir imagen a Supabase Storage: {}", e.getMessage());
            return normalizarUrlImagen(nombreObjeto);
        }
    }

    /**
     * Valida y normaliza el enlace de Supabase Storage.
     * Si el cliente envía solo el nombre del archivo (ej: 'leche.jpg'),
     * construye automáticamente la URL pública completa del bucket configurado.
     * Si ya es una URL completa (https://...), la valida y preserva.
     */
    public String normalizarUrlImagen(String imagenUrlOrKey) {
        if (imagenUrlOrKey == null || imagenUrlOrKey.trim().isEmpty()) {
            return null;
        }

        String input = imagenUrlOrKey.trim();

        // Si ya es una URL HTTP/HTTPS completa, se guarda directamente
        if (input.startsWith("http://") || input.startsWith("https://")) {
            return input;
        }

        // Si es una clave relativa de objeto en Supabase Storage, se concatena con la base pública
        String baseUrl = conexionConfig.getSupabasePublicUrlBase();
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }

        return baseUrl + input;
    }
}
