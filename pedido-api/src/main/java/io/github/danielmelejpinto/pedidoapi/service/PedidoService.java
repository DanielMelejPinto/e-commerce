package io.github.danielmelejpinto.pedidoapi.service;

import org.springframework.stereotype.Service;

import io.github.danielmelejpinto.pedidoapi.model.Pedido;
import io.github.danielmelejpinto.pedidoapi.repository.PedidoRepository;
import java.util.List;

@Service
public class PedidoService {

    private final PedidoRepository repository;

    PedidoService(PedidoRepository repository) {
        this.repository = repository;
    }

    // Método para guardar un pedido nuevo
    public Pedido crearPedido(Pedido pedido) {
        // Aquí en el futuro puedes poner reglas de negocio
        return repository.save(pedido);
    }

    // Método para obtener todos los pedidos
    public List<Pedido> obtenerTodos() {
        return repository.findAll();
    }
}