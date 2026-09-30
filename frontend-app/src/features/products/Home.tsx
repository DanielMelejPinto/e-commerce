import { useState, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { productoService } from '../../services/productoService';
import type { Producto } from '../../types';
import { useCartStore } from '../../store/useCartStore';
import styles from './Home.module.css';

const Home = () => {
  const [page, setPage] = useState(0);
  const [searchTerm, setSearchTerm] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');

  // Debounce the search input to avoid hitting the backend on every keystroke
  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedSearch(searchTerm);
      setPage(0); // Reset to first page on new search
    }, 500);
    return () => clearTimeout(handler);
  }, [searchTerm]);

  const { data, isLoading: loading } = useQuery({
    queryKey: ['productos', page, debouncedSearch],
    queryFn: () => productoService.obtenerTodos(page, 10, debouncedSearch)
  });

  const [addedItemIds, setAddedItemIds] = useState<Set<number>>(new Set());
  const addToCart = useCartStore((state) => state.addToCart);

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

  const products = data?.content || [];
  const totalPages = data?.page?.totalPages || 1;

  return (
    <main className={styles.container}>
      <div className={styles.searchContainer}>
        <h1>Catálogo de Productos</h1>
        <input 
          type="text" 
          placeholder="Buscar productos en el servidor..." 
          aria-label="Buscar productos en el catálogo"
          className={styles.searchInput}
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
        />
      </div>
      
      <div className={styles.grid}>
        {loading ? (
          // Skeleton loaders
          Array.from({ length: 8 }).map((_, i) => (
            <div key={`skeleton-${i}`} className={styles.card} aria-hidden="true">
              <div className={`${styles.skeleton} ${styles.skeletonImage}`} />
              <div className={`${styles.skeleton} ${styles.skeletonTitle}`} />
              <div className={`${styles.skeleton} ${styles.skeletonDesc}`} />
              <div className={`${styles.skeleton} ${styles.skeletonPrice}`} />
              <div className={`${styles.skeleton} ${styles.skeletonButton}`} />
            </div>
          ))
        ) : products.length > 0 ? (
          products.map((product) => (
            <article key={product.id} className={styles.card}>
              {product.imagenUrl ? (
                <img src={product.imagenUrl} alt={product.nombre} className={styles.image} />
              ) : (
                <div className={styles.imagePlaceholder} aria-hidden="true">Sin Imagen</div>
              )}
              <h2 className={styles.title}>{product.nombre}</h2>
              <p className={styles.desc}>{product.descripcion}</p>
              <p className={styles.price}>${product.precio.toFixed(2)}</p>
              <button 
                className={`${styles.button} ${addedItemIds.has(product.id) ? styles.buttonSuccess : ''}`}
                onClick={() => handleAddToCart(product)}
                aria-label={addedItemIds.has(product.id) ? `${product.nombre} agregado al carrito` : `Agregar ${product.nombre} al carrito`}
              >
                {addedItemIds.has(product.id) ? '¡Agregado!' : 'Agregar al Carrito'}
              </button>
            </article>
          ))
        ) : (
          <div className={styles.emptyState} role="status">
            No se encontraron productos para "{debouncedSearch}"
          </div>
        )}
      </div>

      {totalPages > 1 && (
        <div className={styles.pagination}>
          <button 
            disabled={page === 0} 
            onClick={() => setPage(p => p - 1)}
            className={styles.pageButton}
          >
            Anterior
          </button>
          <span className={styles.pageInfo}>Página {page + 1} de {totalPages}</span>
          <button 
            disabled={page === totalPages - 1} 
            onClick={() => setPage(p => p + 1)}
            className={styles.pageButton}
          >
            Siguiente
          </button>
        </div>
      )}
    </main>
  );
};

export default Home;
