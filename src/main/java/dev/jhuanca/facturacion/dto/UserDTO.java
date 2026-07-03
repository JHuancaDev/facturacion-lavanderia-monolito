package dev.jhuanca.facturacion.dto;

import lombok.Data;

@Data
public class UserDTO {
    private Long id;
    private String username;
    private String password;
    private String nombre;
    private String email;
    private String rol;
    private Boolean activo;
}