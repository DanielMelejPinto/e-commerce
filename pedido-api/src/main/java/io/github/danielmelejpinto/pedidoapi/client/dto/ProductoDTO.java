package io.github.danielmelejpinto.pedidoapi.client.dto;

import java.math.BigDecimal;

public record ProductoDTO(Long id, String nombre, BigDecimal precio, String estado) {}
