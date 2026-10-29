import React, { useEffect, useState } from 'react';
import { Link, useParams, useNavigate, useSearchParams } from 'react-router-dom';
import { api } from '../api';
import { useApp } from '../context/AppContext';
import { ProductCard } from '../components/ProductCard';

export function HomePage() {
  const [featured, setFeatured] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [trackingInput, setTrackingInput] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    Promise.all([api.getProducts({ page: 0, size: 4 }), api.getCategories()])
      .then(([prodRes, catRes]) => {
        setFeatured(prodRes.content || []);
        setCategories(catRes || []);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  const handleQuickTrack = (e) => {
    e.preventDefault();
    if (trackingInput.trim()) {
      navigate(`/track-order?tn=${encodeURIComponent(trackingInput.trim())}`);
    }
  };

  return (
    <div>
      <section className="hero-banner">
        <div className="container hero-grid">
          <div className="hero-text">
            <span className="pill-accent">⚡ Real-Time Automated Order Lifecycle</span>
            <h1>Next-Gen E-Commerce with Automated Order Tracking</h1>
            <p>
              Browse engineering hardware, smart IoT gear, and technical essentials with full transactional inventory protection and real-time multi-stage order tracking.
            </p>
            <div className="hero-cta-row">
              <Link to="/products" className="btn btn-primary btn-lg">
                Explore Catalog →
              </Link>
              <Link to="/track-order" className="btn btn-outline btn-lg">
                Track an Order
              </Link>
            </div>
          </div>

          <div className="hero-tracker-card">
            <h3>🔍 Instant Order Tracker</h3>
            <p className="muted small">
              Enter your unique tracking number (e.g., <code>TRK-...</code>) to inspect live delivery stages and timestamps.
            </p>
            <form onSubmit={handleQuickTrack} className="quick-track-form">
              <input
                type="text"
                placeholder="Enter Tracking Number (TRK-...)"
                value={trackingInput}
                onChange={(e) => setTrackingInput(e.target.value)}
                required
              />
              <button type="submit" className="btn btn-primary">
                Track Now
              </button>
            </form>
            <div className="lifecycle-mini">
              <span>PLACED</span> → <span>CONFIRMED</span> → <span>PACKED</span> → <span>SHIPPED</span> → <span>DELIVERED</span>
            </div>
          </div>
        </div>
      </section>

      <section className="container section-block">
        <div className="section-head">
          <h2>Shop by Category</h2>
          <Link to="/products" className="link-accent">View All Products →</Link>
        </div>
        <div className="category-grid">
          {categories.map((cat) => (
            <Link
              key={cat.id}
              to={`/products?categoryId=${cat.id}`}
              className="category-tile"
            >
              <h4>{cat.name}</h4>
              <p className="muted small">{cat.description}</p>
              <span className="cat-count">{cat.productCount} Product(s)</span>
            </Link>
          ))}
        </div>
      </section>

      <section className="container section-block">
        <div className="section-head">
          <h2>Featured Products</h2>
          <Link to="/products" className="link-accent">Full Catalog →</Link>
        </div>
        {loading ? (
          <div className="loading-box">Loading featured catalog...</div>
        ) : (
          <div className="product-grid">
            {featured.map((p) => (
              <ProductCard key={p.id} product={p} />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

export function ProductListPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [productsPage, setProductsPage] = useState({ content: [], page: 0, totalPages: 1, totalElements: 0 });
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);

  const [keyword, setKeyword] = useState(searchParams.get('keyword') || '');
  const [categoryId, setCategoryId] = useState(searchParams.get('categoryId') || '');
  const [minPrice, setMinPrice] = useState('');
  const [maxPrice, setMaxPrice] = useState('');
  const [sortBy, setSortBy] = useState('newest');
  const [page, setPage] = useState(0);

  useEffect(() => {
    api.getCategories().then(setCategories).catch(() => {});
  }, []);

  const fetchCatalog = () => {
    setLoading(true);
    api
      .getProducts({
        keyword: keyword.trim(),
        categoryId: categoryId || undefined,
        minPrice: minPrice || undefined,
        maxPrice: maxPrice || undefined,
        sortBy,
        page,
        size: 6,
      })
      .then((res) => setProductsPage(res))
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchCatalog();
  }, [categoryId, sortBy, page]);

  const handleFilterSubmit = (e) => {
    e.preventDefault();
    setPage(0);
    fetchCatalog();
  };

  const handleReset = () => {
    setKeyword('');
    setCategoryId('');
    setMinPrice('');
    setMaxPrice('');
    setSortBy('newest');
    setPage(0);
    setSearchParams({});
  };

  return (
    <div className="container section-block">
      <div className="page-title-bar">
        <div>
          <h1>Product Catalog</h1>
          <p className="muted">Search, filter by category &amp; price, and sort real-time inventory</p>
        </div>
        <span className="count-chip">{productsPage.totalElements} item(s) found</span>
      </div>

      <form className="filter-bar" onSubmit={handleFilterSubmit}>
        <input
          type="text"
          placeholder="Search by product name or keyword..."
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
        />
        <select
          value={categoryId}
          onChange={(e) => {
            setCategoryId(e.target.value);
            setPage(0);
          }}
        >
          <option value="">All Categories</option>
          {categories.map((c) => (
            <option key={c.id} value={c.id}>{c.name}</option>
          ))}
        </select>
        <input
          type="number"
          min="0"
          step="0.01"
          placeholder="Min Price (₹)"
          value={minPrice}
          onChange={(e) => setMinPrice(e.target.value)}
        />
        <input
          type="number"
          min="0"
          step="0.01"
          placeholder="Max Price (₹)"
          value={maxPrice}
          onChange={(e) => setMaxPrice(e.target.value)}
        />
        <select
          value={sortBy}
          onChange={(e) => {
            setSortBy(e.target.value);
            setPage(0);
          }}
        >
          <option value="newest">Sort: Newest First</option>
          <option value="price_asc">Price: Low to High</option>
          <option value="price_desc">Price: High to Low</option>
          <option value="name_asc">Name: A to Z</option>
          <option value="name_desc">Name: Z to A</option>
        </select>
        <button type="submit" className="btn btn-primary">Apply</button>
        <button type="button" className="btn btn-outline" onClick={handleReset}>Reset</button>
      </form>

      {loading ? (
        <div className="loading-box">Loading products...</div>
      ) : productsPage.content.length === 0 ? (
        <div className="empty-state">
          <h3>No matching products found</h3>
          <p className="muted">Try adjusting your search keywords or price filter criteria.</p>
          <button type="button" className="btn btn-primary" onClick={handleReset}>
            Show All Products
          </button>
        </div>
      ) : (
        <>
          <div className="product-grid">
            {productsPage.content.map((p) => (
              <ProductCard key={p.id} product={p} />
            ))}
          </div>

          {productsPage.totalPages > 1 && (
            <div className="pagination-bar">
              <button
                type="button"
                className="btn btn-outline btn-sm"
                disabled={page <= 0}
                onClick={() => setPage((p) => p - 1)}
              >
                ← Previous
              </button>
              <span>
                Page <strong>{page + 1}</strong> of <strong>{productsPage.totalPages}</strong>
              </span>
              <button
                type="button"
                className="btn btn-outline btn-sm"
                disabled={page + 1 >= productsPage.totalPages}
                onClick={() => setPage((p) => p + 1)}
              >
                Next →
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}

export function ProductDetailPage() {
  const { id } = useParams();
  const { user, refreshCart, showToast } = useApp();
  const navigate = useNavigate();
  const [product, setProduct] = useState(null);
  const [quantity, setQuantity] = useState(1);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [adding, setAdding] = useState(false);

  useEffect(() => {
    setLoading(true);
    api
      .getProductById(id)
      .then((data) => {
        setProduct(data);
        setQuantity(1);
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [id]);

  const handleAddToCart = async (e) => {
    e.preventDefault();
    if (!user) {
      showToast('Please log in to add products to your cart.', 'info');
      navigate('/login');
      return;
    }
    setAdding(true);
    try {
      await api.addToCart(product.id, Number(quantity));
      await refreshCart();
      showToast(`Added ${quantity} × "${product.name}" to your cart!`, 'success');
    } catch (err) {
      showToast(err.message, 'error');
    } finally {
      setAdding(false);
    }
  };

  if (loading) return <div className="container section-block loading-box">Loading product details...</div>;
  if (error || !product) {
    return (
      <div className="container section-block empty-state">
        <h2>Product Unavailable</h2>
        <p className="muted">{error || 'The requested product could not be found.'}</p>
        <Link to="/products" className="btn btn-primary">Return to Catalog</Link>
      </div>
    );
  }

  return (
    <div className="container section-block">
      <div className="breadcrumb">
        <Link to="/products">← Back to Product Catalog</Link>
      </div>

      <div className="product-detail-card">
        <div className="detail-image-col">
          <img
            src={product.imagePath || '/api/uploads/fallback-product.svg'}
            alt={product.name}
            onError={(e) => {
              e.target.onerror = null;
              e.target.src = '/api/uploads/fallback-product.svg';
            }}
          />
        </div>
        <div className="detail-info-col">
          <span className="category-tag">{product.categoryName}</span>
          <h1>{product.name}</h1>
          <div className="detail-price">₹{Number(product.price).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</div>

          <div className={`stock-banner ${product.available ? 'available' : 'unavailable'}`}>
            <strong>{product.availabilityMessage}</strong>
            {!product.available && (
              <p className="small">
                This item is currently out of stock and cannot be ordered until restocked by an administrator.
              </p>
            )}
          </div>

          <p className="detail-description">{product.description}</p>

          <div className="detail-meta-list">
            <div><strong>Product ID:</strong> #{product.id}</div>
            <div><strong>Added On:</strong> {product.createdAt ? new Date(product.createdAt).toLocaleDateString() : 'N/A'}</div>
          </div>

          {product.available ? (
            <form onSubmit={handleAddToCart} className="detail-buy-form">
              <label>
                Quantity:
                <input
                  type="number"
                  min="1"
                  max={product.stockQuantity}
                  value={quantity}
                  onChange={(e) => setQuantity(Math.max(1, Math.min(product.stockQuantity, Number(e.target.value))))}
                />
              </label>
              <button type="submit" className="btn btn-primary btn-lg" disabled={adding}>
                {adding ? 'Adding...' : '🛒 Add to Shopping Cart'}
              </button>
            </form>
          ) : (
            <button type="button" className="btn btn-outline btn-lg" disabled>
              Currently Out of Stock
            </button>
          )}

          {user?.role === 'ADMIN' && (
            <div style={{ marginTop: '1rem' }}>
              <Link to={`/admin/products/${product.id}/edit`} className="btn btn-outline btn-sm">
                ⚙️ Edit Product / Upload Image (Admin)
              </Link>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

