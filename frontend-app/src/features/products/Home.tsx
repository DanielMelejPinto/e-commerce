import { useEffect, useState, useMemo } from 'react';
import { productoService } from '../../services/productoService';
import type { Producto } from '../../types';
import { useCart } from '../../context/CartContext';
import styles from './Home.module.css';

const Home = () => {
  const [products, setProducts] = useState<Producto[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [addedItemIds, setAddedItemIds] = useState<Set<number>>(new Set());
  const { addToCart } = useCart();

  useEffect(() => {
    const fetchProducts = async () => {
      try {
        const data = await productoService.obtenerTodos();
        setProducts(data);
      } catch (error) {
        console.error('Error fetching products', error);
      } finally {
        setLoading(false);
      }
    };

    fetchProducts();
  }, []);

  const handleAddToCart = (product: Producto) => {
    addToCart({
      productoId: product.id,
      nombre: product.nombre,
      precio: product.precio,
      imagenUrl: product.imagenUrl
    });

    setAddedItemIds(prev => new Set(prev).add(product.id));
    setTimeout(() => {
      setAddedItemIds(prev => {
        const next = new Set(prev);
        next.delete(product.id);
        return next;
      });
    }, 2000);
  };

  const filteredProducts = useMemo(() => {
    return products.filter(p => 
      p.nombre.toLowerCase().includes(searchTerm.toLowerCase()) || 
      p.descripcion.toLowerCase().includes(searchTerm.toLowerCase())
    );
  }, [products, searchTerm]);

  return (
    <div className={styles.container}>
      <div className={styles.searchContainer}>
        <h1>Catálogo de Productos</h1>
        <input 
          type="text" 
          placeholder="Buscar productos..." 
          className={styles.searchInput}
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
        />
      </div>
      
      <div className={styles.grid}>
        {loading ? (
          // Skeleton loaders
          Array.from({ length: 8 }).map((_, i) => (
            <div key={`skeleton-${i}`} className={styles.card}>
              <div className={`${styles.skeleton} ${styles.skeletonImage}`} />
              <div className={`${styles.skeleton} ${styles.skeletonTitle}`} />
              <div className={`${styles.skeleton} ${styles.skeletonDesc}`} />
              <div className={`${styles.skeleton} ${styles.skeletonPrice}`} />
              <div className={`${styles.skeleton} ${styles.skeletonButton}`} />
            </div>
          ))
        ) : filteredProducts.length > 0 ? (
          filteredProducts.map((product) => (
            <div key={product.id} className={styles.card}>
              {product.imagenUrl ? (
                <img src={product.imagenUrl} alt={product.nombre} className={styles.image} />
              ) : (
                <div className={styles.imagePlaceholder}>Sin Imagen</div>
              )}
              <h2 className={styles.title}>{product.nombre}</h2>
              <p className={styles.desc}>{product.descripcion}</p>
              <p className={styles.price}>${product.precio.toFixed(2)}</p>
              <button 
                className={`${styles.button} ${addedItemIds.has(product.id) ? styles.buttonSuccess : ''}`}
                onClick={() => handleAddToCart(product)}
              >
                {addedItemIds.has(product.id) ? '¡Agregado!' : 'Agregar al Carrito'}
              </button>
            </div>
          ))
        ) : (
          <div className={styles.emptyState}>
            No se encontraron productos para "{searchTerm}"
          </div>
        )}
      </div>
    </div>
  );
};

export default Home;
