import os

base = "frontend-app/src"

dirs = [
    "features/auth",
    "features/products",
    "features/cart",
    "features/orders",
    "features/admin",
    "components",
    "layouts",
    "api",
    "context"
]

for d in dirs:
    os.makedirs(os.path.join(base, d), exist_ok=True)

# 1. API Client setup
api_code = """import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api', // Gateway o URL base
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default api;
"""

with open(os.path.join(base, "api/axios.ts"), "w") as f:
    f.write(api_code)

# 2. Main App Router Setup
app_code = """import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import MainLayout from './layouts/MainLayout';
import Home from './features/products/Home';

const App = () => {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<MainLayout />}>
          <Route index element={<Home />} />
          {/* Other routes will be added here */}
        </Route>
      </Routes>
    </BrowserRouter>
  );
};

export default App;
"""

with open(os.path.join(base, "App.tsx"), "w") as f:
    f.write(app_code)

# 3. MainLayout
layout_code = """import React from 'react';
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
"""

with open(os.path.join(base, "layouts/MainLayout.tsx"), "w") as f:
    f.write(layout_code)

layout_css = """.container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.main {
  flex: 1;
  padding: 2rem;
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
}

.footer {
  text-align: center;
  padding: 1.5rem;
  background-color: #f8f9fa;
  border-top: 1px solid #e9ecef;
}
"""

with open(os.path.join(base, "layouts/MainLayout.module.css"), "w") as f:
    f.write(layout_css)

# 4. Navbar
navbar_code = """import React from 'react';
import { Link } from 'react-router-dom';
import { ShoppingCart, User } from 'lucide-react';
import styles from './Navbar.module.css';

const Navbar = () => {
  return (
    <header className={styles.header}>
      <div className={styles.navContainer}>
        <Link to="/" className={styles.logo}>TechStore</Link>
        <nav className={styles.nav}>
          <Link to="/cart" className={styles.link}>
            <ShoppingCart size={24} />
          </Link>
          <Link to="/profile" className={styles.link}>
            <User size={24} />
          </Link>
        </nav>
      </div>
    </header>
  );
};

export default Navbar;
"""

with open(os.path.join(base, "components/Navbar.tsx"), "w") as f:
    f.write(navbar_code)

navbar_css = """.header {
  background-color: #ffffff;
  border-bottom: 1px solid #e9ecef;
  padding: 1rem 2rem;
  position: sticky;
  top: 0;
  z-index: 100;
}

.navContainer {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.logo {
  font-size: 1.5rem;
  font-weight: bold;
  color: #212529;
  text-decoration: none;
}

.nav {
  display: flex;
  gap: 1.5rem;
  align-items: center;
}

.link {
  color: #495057;
  text-decoration: none;
  display: flex;
  align-items: center;
  transition: color 0.2s;
}

.link:hover {
  color: #228be6;
}
"""

with open(os.path.join(base, "components/Navbar.module.css"), "w") as f:
    f.write(navbar_css)

# 5. Home
home_code = """import React, { useEffect, useState } from 'react';
import api from '../../api/axios';
import styles from './Home.module.css';

const Home = () => {
  const [products, setProducts] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Para simplificar, asumimos que Vite usará un proxy si la API está en localhost:8080
    // O configuramos la URL en Vite
    const fetchProducts = async () => {
      try {
        const response = await api.get('http://localhost:8080/api/productos');
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
    return <div>Cargando productos...</div>;
  }

  return (
    <div className={styles.container}>
      <h1>Catálogo de Productos</h1>
      <div className={styles.grid}>
        {products.map((product) => (
          <div key={product.id} className={styles.card}>
            {product.imagenUrl && (
              <img src={product.imagenUrl} alt={product.nombre} className={styles.image} />
            )}
            <h2 className={styles.title}>{product.nombre}</h2>
            <p className={styles.price}>${product.precio.toFixed(2)}</p>
            <button className={styles.button}>Ver Detalle</button>
          </div>
        ))}
      </div>
    </div>
  );
};

export default Home;
"""

with open(os.path.join(base, "features/products/Home.tsx"), "w") as f:
    f.write(home_code)

home_css = """.container {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 2rem;
}

.card {
  border: 1px solid #e9ecef;
  border-radius: 8px;
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  background-color: white;
  transition: transform 0.2s, box-shadow 0.2s;
}

.card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.image {
  width: 100%;
  height: 200px;
  object-fit: cover;
  border-radius: 4px;
}

.title {
  font-size: 1.2rem;
  margin: 0;
  color: #343a40;
}

.price {
  font-size: 1.25rem;
  font-weight: bold;
  color: #2b8a3e;
  margin: 0;
}

.button {
  background-color: #228be6;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 0.75rem;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s;
  margin-top: auto;
}

.button:hover {
  background-color: #1c7ed6;
}
"""

with open(os.path.join(base, "features/products/Home.module.css"), "w") as f:
    f.write(home_css)

print("Base setup written")
