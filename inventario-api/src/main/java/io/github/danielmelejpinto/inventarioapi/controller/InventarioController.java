package io.github.danielmelejpinto.inventarioapi.controller;

import org.springframework.web.bind.annotation.RestController;

import io.github.danielmelejpinto.inventarioapi.service.InventarioService;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.RequestMapping;


@RestController 
@RequestMapping("/api/inventarios")
@Tag(name = "Inventarios", description = "Operaciones CRUD sobre el inventario")
public class InventarioController {
    
    private final InventarioService service;

    
}
