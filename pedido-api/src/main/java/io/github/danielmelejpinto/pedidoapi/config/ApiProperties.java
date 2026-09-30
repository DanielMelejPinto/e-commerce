package io.github.danielmelejpinto.pedidoapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "api")
public class ApiProperties {
    
    private Producto producto = new Producto();
    private Inventario inventario = new Inventario();
    private Usuario usuario = new Usuario();

    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }

    public Inventario getInventario() { return inventario; }
    public void setInventario(Inventario inventario) { this.inventario = inventario; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public static class Producto {
        private String url;
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }

    public static class Inventario {
        private String url;
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }

    public static class Usuario {
        private String url;
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }
}
