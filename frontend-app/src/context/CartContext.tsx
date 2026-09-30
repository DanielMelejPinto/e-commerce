import { createContext, useContext, useState, useEffect } from 'react';

export interface CartItem {
  productoId: number;
  nombre: string;
  precio: number;
  cantidad: number;
  imagenUrl?: string;
}

interface CartContextType {
  items: CartItem[];
  addToCart: (item: Omit<CartItem, 'cantidad'>, cantidad?: number) => void;
  removeFromCart: (productoId: number) => void;
  updateQuantity: (productoId: number, cantidad: number) => void;
  clearCart: () => void;
  total: number;
}

const CartContext = createContext<CartContextType | undefined>(undefined);

export const CartProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [items, setItems] = useState<CartItem[]>(() => {
    const saved = localStorage.getItem('cart');
    return saved ? JSON.parse(saved) : [];
  });

  useEffect(() => {
    localStorage.setItem('cart', JSON.stringify(items));
  }, [items]);

  const addToCart = (newItem: Omit<CartItem, 'cantidad'>, cantidad = 1) => {
    setItems(prev => {
      const existing = prev.find(item => item.productoId === newItem.productoId);
      if (existing) {
        return prev.map(item =>
          item.productoId === newItem.productoId
            ? { ...item, cantidad: item.cantidad + cantidad }
            : item
        );
      }
      return [...prev, { ...newItem, cantidad }];
    });
  };

  const removeFromCart = (productoId: number) => {
    setItems(prev => prev.filter(item => item.productoId !== productoId));
  };

  const updateQuantity = (productoId: number, cantidad: number) => {
    if (cantidad <= 0) {
      removeFromCart(productoId);
      return;
    }
    setItems(prev => prev.map(item =>
      item.productoId === productoId ? { ...item, cantidad } : item
    ));
  };

  const clearCart = () => setItems([]);

  const total = items.reduce((sum, item) => sum + (item.precio * item.cantidad), 0);

  return (
    <CartContext.Provider value={{ items, addToCart, removeFromCart, updateQuantity, clearCart, total }}>
      {children}
    </CartContext.Provider>
  );
};

export const useCart = () => {
  const context = useContext(CartContext);
  if (context === undefined) {
    throw new Error('useCart must be used within a CartProvider');
  }
  return context;
};
