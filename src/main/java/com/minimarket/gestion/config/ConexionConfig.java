package com.minimarket.gestion.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:conexion.properties")
@Getter
@Setter
public class ConexionConfig {

    @Value("${spring.datasource.url}")
    private String databaseUrl;

    @Value("${spring.datasource.username}")
    private String databaseUsername;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Value("${supabase.url:https://your-project.supabase.co}")
    private String supabaseUrl;

    @Value("${supabase.anon-key:your-anon-key}")
    private String supabaseAnonKey;

    @Value("${supabase.bucket-name:productos-minimarket}")
    private String supabaseBucketName;

    @Value("${supabase.public-url-base:https://your-project.supabase.co/storage/v1/object/public/productos-minimarket/}")
    private String supabasePublicUrlBase;
}
