import api from '../api/axios';

export interface PedidoItemRequest {
  productoId: number;
  cantidad: number;
}

export interface PedidoRequest {
  items: PedidoItemRequest[];
}

export interface PedidoResponse {
  id: number;
  usuarioId: number;
  total: number;
  estado: string;
  fechaCreacion: string;
}

export const crearPedido = async (data: PedidoRequest): Promise<PedidoResponse> => {
  // Add Idempotency-Key
  const idempotencyKey = crypto.randomUUID();
  const response = await api.post('/api/pedidos', data, {
    headers: {
      'Idempotency-Key': idempotencyKey
    }
  });
  return response.data;
};

export const obtenerMisPedidos = async (): Promise<PedidoResponse[]> => {
  const response = await api.get('/api/pedidos/mis-pedidos');
  return response.data;
};

export const cancelarPedido = async (id: number): Promise<PedidoResponse> => {
  const response = await api.post(`/api/pedidos/${id}/cancelar`);
  return response.data;
};
