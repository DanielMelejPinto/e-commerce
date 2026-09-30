import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export interface CartItem {
  productoId: number;
  nombre: string;
  precio: number;
  cantidad: number;
  imagenUrl?: string;
}

interface CartState {
  items: CartItem[];
  addToCart: (item: Omit<CartItem, 'cantidad'>, cantidad?: number) => void;
  removeFromCart: (productoId: number) => void;
  updateQuantity: (productoId: number, cantidad: number) => void;
  clearCart: () => void;
  getTotal: () => number;
}

export const useCartStore = create<CartState>()(
  persist(
    (set, get) => ({
      items: [],
      
      addToCart: (newItem, cantidad = 1) => set((state) => {
        const existing = state.items.find(item => item.productoId === newItem.productoId);
        if (existing) {
          return {
            items: state.items.map(item =>
              item.productoId === newItem.productoId
                ? { ...item, cantidad: item.cantidad + cantidad }
                : item
            )
          };
        }
        return { items: [...state.items, { ...newItem, cantidad }] };
      }),

      removeFromCart: (productoId) => set((state) => ({
        items: state.items.filter(item => item.productoId !== productoId)
      })),

      updateQuantity: (productoId, cantidad) => set((state) => {
        if (cantidad <= 0) {
          return { items: state.items.filter(item => item.productoId !== productoId) };
        }
        return {
          items: state.items.map(item =>
            item.productoId === productoId ? { ...item, cantidad } : item
          )
        };
      }),

      clearCart: () => set({ items: [] }),

      getTotal: () => {
        return get().items.reduce((sum, item) => sum + (item.precio * item.cantidad), 0);
      }
    }),
    {
      name: 'cart-storage', // Key for localStorage
    }
  )
);
