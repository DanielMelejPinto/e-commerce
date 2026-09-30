import api from '../api/axios';
import type { Producto } from '../types';

export const productoService = {
  obtenerTodos: async (): Promise<Producto[]> => {
    const response = await api.get('/api/productos?size=100');
    return response.data.content || response.data;
  },
  
  crear: async (producto: Omit<Producto, 'id'>): Promise<Producto> => {
    const response = await api.post('/api/productos', producto);
    return response.data;
  }
};
