import api from '../api/axios';
import type { Pedido } from '../types';

export const pedidoService = {
  crearPedido: async (payload: { items: Array<{ productoId: number; cantidad: number }> }): Promise<Pedido> => {
    let idempotencyKey = '';
    const cartHash = JSON.stringify(payload.items);
    
    // Check if we have an ongoing checkout state
    const storedState = localStorage.getItem('checkout_state');
    if (storedState) {
      try {
        const parsed = JSON.parse(storedState);
        if (parsed.cartHash === cartHash && parsed.idempotencyKey) {
          idempotencyKey = parsed.idempotencyKey;
        }
      } catch {
        // ignore parsing error
      }
    }
    
    if (!idempotencyKey) {
      idempotencyKey = crypto.randomUUID();
      localStorage.setItem('checkout_state', JSON.stringify({ cartHash, idempotencyKey }));
    }

    const response = await api.post('/api/pedidos', payload, {
      headers: {
        'Idempotency-Key': idempotencyKey
      }
    });
    // Clear checkout state on success
    localStorage.removeItem('checkout_state');
    return response.data;
  },

  obtenerMisPedidos: async (): Promise<Pedido[]> => {
    const response = await api.get('/api/pedidos/mis-pedidos');
    return response.data;
  },

  cancelarPedido: async (id: number): Promise<Pedido> => {
    const response = await api.put(`/api/pedidos/${id}/cancelar`);
    return response.data;
  }
};
