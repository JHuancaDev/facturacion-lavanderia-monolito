/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dev.jhuanca.facturacion.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 *
 * @author javier
 */
@Controller
public class AuthC {

    @GetMapping("/login")
    public String login(Model model) {
        // No usar layout/base para login
        return "auth/login"; // ← Cambiar a la página de login simple
    }

}
