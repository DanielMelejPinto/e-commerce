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
  
  crear: async (producto: Omit<Producto, 'id'>): Promise<Producto> => {
    const response = await api.post('/api/productos', producto);
    return response.data;
  }
};
