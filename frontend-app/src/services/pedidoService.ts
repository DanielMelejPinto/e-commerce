import api from '../api/axios';
import { Pedido, PedidoRequest } from '../types';
import { v4 as uuidv4 } from 'uuid';

function generateHash(items: any) {
  return JSON.stringify(items);
}

export const pedidoService = {
  crearPedido: async (pedidoReq: PedidoRequest): Promise<Pedido> => {
    let idempotencyKey = '';
    const cartHash = generateHash(pedidoReq.items);
    
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
      idempotencyKey = uuidv4();
      localStorage.setItem('checkout_state', JSON.stringify({ cartHash, idempotencyKey }));
    }

    try {
      const response = await api.post('/api/pedidos', pedidoReq, {
        headers: {
          'Idempotency-Key': idempotencyKey
        }
      });
      // Clear checkout state on success
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
    const response = await api.put(`/api/pedidos/${id}/cancelar`);
    return response.data;
  }
};
