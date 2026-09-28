package io.github.danielmelejpinto.pedidoapi.service;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import io.github.danielmelejpinto.pedidoapi.model.Pedido;
import io.github.danielmelejpinto.pedidoapi.repository.PedidoRepository;
import java.util.List;

@Service
public class PedidoService {

    private final PedidoRepository repository;

    public PedidoService(PedidoRepository repository) {
        this.repository = repository;
    }

    public Pedido crearPedido(Pedido pedido) {
        return repository.save(pedido);
    }

    public List<Pedido> obtenerTodos() {
        return repository.findAll();
    }

    public Pedido obtenerPorId(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El pedido no existe"));
    }

    public Pedido actualizarPedido(Long id, Pedido pedidoActualizado) {
        Pedido pedidoExistente = obtenerPorId(id);
        
        pedidoExistente.setName(pedidoActualizado.getName());
        pedidoExistente.setPrecio(pedidoActualizado.getPrecio());
        
        return repository.save(pedidoExistente); 
    }

    public void eliminarPedido(Long id) {
        repository.deleteById(id);
    }
}