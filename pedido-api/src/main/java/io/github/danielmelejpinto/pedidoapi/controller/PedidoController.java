package io.github.danielmelejpinto.pedidoapi.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;


@RestController 
public class PedidoController {
   
    @GetMapping("/api/pedido/estado")
    public String estado() {
        return "API de pedido funcionando";
    }
    
    
}
