package io.github.danielmelejpinto.pedidoapi.service;

import io.github.danielmelejpinto.pedidoapi.client.InventarioClient;
import io.github.danielmelejpinto.pedidoapi.client.ProductoClient;
import io.github.danielmelejpinto.pedidoapi.client.dto.ProductoDTO;
import io.github.danielmelejpinto.pedidoapi.dto.PedidoRequest;
import io.github.danielmelejpinto.pedidoapi.model.EstadoPedido;
import io.github.danielmelejpinto.pedidoapi.model.Pedido;
import io.github.danielmelejpinto.pedidoapi.model.PedidoItem;
import io.github.danielmelejpinto.pedidoapi.repository.PedidoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.List;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

@Service
public class PedidoService {

    private static final Logger log = LoggerFactory.getLogger(PedidoService.class);

    private final PedidoRepository pedidoRepository;
    private final ProductoClient productoClient;
    private final InventarioClient inventarioClient;
    private final io.github.danielmelejpinto.pedidoapi.repository.ClaveIdempotenciaRepository claveIdempotenciaRepository;
    private final ApplicationContext context;

    public PedidoService(PedidoRepository pedidoRepository, ProductoClient productoClient,
            InventarioClient inventarioClient, io.github.danielmelejpinto.pedidoapi.repository.ClaveIdempotenciaRepository claveIdempotenciaRepository, ApplicationContext context) {
        this.pedidoRepository = pedidoRepository;
        this.productoClient = productoClient;
        this.inventarioClient = inventarioClient;
        this.claveIdempotenciaRepository = claveIdempotenciaRepository;
        this.context = context;
    }

    private String generarHash(PedidoRequest request) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            StringBuilder sb = new StringBuilder();
            for (var item : request.items()) {
                sb.append(item.productoId()).append(":").append(item.cantidad()).append(";");
            }
            byte[] encodedhash = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (int i = 0; i < encodedhash.length; i++) {
                String hex = Integer.toHexString(0xff & encodedhash[i]);
                if(hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Pedido iniciarPedidoAtomico(Long usuarioId, PedidoRequest request, String idempotencyKey, String hash) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            if (idempotencyKey.length() > 255) {
                throw new IllegalArgumentException("La longitud de la clave de idempotencia no puede exceder los 255 caracteres");
            }
            java.util.Optional<io.github.danielmelejpinto.pedidoapi.model.ClaveIdempotencia> existente = 
                claveIdempotenciaRepository.findById(new io.github.danielmelejpinto.pedidoapi.model.ClaveIdempotenciaId(usuarioId, idempotencyKey));
            if (existente.isPresent()) {
                var clave = existente.get();
                if (!clave.getHashContenido().equals(hash)) {
                    throw new io.github.danielmelejpinto.pedidoapi.exception.EstadoPedidoInvalidoException("La clave de idempotencia ya fue utilizada con un contenido diferente");
                }
                if ("EN_PROGRESO".equals(clave.getEstado()) || "REINTENTANDO".equals(clave.getEstado())) {
                    // Si está en progreso y han pasado más de 2 minutos, lo consideramos fallido y lo reintentamos.
                    if (clave.getFechaCreacion().isBefore(java.time.LocalDateTime.now().minusMinutes(2))) {
                        clave.setEstado("REINTENTANDO");
                        clave.setFechaCreacion(java.time.LocalDateTime.now()); // Update timestamp to prevent immediate retries
                        claveIdempotenciaRepository.save(clave);
                        // Limpiamos los items del pedido para empezar de cero (las reservas en inventario son idempotentes)
                        Pedido p = pedidoRepository.findById(clave.getPedidoId()).orElseThrow();
                        p.getItems().clear();
                        pedidoRepository.save(p);
                        return p;
                    }
                    throw new IllegalStateException("La operación ya está en progreso");
                }
                return pedidoRepository.findById(clave.getPedidoId())
                        .orElseThrow(() -> new IllegalStateException("Pedido referenciado por idempotencia no existe"));
            }
        }

        Pedido pedido = new Pedido();
        pedido.setUsuarioId(usuarioId);
        pedido.setEstado(EstadoPedido.PENDIENTE);
        pedido.setTotal(BigDecimal.ZERO);
        pedido = pedidoRepository.saveAndFlush(pedido);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            io.github.danielmelejpinto.pedidoapi.model.ClaveIdempotencia clave = new io.github.danielmelejpinto.pedidoapi.model.ClaveIdempotencia(usuarioId, idempotencyKey, pedido.getId(), hash, "EN_PROGRESO");
            claveIdempotenciaRepository.saveAndFlush(clave);
        }

