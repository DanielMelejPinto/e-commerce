import { render, screen, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { CartProvider, useCart } from './CartContext';
import { describe, it, expect, beforeEach } from 'vitest';

const TestComponent = () => {
  const { items, addToCart, removeFromCart, updateQuantity, clearCart, total } = useCart();
  
  return (
    <div>
      <div data-testid="total">{total}</div>
      <div data-testid="items-length">{items.length}</div>
      {items.map(item => (
        <div key={item.productoId} data-testid={`item-${item.productoId}`}>
          {item.nombre} - {item.cantidad}
        </div>
      ))}
      <button onClick={() => addToCart({ productoId: 1, nombre: 'Test Prod', precio: 100 })}>
        Add Item
      </button>
      <button onClick={() => updateQuantity(1, 3)}>Update Qty</button>
      <button onClick={() => removeFromCart(1)}>Remove Item</button>
      <button onClick={clearCart}>Clear</button>
    </div>
  );
};

describe('CartContext', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('debería inicializar el carrito vacío', () => {
    render(
      <CartProvider>
        <TestComponent />
      </CartProvider>
    );
    expect(screen.getByTestId('total')).toHaveTextContent('0');
    expect(screen.getByTestId('items-length')).toHaveTextContent('0');
  });

  it('debería agregar un item al carrito', async () => {
    const user = userEvent.setup();
    render(
      <CartProvider>
        <TestComponent />
      </CartProvider>
    );
    
    await user.click(screen.getByText('Add Item'));
    expect(screen.getByTestId('items-length')).toHaveTextContent('1');
    expect(screen.getByTestId('total')).toHaveTextContent('100');
    expect(screen.getByTestId('item-1')).toHaveTextContent('Test Prod - 1');
  });

  it('debería aumentar la cantidad si el item ya existe', async () => {
    const user = userEvent.setup();
    render(
      <CartProvider>
        <TestComponent />
      </CartProvider>
    );
    
    await user.click(screen.getByText('Add Item'));
    await user.click(screen.getByText('Add Item'));
    
    expect(screen.getByTestId('items-length')).toHaveTextContent('1');
    expect(screen.getByTestId('total')).toHaveTextContent('200');
    expect(screen.getByTestId('item-1')).toHaveTextContent('Test Prod - 2');
  });

  it('debería actualizar la cantidad de un item', async () => {
    const user = userEvent.setup();
    render(
      <CartProvider>
        <TestComponent />
      </CartProvider>
    );
    
    await user.click(screen.getByText('Add Item'));
    await user.click(screen.getByText('Update Qty'));
    
    expect(screen.getByTestId('total')).toHaveTextContent('300');
    expect(screen.getByTestId('item-1')).toHaveTextContent('Test Prod - 3');
  });

  it('debería remover un item del carrito', async () => {
    const user = userEvent.setup();
    render(
      <CartProvider>
        <TestComponent />
      </CartProvider>
    );
    
    await user.click(screen.getByText('Add Item'));
    await user.click(screen.getByText('Remove Item'));
    
    expect(screen.getByTestId('items-length')).toHaveTextContent('0');
    expect(screen.queryByTestId('item-1')).not.toBeInTheDocument();
  });

  it('debería vaciar el carrito completamente', async () => {
    const user = userEvent.setup();
    render(
      <CartProvider>
        <TestComponent />
      </CartProvider>
    );
    
    await user.click(screen.getByText('Add Item'));
    await user.click(screen.getByText('Clear'));
    
    expect(screen.getByTestId('items-length')).toHaveTextContent('0');
    expect(screen.getByTestId('total')).toHaveTextContent('0');
  });
});
