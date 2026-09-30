import { Link, useNavigate } from 'react-router-dom';
import { ShoppingCart, User, LogOut, LogIn } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';
import styles from './Navbar.module.css';

const Navbar = () => {
  const { user, logout } = useAuth();
  const { items } = useCart();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <header className={styles.header}>
      <div className={styles.navContainer}>
        <Link to="/" className={styles.logo} aria-label="TechStore - Volver al inicio">TechStore</Link>
        <nav className={styles.nav} aria-label="Navegación principal">
          <Link to="/cart" className={styles.link} aria-label={`Ir al carrito (${items.reduce((acc, item) => acc + item.cantidad, 0)} artículos)`}>
            <div className={styles.cartIconWrapper} aria-hidden="true">
              <ShoppingCart size={24} />
              {items.length > 0 && (
                <span className={styles.cartBadge}>{items.reduce((acc, item) => acc + item.cantidad, 0)}</span>
              )}
            </div>
          </Link>
          
          {user ? (
            <>
              {user.rol === 'ADMIN' && (
                <Link to="/admin" className={styles.adminLink} aria-label="Ir al Panel de Administración">
                  Panel Admin
                </Link>
              )}
              <Link to="/profile" className={styles.link} aria-label={`Ir al perfil de ${user.nombre}`}>
                <User size={24} aria-hidden="true" />
                <span className={styles.userName}>{user.nombre}</span>
              </Link>
              <button onClick={handleLogout} className={styles.iconButton} aria-label="Cerrar sesión" title="Cerrar sesión">
                <LogOut size={24} aria-hidden="true" />
              </button>
            </>
          ) : (
            <Link to="/login" className={styles.link} aria-label="Iniciar sesión o registrarse">
              <LogIn size={24} aria-hidden="true" />
              <span className={styles.userName}>Ingresar</span>
            </Link>
          )}
        </nav>
      </div>
    </header>
  );
};

export default Navbar;
