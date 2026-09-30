import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useCart } from '../../context/CartContext';
import { useAuth } from '../../context/AuthContext';
import { pedidoService } from '../../services/pedidoService';
import { Trash2, CheckCircle } from 'lucide-react';
import styles from './Cart.module.css';

const Cart = () => {
  const { items, updateQuantity, removeFromCart, total, clearCart } = useCart();
  const { user } = useAuth();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [isSuccess, setIsSuccess] = useState(false);
  const [orderId, setOrderId] = useState<number | null>(null);

  const handleCheckout = async () => {
    if (!user) {
      navigate('/login');
      return;
    }

    if (items.length === 0) return;

    setLoading(true);
    setError('');

    try {
      const payload = {
        items: items.map(item => ({
          productoId: item.productoId,
          cantidad: item.cantidad
        }))
      };

      const order = await pedidoService.crearPedido(payload);
      clearCart();
      setOrderId(order.id);
      setIsSuccess(true);
    } catch (err: unknown) {
      console.error(err);
      if (typeof err === 'object' && err !== null && 'response' in err) {
        const error = err as { response?: { data?: { message?: string } } };
        setError(error.response?.data?.message || 'Error al procesar el pedido. Verifica el stock.');
      } else {
        setError('Error al procesar el pedido. Verifica el stock.');
      }
    } finally {
      setLoading(false);
    }
  };

  if (isSuccess) {
    return (
      <div className={styles.container}>
        <div className={styles.successState}>
          <CheckCircle />
          <h2>¡Pedido Confirmado!</h2>
          <p>Tu pedido #{orderId} ha sido creado exitosamente.</p>
          <Link to="/profile">
            <button className={styles.checkoutButton} style={{ width: 'auto', padding: '0.75rem 2rem' }}>
              Ver mis pedidos
            </button>
          </Link>
        </div>
      </div>
    );
  }

  if (items.length === 0) {
    return (
      <div className={styles.container}>
        <h1>Carrito de Compras</h1>
        <div className={styles.empty}>Tu carrito está vacío.</div>
      </div>
    );
  }

  return (
    <div className={styles.container}>
      <h1>Carrito de Compras</h1>
      
      {error && <div className={styles.error}>{error}</div>}

      <div className={styles.cartContent}>
        <div className={styles.itemsList}>
          {items.map(item => (
            <div key={item.productoId} className={styles.cartItem}>
              {item.imagenUrl ? (
                <img src={item.imagenUrl} alt={item.nombre} className={styles.itemImage} />
              ) : (
                <div className={styles.imagePlaceholder} />
              )}
              
              <div className={styles.itemDetails}>
                <h3>{item.nombre}</h3>
                <p className={styles.itemPrice}>${item.precio.toFixed(2)}</p>
              </div>

              <div className={styles.itemActions}>
                <input
                  type="number"
                  min="1"
                  value={item.cantidad}
                  onChange={(e) => updateQuantity(item.productoId, parseInt(e.target.value) || 1)}
                  className={styles.quantityInput}
                />
                <button
                  onClick={() => removeFromCart(item.productoId)}
                  className={styles.deleteButton}
                >
                  <Trash2 size={20} />
                </button>
              </div>
            </div>
          ))}
        </div>

        <div className={styles.summary}>
          <h2>Resumen</h2>
          <div className={styles.summaryRow}>
            <span>Total:</span>
            <span className={styles.totalPrice}>${total.toFixed(2)}</span>
          </div>
          <button
            onClick={handleCheckout}
            disabled={loading}
            className={styles.checkoutButton}
          >
            {loading ? 'Procesando...' : 'Realizar Pedido'}
          </button>
        </div>
      </div>
    </div>
  );
};

export default Cart;
