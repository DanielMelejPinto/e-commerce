import { useEffect, useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { pedidoService } from '../../services/pedidoService';
import { productoService } from '../../services/productoService';
import type { Pedido, Producto } from '../../types';
import styles from './Profile.module.css';

// Componente interactivo para evitar window.confirm
const CancelButton = ({ onConfirm, disabled }: { onConfirm: () => void, disabled?: boolean }) => {
  const [asking, setAsking] = useState(false);

  if (asking) {
    return (
      <div style={{ display: 'flex', gap: '10px' }}>
        <button 
          onClick={onConfirm} 
          className={`${styles.cancelButton} ${styles.cancelConfirmButton}`}
          disabled={disabled}
        >
          ¿Seguro? Cancelar
        </button>
        <button 
          onClick={() => setAsking(false)} 
          className={styles.cancelButton} 
          style={{ backgroundColor: '#6c757d' }}
          disabled={disabled}
        >
          No
        </button>
      </div>
    );
  }

  return (
    <button 
      onClick={() => setAsking(true)} 
      className={styles.cancelButton}
      disabled={disabled}
    >
      Cancelar Pedido
    </button>
  );
};

const Profile = () => {
  const { user } = useAuth();
  const [pedidos, setPedidos] = useState<Pedido[]>([]);
  const [productsMap, setProductsMap] = useState<Record<number, string>>({});
  const [loading, setLoading] = useState(true);
  const [canceling, setCanceling] = useState<number | null>(null);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [pedidosData, productosData] = await Promise.all([
          pedidoService.obtenerMisPedidos(),
          productoService.obtenerTodos()
        ]);
        
        // Crear mapa de productos para buscar el nombre por ID
        const pMap: Record<number, string> = {};
        productosData.forEach((p: Producto) => {
          pMap[p.id] = p.nombre;
        });

        // Ordenar pedidos del más nuevo al más viejo
        const sortedPedidos = pedidosData.sort((a, b) => 
          new Date(b.fechaCreacion).getTime() - new Date(a.fechaCreacion).getTime()
        );

        setProductsMap(pMap);
        setPedidos(sortedPedidos);
      } catch (error) {
        console.error('Error fetching data', error);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  const handleCancelar = async (pedidoId: number) => {
    setCanceling(pedidoId);
    try {
      const pedidoCancelado = await pedidoService.cancelarPedido(pedidoId);
      setPedidos((prev) => prev.map((p) => (p.id === pedidoId ? pedidoCancelado : p)));
    } catch (error: unknown) {
      if (typeof error === 'object' && error !== null && 'response' in error) {
        const err = error as { response?: { data?: { message?: string } } };
        alert(err.response?.data?.message || 'Error al cancelar el pedido');
      } else {
        alert('Error al cancelar el pedido');
      }
    } finally {
      setCanceling(null);
    }
  };

  return (
    <main className={styles.container}>
      <h1 className={styles.title}>Mi Perfil</h1>
      
      <section className={styles.card}>
        <h2>Datos Personales</h2>
        <p><strong>Nombre:</strong> {user?.nombre}</p>
        <p><strong>Email:</strong> {user?.email}</p>
        <p><strong>Rol:</strong> {user?.rol}</p>
      </section>

      <section className={styles.ordersSection}>
        <h2>Historial de Pedidos</h2>
        {loading ? (
          <p role="status">Cargando historial...</p>
        ) : pedidos.length === 0 ? (
          <p>No has realizado ningún pedido aún.</p>
        ) : (
          <div className={styles.ordersList}>
            {pedidos.map((pedido) => (
              <article key={pedido.id} className={styles.orderCard}>
                <div className={styles.orderHeader}>
                  <span className={styles.orderId}>Pedido #{pedido.id}</span>
                  <span className={`${styles.status} ${styles[pedido.estado.toLowerCase()]}`}>
                    {pedido.estado}
                  </span>
                </div>
                <div className={styles.orderDetails}>
                  <p><strong>Fecha:</strong> {new Date(pedido.fechaCreacion).toLocaleString()}</p>
                  <p><strong>Total:</strong> ${pedido.total.toFixed(2)}</p>
                </div>
                <div className={styles.orderItems}>
                  <h4>Productos:</h4>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '5px' }}>
                    {pedido.items.map((item, idx) => (
                      <div key={idx} className={styles.itemDetailRow}>
                        <span>{item.cantidad}x {productsMap[item.productoId] || `Producto #${item.productoId}`}</span>
                        <span>${(item.precioUnitario * item.cantidad).toFixed(2)}</span>
                      </div>
                    ))}
                  </div>
                </div>
                {pedido.estado === 'CONFIRMADO' && (
                  <CancelButton 
                    onConfirm={() => handleCancelar(pedido.id)} 
                    disabled={canceling === pedido.id}
                  />
                )}
              </article>
            ))}
          </div>
        )}
      </section>
    </main>
  );
};

export default Profile;
