
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
        <Link to="/" className={styles.logo}>TechStore</Link>
        <nav className={styles.nav}>
          <Link to="/cart" className={styles.link}>
            <div className={styles.cartIconWrapper}>
              <ShoppingCart size={24} />
              {items.length > 0 && (
                <span className={styles.cartBadge}>{items.reduce((acc, item) => acc + item.cantidad, 0)}</span>
              )}
            </div>
          </Link>
          
          {user ? (
            <>
              {user.rol === 'ADMIN' && (
                <Link to="/admin" className={styles.adminLink}>
                  Panel Admin
                </Link>
              )}
              <Link to="/profile" className={styles.link}>
                <User size={24} />
                <span className={styles.userName}>{user.nombre}</span>
              </Link>
              <button onClick={handleLogout} className={styles.iconButton}>
                <LogOut size={24} />
              </button>
            </>
          ) : (
            <Link to="/login" className={styles.link}>
              <LogIn size={24} />
              <span className={styles.userName}>Ingresar</span>
            </Link>
          )}
        </nav>
      </div>
    </header>
  );
};

export default Navbar;
