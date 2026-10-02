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
    const cartHash = JSON.stringify(data);
    let storedState = localStorage.getItem('checkout_state');
    let idempotencyKey = crypto.randomUUID();

    if (storedState) {
      try {
        const parsed = JSON.parse(storedState);
        if (parsed.cartHash === cartHash && parsed.idempotencyKey) {
          idempotencyKey = parsed.idempotencyKey;
        } else {
          // If cart changed, start a new logical attempt
          localStorage.setItem('checkout_state', JSON.stringify({ cartHash, idempotencyKey }));
        }
      } catch (e) {
        localStorage.setItem('checkout_state', JSON.stringify({ cartHash, idempotencyKey }));
      }
    } else {
      localStorage.setItem('checkout_state', JSON.stringify({ cartHash, idempotencyKey }));
    }

    try {
      const response = await api.post('/api/pedidos', data, {
        headers: {
          'Idempotency-Key': idempotencyKey
        }
      });
      // Clear on success
      localStorage.removeItem('checkout_state');
      return response.data;
    } catch (error) {
      // Keep it in localStorage for retries
      throw error;
    }
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
