export interface Producto {
  id: number;
  nombre: string;
  descripcion: string;
  precio: number;
  imagenUrl: string;
}

export interface Inventario {
  productoId: number;
  cantidadDisponible: number;
  cantidadReservada: number;
}

export interface Usuario {
  id: number;
  nombre: string;
  email: string;
  rol: string;
}

export interface PedidoItem {
  productoId: number;
  cantidad: number;
  precioUnitario: number;
}

export interface Pedido {
  id: number;
  usuarioId: number;
  fechaCreacion: string;
  estado: 'PENDIENTE' | 'CONFIRMADO' | 'CANCELADO' | 'ERROR';
  total: number;
  items: PedidoItem[];
}