        return pedido;
    }

    public Pedido crearPedido(Long usuarioId, PedidoRequest request, String idempotencyKey) {
        String hash = generarHash(request);

        Pedido pedido;
        try {
            pedido = context.getBean(PedidoService.class).iniciarPedidoAtomico(usuarioId, request, idempotencyKey, hash);
        } catch (DataIntegrityViolationException e) {
            // Concurrencia al crear la clave de idempotencia
            return crearPedido(usuarioId, request, idempotencyKey); // reintento
        }

        if (pedido.getEstado() == EstadoPedido.CONFIRMADO || pedido.getEstado() == EstadoPedido.CANCELADO) {
            log.info("Pedido idempotente recuperado para clave {}, estado {}", idempotencyKey, pedido.getEstado());
            return pedido;
        }

        if (pedido.getEstado() == EstadoPedido.PENDIENTE && !pedido.getItems().isEmpty()) {
            // Ya procesado o en progreso pero con items. En este sistema simple, si tiene items, asumiremos que se debe a otra petición en progreso.
            // O mejor: si está EN_PROGRESO y la fecha de creación es muy reciente, es que hay otra petición atendiendo.
            // Para simplicidad, si está PENDIENTE pero no es un pedido recién creado (items vacíos), retornamos conflicto o reintentamos.
            // Como lo acabamos de recuperar de la BD o crear, si está vacío fue creado por nosotros. Si tiene items, ya se procesó en otra transacción que falló, o está en ello.
            // Para ser robustos, intentamos reservar de nuevo.
        }

        BigDecimal total = BigDecimal.ZERO;
        boolean error = false;

        // Limpiamos items para reconstruir en caso de reintento de pedido PENDIENTE fallido
        // pedido.getItems().clear(); 

        try {
            for (var itemReq : request.items()) {
                ProductoDTO producto = productoClient.obtenerProducto(itemReq.productoId());
                if (!"ACTIVO".equals(producto.estado())) {
                    throw new IllegalStateException("El producto " + producto.id() + " no está ACTIVO");
                }

                inventarioClient.reservarStock(itemReq.productoId(), itemReq.cantidad(), pedido.getId());

                // Solo agregamos el item si no estaba antes (por idempotencia)
                boolean itemExiste = pedido.getItems().stream().anyMatch(i -> i.getProductoId().equals(itemReq.productoId()));
                if (!itemExiste) {
                    PedidoItem item = new PedidoItem();
                    item.setProductoId(itemReq.productoId());
                    item.setCantidad(itemReq.cantidad());
                    item.setPrecioUnitario(producto.precio());
                    total = total.add(producto.precio().multiply(new BigDecimal(itemReq.cantidad())));
                    pedido.addItem(item);
                } else {
                    total = total.add(producto.precio().multiply(new BigDecimal(itemReq.cantidad())));
                }
            }
        } catch (Exception e) {
            error = true;
            // No hacemos throw aquí, marcamos como error para confirmar el rollback o cancelarlo luego.
            // O si preferimos, dejamos que siga tirando y el frontend reintente. Pero tenemos que manejar la compensación.
            // Dado que las reservas ahora están atadas al `pedidoId`, podemos cancelar el pedido entero y liberar todo de una.
            // Pero "Confirma CANCELADO únicamente cuando la liberación correspondiente esté completada."
            // En caso de fallo durante creación, la saga compensa.
            for (var itemReq : request.items()) {
                try {
                    inventarioClient.liberarStock(itemReq.productoId(), itemReq.cantidad(), pedido.getId());
                } catch (Exception exCompensacion) {
                    log.error("Error al liberar stock de pedido {}", pedido.getId(), exCompensacion);
                }
            }
            throw e;
        }
        
        return finalizarPedido(pedido.getId(), total, idempotencyKey);
    }

    @Transactional
    public Pedido finalizarPedido(Long pedidoId, BigDecimal total, String idempotencyKey) {
        Pedido pedido = pedidoRepository.findById(pedidoId).orElseThrow();
        pedido.setTotal(total);
        pedido.setEstado(EstadoPedido.CONFIRMADO);
        pedido = pedidoRepository.save(pedido);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            java.util.Optional<io.github.danielmelejpinto.pedidoapi.model.ClaveIdempotencia> existente = 
                claveIdempotenciaRepository.findById(new io.github.danielmelejpinto.pedidoapi.model.ClaveIdempotenciaId(pedido.getUsuarioId(), idempotencyKey));
            if (existente.isPresent()) {
                var clave = existente.get();
                clave.setEstado("COMPLETADO");
                claveIdempotenciaRepository.save(clave);
            }
        }
        return pedido;
    }

    public List<Pedido> obtenerPedidosPorUsuario(Long usuarioId) {
        return pedidoRepository.findByUsuarioId(usuarioId);
    }

    public Pedido cancelarPedido(Long usuarioId, Long pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new io.github.danielmelejpinto.pedidoapi.exception.PedidoNoEncontradoException(pedidoId));

        if (!pedido.getUsuarioId().equals(usuarioId)) {
            throw new io.github.danielmelejpinto.pedidoapi.exception.PedidoNoEncontradoException(pedidoId);
        }

        if (pedido.getEstado() != EstadoPedido.CONFIRMADO) {
            throw new io.github.danielmelejpinto.pedidoapi.exception.EstadoPedidoInvalidoException(
                    "No se puede cancelar el pedido porque está en estado " + pedido.getEstado());
        }

        // Liberar el stock reservado
        for (PedidoItem item : pedido.getItems()) {
            try {
                inventarioClient.liberarStock(item.getProductoId(), item.getCantidad(), pedidoId);
            } catch (Exception e) {
                log.error("Fallo al liberar stock durante cancelación. Pedido: {}, Producto: {}", 
                          pedido.getId(), item.getProductoId(), e);
                throw new io.github.danielmelejpinto.pedidoapi.exception.ServicioDependienteException(
                        "No se pudo completar la cancelación por un error en inventario-api");
            }
        }

        return confirmarCancelacion(pedidoId, usuarioId);
    }

    @Transactional
    public Pedido confirmarCancelacion(Long pedidoId, Long usuarioId) {
        Pedido pedido = pedidoRepository.findById(pedidoId).orElseThrow();
        int filas = pedidoRepository.cancelarSiConfirmado(pedidoId, usuarioId);
        if (filas == 0) {
            throw new io.github.danielmelejpinto.pedidoapi.exception.EstadoPedidoInvalidoException(
                    "No se puede cancelar el pedido porque ya no está en estado CONFIRMADO");
        }
        pedido.setEstado(EstadoPedido.CANCELADO);
        return pedido;
    }
}
