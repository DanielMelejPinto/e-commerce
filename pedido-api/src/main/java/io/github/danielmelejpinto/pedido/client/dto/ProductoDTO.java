package io.github.danielmelejpinto.pedido.client.dto;

import java.math.BigDecimal;

public record ProductoDTO(Long id, String nombre, BigDecimal precio, String estado) {}
