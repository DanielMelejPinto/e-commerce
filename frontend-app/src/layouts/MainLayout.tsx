
import { Outlet } from 'react-router-dom';
import Navbar from '../components/Navbar';
import styles from './MainLayout.module.css';

const MainLayout = () => {
  return (
    <div className={styles.container}>
      <Navbar />
      <main className={styles.main}>
        <Outlet />
      </main>
      <footer className={styles.footer}>
        <p>© 2023 TechStore. Todos los derechos reservados.</p>
      </footer>
    </div>
  );
};

export default MainLayout;
