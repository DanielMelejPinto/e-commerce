import api from '../api/axios';
import type { Inventario } from '../types';

export const inventarioService = {
  obtenerPorProducto: async (productoId: number): Promise<Inventario> => {
    const response = await api.get(`/api/inventarios/producto/${productoId}`);
    return response.data;
  },

  inicializar: async (productoId: number): Promise<Inventario> => {
    const response = await api.post(`/api/inventarios/producto/${productoId}`);
    return response.data;
  },

  agregarStock: async (productoId: number, cantidad: number): Promise<void> => {
    await api.put(`/api/inventarios/producto/${productoId}/agregar`, { cantidad });
  }
};
