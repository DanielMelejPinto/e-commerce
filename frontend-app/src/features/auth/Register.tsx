import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { usuarioService } from '../../services/usuarioService';
import { extractErrorMessage } from '../../utils/errorHelper';
import styles from './Login.module.css';

const Register = () => {
  const [nombre, setNombre] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    
    if (password.length < 8 || password.length > 72) {
      setError('La contraseña debe tener entre 8 y 72 caracteres');
      return;
    }
    
    // Check byte size for BCrypt limitation (72 bytes)
    const byteSize = new Blob([password]).size;
    if (byteSize > 72) {
      setError('La contraseña excede el límite máximo de 72 bytes permitido');
      return;
    }

    setLoading(true);
    try {
      await usuarioService.registro(nombre, email, password);
      // Registro exitoso, redirigimos a login
      navigate('/login?registrado=true');
    } catch (err: unknown) {
      setError(extractErrorMessage(err, 'Error al registrar el usuario'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className={styles.container}>
      <div className={styles.card}>
        <h1 className={styles.title}>Crear Cuenta</h1>
        {error && <div className={styles.error} role="alert">{error}</div>}
        <form onSubmit={handleSubmit} className={styles.form} aria-label="Formulario de registro">
          <div className={styles.formGroup}>
            <label htmlFor="nombre">Nombre completo</label>
            <input
              id="nombre"
              type="text"
              value={nombre}
              onChange={(e) => setNombre(e.target.value)}
              required
              className={styles.input}
            />
          </div>
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
            {loading ? 'Registrando...' : 'Registrarse'}
          </button>
        </form>
        <p style={{textAlign: 'center', marginTop: '1rem'}}>
          ¿Ya tienes cuenta? <Link to="/login">Inicia sesión</Link>
        </p>
      </div>
    </main>
  );
};

export default Register;
