import api from '../api/axios';
import type { Pedido } from '../types';

export interface PedidoItemRequest {
  productoId: number;
  cantidad: number;
}

export interface PedidoRequest {
  items: PedidoItemRequest[];
}

export const pedidoService = {
  crearPedido: async (data: PedidoRequest): Promise<Pedido> => {
    // Add Idempotency-Key
    const idempotencyKey = crypto.randomUUID();
    const response = await api.post('/api/pedidos', data, {
      headers: {
        'Idempotency-Key': idempotencyKey
      }
    });
    return response.data;
  },

  obtenerMisPedidos: async (): Promise<Pedido[]> => {
    const response = await api.get('/api/pedidos/mis-pedidos');
    return response.data;
  },

  cancelarPedido: async (id: number): Promise<Pedido> => {
    const response = await api.post(`/api/pedidos/${id}/cancelar`);
    return response.data;
  }
};

