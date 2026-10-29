import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useApp } from '../context/AppContext';
import { api } from '../api';

export function ProductCard({ product }) {
  const { user, refreshCart, showToast } = useApp();
  const [adding, setAdding] = useState(false);
  const navigate = useNavigate();

  const handleAdd = async () => {
    if (!user) {
      showToast('Please log in to add items to your shopping cart.', 'info');
      navigate('/login');
      return;
    }
    setAdding(true);
    try {
      await api.addToCart(product.id, 1);
      await refreshCart();
      showToast(`Added "${product.name}" to cart!`, 'success');
    } catch (err) {
      showToast(err.message, 'error');
    } finally {
      setAdding(false);
    }
  };

  const imgSrc = product.imagePath || '/api/uploads/fallback-product.svg';

  return (
    <div className="product-card">
      <Link to={`/products/${product.id}`} className="product-img-wrap">
        <img
          src={imgSrc}
          alt={product.name}
          onError={(e) => {
            e.target.onerror = null;
            e.target.src = '/api/uploads/fallback-product.svg';
          }}
        />
        <span className="category-tag">{product.categoryName || 'General'}</span>
      </Link>

      <div className="product-body">
        <Link to={`/products/${product.id}`} className="product-title">
          {product.name}
        </Link>
        <p className="product-desc">{product.description}</p>

        <div className="product-stock-row">
          <span className={`stock-pill ${product.available ? (product.stockQuantity <= 5 ? 'low' : 'in') : 'out'}`}>
            {product.availabilityMessage}
          </span>
        </div>

        <div className="product-footer">
          <div className="product-price">₹{Number(product.price).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</div>
          <div className="product-actions">
            <Link to={`/products/${product.id}`} className="btn btn-outline btn-sm">
              Details
            </Link>
            <button
              type="button"
              className="btn btn-primary btn-sm"
              disabled={!product.available || adding}
              onClick={handleAdd}
            >
              {adding ? 'Adding...' : product.available ? '+ Add to Cart' : 'Unavailable'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

