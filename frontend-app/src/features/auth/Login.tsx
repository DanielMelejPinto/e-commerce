import { useState } from 'react';
import { useNavigate, Link, useSearchParams } from 'react-router-dom';
import { usuarioService } from '../../services/usuarioService';
import { useAuth } from '../../context/AuthContext';
import styles from './Login.module.css';

const Login = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const registrado = searchParams.get('registrado');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const tokenRes = await usuarioService.login(email, password);
      const token = tokenRes.token;
      const profile = await usuarioService.obtenerPerfil(token);
      login(token, profile);
      navigate('/');
    } catch (err: unknown) {
      if (typeof err === 'object' && err !== null && 'response' in err) {
        const error = err as { response?: { status?: number } };
        if (error.response?.status === 401) {
          setError('Credenciales incorrectas');
        } else {
          setError('Error de conexión con el servidor');
        }
      } else {
        setError('Error desconocido');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className={styles.container}>
      <div className={styles.card}>
        <h1 className={styles.title}>Iniciar Sesión</h1>
        {registrado && <div className={styles.success} role="status">Registro exitoso. Por favor inicia sesión.</div>}
        {error && <div className={styles.error} role="alert">{error}</div>}
        <form onSubmit={handleSubmit} className={styles.form} aria-label="Formulario de inicio de sesión">
          <div className={styles.formGroup}>
            <label htmlFor="email">Email</label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              className={styles.input}
            />
          </div>
          <div className={styles.formGroup}>
            <label htmlFor="password">Contraseña</label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              className={styles.input}
            />
          </div>
          <button type="submit" disabled={loading} className={styles.button} aria-busy={loading}>
            {loading ? 'Ingresando...' : 'Ingresar'}
          </button>
        </form>
        <p style={{textAlign: 'center', marginTop: '1rem'}}>
          ¿No tienes cuenta? <Link to="/register">Regístrate</Link>
        </p>
      </div>
    </main>
  );
};

export default Login;
