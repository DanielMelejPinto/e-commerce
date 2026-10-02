import { useEffect, useState } from 'react';
import { productoService } from '../../services/productoService';
import { inventarioService } from '../../services/inventarioService';
import type { Producto, Inventario } from '../../types';
import styles from './AdminProducts.module.css';

const AdminProducts = () => {
  const [products, setProducts] = useState<Producto[]>([]);
  const [inventories, setInventories] = useState<Record<number, Inventario>>({});
  const [loading, setLoading] = useState(true);
  
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  // Form states for new product
  const [showForm, setShowForm] = useState(false);
  const [nombre, setNombre] = useState('');
  const [descripcion, setDescripcion] = useState('');
  const [precio, setPrecio] = useState('');
  const [imagenUrl, setImagenUrl] = useState('');

  // Stock management state
  const [stockAdd, setStockAdd] = useState<Record<number, string>>({});

  const fetchProducts = async (currentPage = 0) => {
    try {
      const prodList = await productoService.obtenerTodosAdmin(currentPage, 10);
      setProducts(prodList.content);
      setTotalPages(prodList.page.totalPages);
      setPage(currentPage);
      
      // Fetch inventory for each product
      const invData: Record<number, Inventario> = {};
      await Promise.all(
        prodList.content.map(async (p: Producto) => {
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
    fetchProducts(0);
  }, []);

  const [creating, setCreating] = useState(false);
  const [updatingStock, setUpdatingStock] = useState<number | null>(null);

  const handleCreateProduct = async (e: React.FormEvent) => {
    e.preventDefault();
    if (creating) return;
    setCreating(true);
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
      fetchProducts(page);
    } catch (err) {
      console.error(err);
      alert('Error creando producto');
    } finally {
      setCreating(false);
    }
  };

  const handleAddStock = async (productoId: number) => {
    const qty = parseInt(stockAdd[productoId] || '0');
    if (qty <= 0 || updatingStock === productoId) return;
    setUpdatingStock(productoId);
    try {
      await inventarioService.agregarStock(productoId, qty);
      setStockAdd({ ...stockAdd, [productoId]: '' });
      fetchProducts(page);
    } catch (err) {
      console.error(err);
      alert('Error agregando stock');
    } finally {
      setUpdatingStock(null);
    }
  };

  if (loading) return <main role="status">Cargando panel de administración...</main>;

  return (
    <main className={styles.container}>
      <div className={styles.header}>
        <h1>Administración de Productos e Inventario</h1>
        <button className={styles.primaryButton} onClick={() => setShowForm(!showForm)}>
          {showForm ? 'Cancelar' : 'Nuevo Producto'}
        </button>
      </div>

      {showForm && (
        <form onSubmit={handleCreateProduct} className={styles.formCard} aria-label="Formulario de creación de producto">
          <h2>Crear Producto</h2>
          <div className={styles.formGrid}>
            <div className={styles.formGroup}>
              <label htmlFor="nombre">Nombre</label>
              <input id="nombre" required value={nombre} onChange={e => setNombre(e.target.value)} />
            </div>
            <div className={styles.formGroup}>
              <label htmlFor="precio">Precio</label>
              <input id="precio" required type="number" step="0.01" value={precio} onChange={e => setPrecio(e.target.value)} />
            </div>
            <div className={styles.formGroup}>
              <label htmlFor="imagenUrl">URL Imagen</label>
              <input id="imagenUrl" value={imagenUrl} onChange={e => setImagenUrl(e.target.value)} />
            </div>
            <div className={styles.formGroup}>
              <label htmlFor="descripcion">Descripción</label>
              <textarea id="descripcion" value={descripcion} onChange={e => setDescripcion(e.target.value)} />
            </div>
          </div>
          <button type="submit" disabled={creating} className={styles.successButton}>
            {creating ? 'Guardando...' : 'Guardar Producto'}
          </button>
        </form>
      )}

      <table className={styles.table}>
        <caption className="sr-only">Inventario de productos</caption>
        <thead>
          <tr>
            <th scope="col">ID</th>
            <th scope="col">Producto</th>
            <th scope="col">Precio</th>
            <th scope="col">Stock Disponible</th>
            <th scope="col">Stock Reservado</th>
            <th scope="col">Añadir Stock</th>
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
                  {inventories[p.id]?.cantidadDisponible ?? 0}
                </span>
              </td>
              <td>
                <span className={styles.badgeWarning}>
                  {inventories[p.id]?.cantidadReservada ?? 0}
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
                    aria-label={`Cantidad a añadir al stock de ${p.nombre}`}
                    disabled={updatingStock === p.id}
                  />
                  <button 
                    onClick={() => handleAddStock(p.id)}
                    className={styles.secondaryButton}
                    disabled={!stockAdd[p.id] || updatingStock === p.id}
                    aria-label={`Ingresar stock para ${p.nombre}`}
                  >
                    {updatingStock === p.id ? '...' : 'Ingresar'}
                  </button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <div style={{ marginTop: '1rem', display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
        <button disabled={page === 0} onClick={() => fetchProducts(page - 1)} className={styles.secondaryButton}>Anterior</button>
        <span>Página {page + 1} de {totalPages || 1}</span>
        <button disabled={page >= totalPages - 1} onClick={() => fetchProducts(page + 1)} className={styles.secondaryButton}>Siguiente</button>
      </div>
    </main>
  );
};

export default AdminProducts;
