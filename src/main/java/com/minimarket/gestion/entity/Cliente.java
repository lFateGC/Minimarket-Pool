package com.minimarket.gestion.entity;

import jakarta.persistence.*;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Table(name = "clientes")
@Builder
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo_documento", nullable = false, length = 10)
    private String tipoDocumento; // DNI, RUC, CE

    @Column(name = "numero_documento", nullable = false, unique = true, length = 20)
    private String numeroDocumento;

    @Column(nullable = false, length = 120)
    private String nombres;

    @Column(length = 120)
    private String apellidos;

    @Column(length = 150)
    private String email;

    @Column(length = 25)
    private String telefono;

    @Column(length = 255)
    private String direccion;

    @Column(nullable = false)
    private Boolean activo;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    public Cliente() {
        this.tipoDocumento = "DNI";
        this.activo = true;
    }

    public Cliente(Long id, String tipoDocumento, String numeroDocumento, String nombres, String apellidos,
                   String email, String telefono, String direccion, Boolean activo, LocalDateTime fechaRegistro,
                   Usuario usuario) {
        this.id = id;
        this.tipoDocumento = tipoDocumento != null ? tipoDocumento : "DNI";
        this.numeroDocumento = numeroDocumento;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.email = email;
        this.telefono = telefono;
        this.direccion = direccion;
        this.activo = activo != null ? activo : true;
        this.fechaRegistro = fechaRegistro != null ? fechaRegistro : LocalDateTime.now();
        this.usuario = usuario;
    }

    @PrePersist
    public void prePersist() {
        if (this.fechaRegistro == null) {
            this.fechaRegistro = LocalDateTime.now();
        }
        if (this.activo == null) {
            this.activo = true;
        }
        if (this.tipoDocumento == null) {
            this.tipoDocumento = "DNI";
        }
    }

    public String getNombreCompleto() {
        if (apellidos != null && !apellidos.isBlank()) {
            return nombres + " " + apellidos;
        }
        return nombres;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }
}
