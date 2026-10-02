import api from '../api/axios';
import type { Producto } from '../types';

export interface PaginatedProductos {
  content: Producto[];
  page: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
}

export const productoService = {
  obtenerTodos: async (page = 0, size = 10, nombre = ''): Promise<PaginatedProductos> => {
    const response = await api.get('/api/productos', {
      params: { page, size, nombre: nombre || undefined }
    });
    return response.data;
  },

  obtenerTodosAdmin: async (page = 0, size = 10, nombre = ''): Promise<PaginatedProductos> => {
    const response = await api.get('/api/productos/admin', {
      params: { page, size, nombre: nombre || undefined }
    });
    return response.data;
  },

  obtenerHistorial: async (ids: number[]): Promise<Producto[]> => {
    if (ids.length === 0) return [];
    const response = await api.post('/api/productos/historial', ids);
    return response.data;
  },
  
  crear: async (producto: Omit<Producto, 'id'>): Promise<Producto> => {
    const response = await api.post('/api/productos', producto);
    return response.data;
  }
};
