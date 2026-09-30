import { useEffect, useState } from 'react';
import { productoService } from '../../services/productoService';
import { inventarioService } from '../../services/inventarioService';
import type { Producto, Inventario } from '../../types';
import styles from './AdminProducts.module.css';

const AdminProducts = () => {
  const [products, setProducts] = useState<Producto[]>([]);
  const [inventories, setInventories] = useState<Record<number, Inventario>>({});
  const [loading, setLoading] = useState(true);

  // Form states for new product
  const [showForm, setShowForm] = useState(false);
  const [nombre, setNombre] = useState('');
  const [descripcion, setDescripcion] = useState('');
  const [precio, setPrecio] = useState('');
  const [imagenUrl, setImagenUrl] = useState('');

  // Stock management state
  const [stockAdd, setStockAdd] = useState<Record<number, string>>({});

  const fetchProducts = async () => {
    try {
      const prodList = await productoService.obtenerTodos();
      setProducts(prodList);
      
      // Fetch inventory for each product
      const invData: Record<number, Inventario> = {};
      await Promise.all(
        prodList.map(async (p: Producto) => {
          try {
            const inv = await inventarioService.obtenerPorProducto(p.id);
            invData[p.id] = inv;
          } catch (e: unknown) {
            if (typeof e === 'object' && e !== null && 'response' in e) {
              const err = e as { response?: { status?: number } };
              if (err.response?.status === 404) {
                // Inventario no inicializado, vamos a inicializarlo
                try {
                  const initInv = await inventarioService.inicializar(p.id);
                  invData[p.id] = initInv;
                } catch (err) {
                  console.error("No se pudo inicializar inventario", err);
                }
              }
            }
          }
        })
      );
      setInventories(invData);
    } catch (err) {
      console.error('Error fetching products admin', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProducts();
  }, []);

  const handleCreateProduct = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await productoService.crear({
        nombre,
        descripcion,
        precio: parseFloat(precio),
        imagenUrl
      });
      // Reset
      setShowForm(false);
      setNombre(''); setDescripcion(''); setPrecio(''); setImagenUrl('');
      fetchProducts();
    } catch (err) {
      console.error(err);
      alert('Error creando producto');
    }
  };

  const handleAddStock = async (productoId: number) => {
    const qty = parseInt(stockAdd[productoId] || '0');
    if (qty <= 0) return;
    try {
      await inventarioService.agregarStock(productoId, qty);
      setStockAdd({ ...stockAdd, [productoId]: '' });
      fetchProducts();
    } catch (err) {
      console.error(err);
      alert('Error agregando stock');
    }
  };

  if (loading) return <div>Cargando panel de administración...</div>;

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <h1>Administración de Productos e Inventario</h1>
        <button className={styles.primaryButton} onClick={() => setShowForm(!showForm)}>
          {showForm ? 'Cancelar' : 'Nuevo Producto'}
        </button>
      </div>

      {showForm && (
        <form onSubmit={handleCreateProduct} className={styles.formCard}>
          <h2>Crear Producto</h2>
          <div className={styles.formGrid}>
            <div className={styles.formGroup}>
              <label>Nombre</label>
              <input required value={nombre} onChange={e => setNombre(e.target.value)} />
            </div>
            <div className={styles.formGroup}>
              <label>Precio</label>
              <input required type="number" step="0.01" value={precio} onChange={e => setPrecio(e.target.value)} />
            </div>
            <div className={styles.formGroup}>
              <label>URL Imagen</label>
              <input value={imagenUrl} onChange={e => setImagenUrl(e.target.value)} />
            </div>
            <div className={styles.formGroup}>
              <label>Descripción</label>
              <textarea value={descripcion} onChange={e => setDescripcion(e.target.value)} />
            </div>
          </div>
          <button type="submit" className={styles.successButton}>Guardar Producto</button>
        </form>
      )}

      <table className={styles.table}>
        <thead>
          <tr>
            <th>ID</th>
            <th>Producto</th>
            <th>Precio</th>
            <th>Stock Disponible</th>
            <th>Stock Reservado</th>
            <th>Añadir Stock</th>
          </tr>
        </thead>
        <tbody>
          {products.map(p => (
            <tr key={p.id}>
              <td>{p.id}</td>
              <td>{p.nombre}</td>
              <td>${p.precio.toFixed(2)}</td>
              <td>
                <span className={styles.badgeSuccess}>
                  {inventories[p.id]?.stockDisponible ?? 0}
                </span>
              </td>
              <td>
                <span className={styles.badgeWarning}>
                  {inventories[p.id]?.stockReservado ?? 0}
                </span>
              </td>
              <td>
                <div className={styles.stockAction}>
                  <input
                    type="number"
                    min="1"
                    placeholder="Cant."
                    value={stockAdd[p.id] || ''}
                    onChange={e => setStockAdd({ ...stockAdd, [p.id]: e.target.value })}
                    className={styles.stockInput}
                  />
                  <button 
                    onClick={() => handleAddStock(p.id)}
                    className={styles.secondaryButton}
                    disabled={!stockAdd[p.id]}
                  >
                    Ingresar
                  </button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default AdminProducts;
