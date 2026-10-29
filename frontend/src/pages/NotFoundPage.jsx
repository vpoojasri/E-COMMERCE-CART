import React from 'react';
import { Link } from 'react-router-dom';

export function NotFoundPage() {
  return (
    <div className="container section-block empty-state">
      <div style={{ fontSize: '3.5rem', marginBottom: '0.5rem' }}>🧭</div>
      <h1>404 — Page Not Found</h1>
      <p className="muted">
        The page or resource you requested does not exist. Use the navigation bar or buttons below to return to the store.
      </p>
      <div className="demo-btn-row" style={{ justifyContent: 'center', marginTop: '1rem' }}>
        <Link to="/" className="btn btn-primary">Return Home</Link>
        <Link to="/products" className="btn btn-outline">Browse Catalog</Link>
        <Link to="/track-order" className="btn btn-outline">Track an Order</Link>
      </div>
    </div>
  );
}

