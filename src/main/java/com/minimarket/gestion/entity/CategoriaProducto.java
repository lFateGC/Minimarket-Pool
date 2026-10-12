package com.minimarket.gestion.entity;

import jakarta.persistence.*;
import lombok.Builder;

@Entity
@Table(name = "categorias_producto")
@Builder
public class CategoriaProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    public CategoriaProducto() {
    }

    public CategoriaProducto(String nombre) {
        this.nombre = nombre;
    }

    public CategoriaProducto(Long id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}
