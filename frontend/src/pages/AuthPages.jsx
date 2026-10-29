import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useApp } from '../context/AppContext';

export function LoginPage() {
  const { handleLogin } = useApp();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const onSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      const user = await handleLogin({ email, password });
      if (user.role === 'ADMIN') {
        navigate('/admin');
      } else {
        navigate('/products');
      }
    } catch (err) {
      setError(err.message || 'Invalid login credentials.');
    } finally {
      setSubmitting(false);
    }
  };

  const fillDemo = (role) => {
    if (role === 'admin') {
      setEmail('admin@ecommerce.edu');
      setPassword('Admin@123');
    } else {
      setEmail('customer@ecommerce.edu');
      setPassword('Customer@123');
    }
  };

  return (
    <div className="container section-block auth-container">
      <div className="auth-card">
        <h2>Sign In to Your Account</h2>
        <p className="muted">Access your shopping cart, live order tracking, or administrator console.</p>

        {error && <div className="alert alert-error">{error}</div>}

        <form onSubmit={onSubmit} className="form-stack">
          <div className="form-group">
            <label htmlFor="login-email">Email Address</label>
            <input
              id="login-email"
              type="email"
              required
              placeholder="you@university.edu"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </div>

          <div className="form-group">
            <label htmlFor="login-password">Password</label>
            <input
              id="login-password"
              type="password"
              required
              placeholder="Enter your password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>

          <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>
            {submitting ? 'Signing In...' : 'Sign In'}
          </button>
        </form>

        <div className="demo-credentials-box">
          <p className="small muted"><strong>College Lab Quick Fill (Configured via Environment/Seeder):</strong></p>
          <div className="demo-btn-row">
            <button type="button" className="btn btn-outline btn-sm" onClick={() => fillDemo('customer')}>
              Fill Demo Customer
            </button>
            <button type="button" className="btn btn-outline btn-sm" onClick={() => fillDemo('admin')}>
              Fill Demo Admin
            </button>
          </div>
        </div>

        <p className="auth-switch">
          New customer? <Link to="/register">Create an account here</Link>
        </p>
      </div>
    </div>
  );
}

export function RegisterPage() {
  const { handleRegister } = useApp();
  const navigate = useNavigate();
  const [form, setForm] = useState({
    fullName: '',
    email: '',
    password: '',
    phone: '',
    shippingAddress: '',
  });
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const onSubmit = async (e) => {
    e.preventDefault();
    setError('');
    if (form.password.length < 6) {
      setError('Password must be at least 6 characters long.');
      return;
    }
    setSubmitting(true);
    try {
      await handleRegister(form);
      navigate('/products');
    } catch (err) {
      setError(err.message || 'Registration failed.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="container section-block auth-container">
      <div className="auth-card">
        <h2>Create Customer Account</h2>
        <p className="muted">Register to place orders and track deliveries automatically.</p>

        {error && <div className="alert alert-error">{error}</div>}

        <form onSubmit={onSubmit} className="form-stack">
          <div className="form-group">
            <label>Full Name *</label>
            <input
              type="text"
              required
              minLength={2}
              placeholder="e.g., Rohan Verma"
              value={form.fullName}
              onChange={(e) => setForm({ ...form, fullName: e.target.value })}
            />
          </div>

          <div className="form-group">
            <label>Email Address *</label>
            <input
              type="email"
              required
              placeholder="rohan@university.edu"
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
            />
          </div>

          <div className="form-group">
            <label>Password (min. 6 characters) *</label>
            <input
              type="password"
              required
              minLength={6}
              placeholder="Create a strong password"
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
            />
          </div>

          <div className="form-group">
            <label>Contact Phone</label>
            <input
              type="text"
              placeholder="+1-555-019-2834"
              value={form.phone}
              onChange={(e) => setForm({ ...form, phone: e.target.value })}
            />
          </div>

          <div className="form-group">
            <label>Default Shipping Address</label>
            <textarea
              rows={2}
              placeholder="Street, Building, City, Postal Code"
              value={form.shippingAddress}
              onChange={(e) => setForm({ ...form, shippingAddress: e.target.value })}
            />
          </div>

          <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>
            {submitting ? 'Creating Account...' : 'Register Account'}
          </button>
        </form>

        <p className="auth-switch">
          Already have an account? <Link to="/login">Sign in here</Link>
        </p>
      </div>
    </div>
  );
}

