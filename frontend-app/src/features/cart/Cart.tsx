import { useState } from 'react';
import { useCartStore } from '../../store/useCartStore';
import { useAuth } from '../../context/AuthContext';
import { pedidoService } from '../../services/pedidoService';
import { useNavigate } from 'react-router-dom';
import { extractErrorMessage } from '../../utils/errorHelper';
import styles from './Cart.module.css';

const Cart = () => {
  const items = useCartStore(state => state.items);
  const removeFromCart = useCartStore(state => state.removeFromCart);
  const updateQuantity = useCartStore(state => state.updateQuantity);
  const clearCart = useCartStore(state => state.clearCart);
  const total = useCartStore(state => state.getTotal());

  const { user } = useAuth();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleCheckout = async () => {
    if (!user) {
      navigate('/login');
      return;
    }
    if (items.length === 0) return;

    setLoading(true);
    setError('');

    try {
      const payloadItems = items.map((i: any) => ({
        productoId: i.producto.id,
        cantidad: i.cantidad
      }));
      await pedidoService.crearPedido({ items: payloadItems });
      clearCart();
      navigate('/profile');
    } catch (err: unknown) {
      setError(extractErrorMessage(err, 'Error al procesar el pedido. Verifica el stock.'));
    } finally {
      setLoading(false);
    }
  };

  if (items.length === 0) {
    return (
      <main className={styles.container}>
        <h1 className={styles.title}>Carrito de Compras</h1>
        <div className={styles.emptyState}>
          <p>Tu carrito está vacío</p>
          <button onClick={() => navigate('/')} className={styles.primaryButton}>
            Volver a la tienda
          </button>
        </div>
      </main>
    );
  }

  return (
    <main className={styles.container}>
      <h1 className={styles.title}>Carrito de Compras</h1>
      
      {error && <div className={styles.error} role="alert">{error}</div>}

      <div className={styles.cartContent}>
        <div className={styles.itemsList}>
          {items.map((item: any) => (
            <article key={item.producto.id} className={styles.cartItem}>
              <div className={styles.itemInfo}>
                <h3>{item.producto.nombre}</h3>
                <p className={styles.price}>${item.producto.precio.toFixed(2)}</p>
              </div>
              <div className={styles.itemControls}>
                <input
                  type="number"
                  min="1"
                  value={item.cantidad}
                  onChange={(e) => updateQuantity(item.producto.id, parseInt(e.target.value) || 1)}
                  className={styles.quantityInput}
                  aria-label={`Cantidad de ${item.producto.nombre}`}
                />
                <button
                  onClick={() => removeFromCart(item.producto.id)}
                  className={styles.removeButton}
                  aria-label={`Eliminar ${item.producto.nombre} del carrito`}
                >
                  Eliminar
                </button>
              </div>
              <div className={styles.itemTotal}>
                ${(item.producto.precio * item.cantidad).toFixed(2)}
              </div>
            </article>
          ))}
        </div>

        <aside className={styles.summaryCard}>
          <h2>Resumen</h2>
          <div className={styles.summaryRow}>
            <span>Total:</span>
            <span className={styles.totalPrice}>${total.toFixed(2)}</span>
          </div>
          <button
            onClick={handleCheckout}
            disabled={loading}
            className={styles.checkoutButton}
            aria-busy={loading}
          >
            {loading ? 'Procesando...' : 'Finalizar Compra'}
          </button>
        </aside>
      </div>
    </main>
  );
};

export default Cart;
