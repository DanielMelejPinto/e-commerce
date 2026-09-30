import api from '../api/axios';
import type { Usuario } from '../types';

export const usuarioService = {
  login: async (email: string, password: string): Promise<{ token: string }> => {
    const response = await api.post('/api/usuarios/login', { email, password });
    return response.data;
  },

  registro: async (nombre: string, email: string, password: string): Promise<void> => {
    await api.post('/api/usuarios/registro', { nombre, email, password });
  },

  obtenerPerfil: async (token?: string): Promise<Usuario> => {
    const headers = token ? { Authorization: `Bearer ${token}` } : undefined;
    const response = await api.get('/api/usuarios/me', { headers });
    return response.data;
  }
};
