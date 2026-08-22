package com.upn.gestion_clinica.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
    @GetMapping("/")
    public String hello() {
        return "¡Hola! La aplicación de Gestión Clínica está funcionando correctamente.";
    }
}
