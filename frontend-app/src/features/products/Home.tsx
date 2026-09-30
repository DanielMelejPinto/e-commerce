import { useEffect, useState } from 'react';
import api from '../../api/axios';
import { useCart } from '../../context/CartContext';
import styles from './Home.module.css';

const Home = () => {
  const [products, setProducts] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const { addToCart } = useCart();

  useEffect(() => {
    const fetchProducts = async () => {
      try {
        const response = await api.get('/api/productos');
        setProducts(response.data.content || response.data);
      } catch (error) {
        console.error('Error fetching products', error);
      } finally {
        setLoading(false);
      }
    };

    fetchProducts();
  }, []);

  if (loading) {
    return <div className={styles.container}><h2>Cargando productos...</h2></div>;
  }

  return (
    <div className={styles.container}>
      <h1>Catálogo de Productos</h1>
      <div className={styles.grid}>
        {products.map((product) => (
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
              className={styles.button}
              onClick={() => addToCart({
                productoId: product.id,
                nombre: product.nombre,
                precio: product.precio,
                imagenUrl: product.imagenUrl
              })}
            >
              Agregar al Carrito
            </button>
          </div>
        ))}
      </div>
    </div>
  );
};

export default Home;
