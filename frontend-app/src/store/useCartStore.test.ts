import { describe, it, expect, beforeEach } from 'vitest';
import { useCartStore } from './useCartStore';

describe('useCartStore', () => {
  beforeEach(() => {
    // Limpiar el estado antes de cada prueba
    useCartStore.getState().clearCart();
  });

  it('debería agregar un producto al carrito vacío', () => {
    const { addToCart } = useCartStore.getState();
    addToCart({ productoId: 1, nombre: 'Producto 1', precio: 100 }, 1);
    
    const { items, getTotal } = useCartStore.getState();
    expect(items).toHaveLength(1);
    expect(items[0].productoId).toBe(1);
    expect(items[0].cantidad).toBe(1);
    expect(getTotal()).toBe(100);
  });

  it('debería incrementar la cantidad si el producto ya existe', () => {
    const { addToCart } = useCartStore.getState();
    addToCart({ productoId: 1, nombre: 'Producto 1', precio: 100 }, 1);
    addToCart({ productoId: 1, nombre: 'Producto 1', precio: 100 }, 2);
    
    const { items, getTotal } = useCartStore.getState();
    expect(items).toHaveLength(1);
    expect(items[0].cantidad).toBe(3);
    expect(getTotal()).toBe(300);
  });

  it('debería eliminar un producto del carrito', () => {
    const { addToCart, removeFromCart } = useCartStore.getState();
    addToCart({ productoId: 1, nombre: 'Producto 1', precio: 100 }, 1);
    addToCart({ productoId: 2, nombre: 'Producto 2', precio: 200 }, 1);
    
    removeFromCart(1);
    
    const { items, getTotal } = useCartStore.getState();
    expect(items).toHaveLength(1);
    expect(items[0].productoId).toBe(2);
    expect(getTotal()).toBe(200);
  });

  it('debería actualizar la cantidad de un producto', () => {
    const { addToCart, updateQuantity } = useCartStore.getState();
    addToCart({ productoId: 1, nombre: 'Producto 1', precio: 100 }, 1);
    
    updateQuantity(1, 5);
    
    const { items, getTotal } = useCartStore.getState();
    expect(items[0].cantidad).toBe(5);
    expect(getTotal()).toBe(500);
  });

  it('debería eliminar el producto si la cantidad se actualiza a 0', () => {
    const { addToCart, updateQuantity } = useCartStore.getState();
    addToCart({ productoId: 1, nombre: 'Producto 1', precio: 100 }, 1);
    
    updateQuantity(1, 0);
    
    const { items, getTotal } = useCartStore.getState();
    expect(items).toHaveLength(0);
    expect(getTotal()).toBe(0);
  });
});
