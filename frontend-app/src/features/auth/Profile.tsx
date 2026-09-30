import { useEffect, useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import api from '../../api/axios';
import styles from './Profile.module.css';

const Profile = () => {
  const { user } = useAuth();
  const [pedidos, setPedidos] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchPedidos = async () => {
      try {
        const response = await api.get('/api/pedidos/mis-pedidos');
        setPedidos(response.data);
      } catch (error) {
        console.error('Error fetching pedidos', error);
      } finally {
        setLoading(false);
      }
    };

    fetchPedidos();
  }, []);

  return (
    <div className={styles.container}>
      <h1 className={styles.title}>Mi Perfil</h1>
      
      <div className={styles.card}>
        <h2>Datos Personales</h2>
        <p><strong>Nombre:</strong> {user?.nombre}</p>
        <p><strong>Email:</strong> {user?.email}</p>
        <p><strong>Rol:</strong> {user?.rol}</p>
      </div>

      <div className={styles.ordersSection}>
        <h2>Mis Pedidos</h2>
        {loading ? (
          <p>Cargando pedidos...</p>
        ) : pedidos.length === 0 ? (
          <p>No has realizado ningún pedido aún.</p>
        ) : (
          <div className={styles.ordersList}>
            {pedidos.map((pedido) => (
              <div key={pedido.id} className={styles.orderCard}>
                <div className={styles.orderHeader}>
                  <span className={styles.orderId}>Pedido #{pedido.id}</span>
                  <span className={`${styles.status} ${styles[pedido.estado.toLowerCase()]}`}>
                    {pedido.estado}
                  </span>
                </div>
                <div className={styles.orderDetails}>
                  <p><strong>Fecha:</strong> {new Date(pedido.fechaCreacion).toLocaleDateString()}</p>
                  <p><strong>Total:</strong> ${pedido.total.toFixed(2)}</p>
                </div>
                <div className={styles.orderItems}>
                  <h4>Productos:</h4>
                  <ul>
                    {pedido.items.map((item: any) => (
                      <li key={item.id}>
                        Producto ID: {item.productoId} x {item.cantidad} (${item.precioUnitario.toFixed(2)})
                      </li>
                    ))}
                  </ul>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default Profile;
