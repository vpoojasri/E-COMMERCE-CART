import React, { useState, useEffect, useCallback } from 'react';
import { Link, NavLink, useNavigate, useParams } from 'react-router-dom';
import { useApp } from '../context/AppContext';
import { api } from '../api';
import { StatusBadge, TrackingTimeline } from '../components/TrackingTimeline';

function AdminNavBar() {
  return null;
}

function RequireAdminGuard({ children }) {
  const { user, authLoading } = useApp();
  if (authLoading) return <div className="container section-block loading-box">Verifying administrator privileges...</div>;
  if (!user || user.role !== 'ADMIN') {
    return (
      <div className="container section-block empty-state">
        <h2>🛡️ Administrator Access Required</h2>
        <p className="muted">This area is restricted to authorized administrators. Every endpoint is protected by server-side role checks.</p>
        <Link to="/login" className="btn btn-primary">Sign In as Administrator</Link>
      </div>
    );
  }
  return children;
}

export function AdminDashboardPage() {
  const { showToast } = useApp();
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [toggling, setToggling] = useState(false);

  const loadStats = useCallback(() => {
    setLoading(true);
    api
      .getAdminDashboard()
      .then(setStats)
      .catch((err) => showToast(err.message, 'error'))
      .finally(() => setLoading(false));
  }, [showToast]);

  useEffect(() => {
    loadStats();
  }, [loadStats]);

  const handleToggleAutomation = async () => {
    if (!stats) return;
    setToggling(true);
    try {
      const res = await api.toggleAutomation(!stats.automationEnabled);
      showToast(res.message, 'info');
      loadStats();
    } catch (err) {
      showToast(err.message, 'error');
    } finally {
      setToggling(false);
    }
  };

  const handleStepSimulationNow = async () => {
    setToggling(true);
    try {
      const res = await api.stepAutomationNow();
      showToast(res.message, 'success');
      loadStats();
    } catch (err) {
      showToast(err.message, 'error');
    } finally {
      setToggling(false);
    }
  };

  return (
    <RequireAdminGuard>
      <div className="container section-block">
        <AdminNavBar />

        <div className="page-title-bar">
          <div>
            <h1>Administrator Control Center</h1>
            <p className="muted">Real-time store metrics, low-stock alerts, Hibernate L2 cache telemetry, and automated tracking engine</p>
          </div>
          <button type="button" className="btn btn-outline btn-sm" onClick={loadStats}>
            ↻ Refresh Metrics
          </button>
        </div>

        {loading || !stats ? (
          <div className="loading-box">Loading administrator telemetry...</div>
        ) : (
          <>
            <div className="kpi-grid">
              <div className="kpi-card">
                <span className="kpi-label">Total Products</span>
                <strong className="kpi-val">{stats.totalProducts}</strong>
              </div>
              <div className="kpi-card">
                <span className="kpi-label">Registered Customers</span>
                <strong className="kpi-val">{stats.totalCustomers}</strong>
              </div>
              <div className="kpi-card">
                <span className="kpi-label">Total Orders</span>
                <strong className="kpi-val">{stats.totalOrders}</strong>
              </div>
              <div className="kpi-card">
                <span className="kpi-label">Pending / Processing</span>
                <strong className="kpi-val warn">{stats.pendingOrders}</strong>
              </div>
              <div className="kpi-card">
                <span className="kpi-label">Shipped / In Transit</span>
                <strong className="kpi-val info">{stats.shippedOrders}</strong>
              </div>
              <div className="kpi-card">
                <span className="kpi-label">Delivered Orders</span>
                <strong className="kpi-val ok">{stats.deliveredOrders}</strong>
              </div>
            </div>

            <div className="dashboard-two-col" style={{ marginTop: '1.75rem' }}>
              <div className="card-panel">
                <h3>⚙️ Automated Order Tracking Simulation Engine</h3>
                <p className="muted small">
                  Advances active orders through legal stages (<code>PLACED → CONFIRMED → PACKED → SHIPPED → OUT_FOR_DELIVERY → DELIVERED</code>). Disabled by default in normal use.
                </p>
                <div className="automation-status-row">
                  <span>
                    Background Scheduler Status:{' '}
                    <strong className={stats.automationEnabled ? 'text-ok' : 'text-muted'}>
                      {stats.automationEnabled ? 'ENABLED (Every 25s)' : 'DISABLED (Default)'}
                    </strong>
                  </span>
                </div>
                <div className="demo-btn-row" style={{ marginTop: '1rem' }}>
                  <button
                    type="button"
                    className={`btn ${stats.automationEnabled ? 'btn-danger-outline' : 'btn-primary'}`}
                    disabled={toggling}
                    onClick={handleToggleAutomation}
                  >
                    {stats.automationEnabled ? '⏸ Disable Auto-Scheduler' : '▶ Enable Auto-Scheduler'}
                  </button>
                  <button
                    type="button"
                    className="btn btn-outline"
                    disabled={toggling}
                    onClick={handleStepSimulationNow}
                  >
                    ⏭ Advance Active Orders 1 Stage Now
                  </button>
                </div>
              </div>

              <div className="card-panel">
                <h3>⚡ Hibernate ORM Second-Level &amp; Query Cache Telemetry</h3>
                <p className="muted small">
                  Live JCache/Caffeine statistics from <code>SessionFactory.getStatistics()</code>
                </p>
                {stats.cacheMetrics && (
                  <div className="cache-stats-grid">
                    <div>
                      <span>L2 Cache Enabled:</span>{' '}
                      <strong>{stats.cacheMetrics.secondLevelCacheEnabled ? 'Yes' : 'No'}</strong>
                    </div>
                    <div>
                      <span>Query Cache Enabled:</span>{' '}
                      <strong>{stats.cacheMetrics.queryCacheEnabled ? 'Yes' : 'No'}</strong>
                    </div>
                    <div>
                      <span>L2 Cache Hits / Misses:</span>{' '}
                      <strong>{stats.cacheMetrics.secondLevelCacheHitCount} / {stats.cacheMetrics.secondLevelCacheMissCount}</strong>
                    </div>
                    <div>
                      <span>L2 Cache Puts:</span>{' '}
                      <strong>{stats.cacheMetrics.secondLevelCachePutCount}</strong>
                    </div>
                    <div>
                      <span>Query Cache Hits / Misses:</span>{' '}
                      <strong>{stats.cacheMetrics.queryCacheHitCount} / {stats.cacheMetrics.queryCacheMissCount}</strong>
                    </div>
                    <div>
                      <span>Active Revenue:</span>{' '}
                      <strong>₹{Number(stats.totalRevenue || 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</strong>
                    </div>
                  </div>
                )}
              </div>
            </div>

            <div className="dashboard-two-col" style={{ marginTop: '1.75rem' }}>
              <div className="card-panel">
                <div className="panel-head">
                  <h3>⚠️ Low-Stock &amp; Out-of-Stock Alerts (≤ 5 units)</h3>
                  <Link to="/admin/products" className="link-accent small">Manage Inventory →</Link>
                </div>
                {stats.lowStockProducts?.length === 0 ? (
                  <p className="muted">All active products are well-stocked above threshold.</p>
                ) : (
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>ID</th>
                        <th>Product</th>
                        <th>Stock</th>
                        <th>Action</th>
                      </tr>
                    </thead>
                    <tbody>
                      {stats.lowStockProducts.map((p) => (
                        <tr key={p.id}>
                          <td>#{p.id}</td>
                          <td>{p.name}</td>
                          <td>
                            <span className={`stock-pill ${p.stockQuantity === 0 ? 'out' : 'low'}`}>
                              {p.stockQuantity} left
                            </span>
                          </td>
                          <td>
                            <Link to={`/admin/products/${p.id}/edit`} className="btn btn-outline btn-sm">
                              Restock
                            </Link>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </div>

              <div className="card-panel">
                <div className="panel-head">
                  <h3>🕒 Recent Customer Orders</h3>
                  <Link to="/admin/orders" className="link-accent small">Manage All Orders →</Link>
                </div>
                {stats.recentOrders?.length === 0 ? (
                  <p className="muted">No orders recorded yet.</p>
                ) : (
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>Order #</th>
                        <th>Customer</th>
                        <th>Total</th>
                        <th>Status</th>
                        <th>Manage</th>
                      </tr>
                    </thead>
                    <tbody>
                      {stats.recentOrders.map((o) => (
                        <tr key={o.id}>
                          <td><strong>{o.orderNumber}</strong></td>
                          <td>{o.customerName}</td>
                          <td>₹{Number(o.totalAmount).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</td>
                          <td><StatusBadge status={o.currentStatus} /></td>
                          <td>
                            <Link to="/admin/orders" className="btn btn-outline btn-sm">
                              Update
                            </Link>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </div>
            </div>
          </>
        )}
      </div>
    </RequireAdminGuard>
  );
}

export function AdminProductsPage() {
  const { showToast } = useApp();
  const [productsPage, setProductsPage] = useState({ content: [], page: 0, totalPages: 1, totalElements: 0 });
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [confirmDeleteProduct, setConfirmDeleteProduct] = useState(null);

  const loadProducts = useCallback(() => {
    setLoading(true);
    api
      .getProducts({ activeOnly: false, page, size: 12 })
      .then(setProductsPage)
      .catch((err) => showToast(err.message, 'error'))
      .finally(() => setLoading(false));
  }, [page, showToast]);

  useEffect(() => {
    loadProducts();
  }, [loadProducts]);

  const handleConfirmDelete = async () => {
    if (!confirmDeleteProduct) return;
    try {
      const res = await api.deleteProduct(confirmDeleteProduct.id);
      showToast(res.message || 'Product deleted.', 'info');
      setConfirmDeleteProduct(null);
      loadProducts();
    } catch (err) {
      showToast(err.message, 'error');
    }
  };

  return (
    <RequireAdminGuard>
      <div className="container section-block">
        <AdminNavBar />

        <div className="page-title-bar">
          <div>
            <h1>Product &amp; Inventory Management</h1>
            <p className="muted">Create, edit, upload images, adjust stock quantities, or remove products</p>
          </div>
          <Link to="/admin/products/new" className="btn btn-primary">
            ➕ Add New Product
          </Link>
        </div>

        {confirmDeleteProduct && (
          <div className="modal-backdrop">
            <div className="modal-card">
              <h3>Confirm Product Removal</h3>
              <p className="muted">
                Are you sure you want to delete/deactivate <strong>{confirmDeleteProduct.name}</strong>?
              </p>
              <div className="modal-actions">
                <button type="button" className="btn btn-outline" onClick={() => setConfirmDeleteProduct(null)}>
                  Cancel
                </button>
                <button type="button" className="btn btn-danger" onClick={handleConfirmDelete}>
                  Confirm Delete
                </button>
              </div>
            </div>
          </div>
        )}

        {loading ? (
          <div className="loading-box">Loading product inventory...</div>
        ) : (
          <div className="card-panel">
            <div className="table-responsive">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Image</th>
                    <th>Name</th>
                    <th>Category</th>
                    <th>Price</th>
                    <th>Stock</th>
                    <th>Active</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {productsPage.content.map((p) => (
                    <tr key={p.id}>
                      <td>#{p.id}</td>
                      <td>
                        <img
                          src={p.imagePath || '/api/uploads/fallback-product.svg'}
                          alt={p.name}
                          className="table-thumb"
                        />
                      </td>
                      <td><strong>{p.name}</strong></td>
                      <td>{p.categoryName}</td>
                      <td>₹{Number(p.price).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</td>
                      <td>
                        <span className={`stock-pill ${p.stockQuantity <= 0 ? 'out' : p.stockQuantity <= 5 ? 'low' : 'in'}`}>
                          {p.stockQuantity} units
                        </span>
                      </td>
                      <td>{p.active ? '✅ Active' : '🚫 Inactive'}</td>
                      <td>
                        <div className="table-btn-group">
                          <Link to={`/admin/products/${p.id}/edit`} className="btn btn-outline btn-sm">
                            Edit / Image
                          </Link>
                          <button
                            type="button"
                            className="btn btn-danger-outline btn-sm"
                            onClick={() => setConfirmDeleteProduct(p)}
                          >
                            Delete
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
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
                <span>Page {page + 1} of {productsPage.totalPages}</span>
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
          </div>
        )}
      </div>
    </RequireAdminGuard>
  );
}

export function AdminProductFormPage() {
  const { id } = useParams();
  const isEdit = Boolean(id);
  const navigate = useNavigate();
  const { showToast } = useApp();

  const [categories, setCategories] = useState([]);
  const [form, setForm] = useState({
    name: '',
    description: '',
    price: '',
    stockQuantity: 10,
    categoryId: '',
    imagePath: '',
    active: true,
  });
  const [selectedFile, setSelectedFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    api.getCategories().then((cats) => {
      setCategories(cats || []);
      if (!isEdit && cats?.length > 0) {
        setForm((prev) => ({ ...prev, categoryId: cats[0].id }));
      }
    });

    if (isEdit) {
      api
        .getProductById(id)
        .then((p) => {
          setForm({
            name: p.name,
            description: p.description,
            price: p.price,
            stockQuantity: p.stockQuantity,
            categoryId: p.categoryId,
            imagePath: p.imagePath || '',
            active: p.active !== false,
          });
        })
        .catch((err) => setError(err.message));
    }
  }, [id, isEdit]);

  const handleImageUpload = async () => {
    if (!selectedFile) {
      showToast('Please choose a JPEG, PNG, or WebP file first.', 'error');
      return;
    }
    setUploading(true);
    setError('');
    try {
      if (isEdit) {
        const updated = await api.uploadProductImage(id, selectedFile);
        setForm((prev) => ({ ...prev, imagePath: updated.imagePath }));
        showToast('Product image uploaded and linked!', 'success');
      } else {
        const res = await api.uploadStandaloneImage(selectedFile);
        setForm((prev) => ({ ...prev, imagePath: res.imagePath }));
        showToast('Image uploaded! Save the product to finalize.', 'success');
      }
      setSelectedFile(null);
    } catch (err) {
      setError(err.message || 'Image upload failed.');
    } finally {
      setUploading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError('');
    try {
      let imagePathToUse = form.imagePath;
      if (selectedFile) {
        const uploadRes = await api.uploadStandaloneImage(selectedFile);
        imagePathToUse = uploadRes.imagePath;
      }

      const payload = {
        name: form.name,
        description: form.description,
        price: Number(form.price),
        stockQuantity: Number(form.stockQuantity),
        categoryId: Number(form.categoryId),
        imagePath: imagePathToUse,
        active: Boolean(form.active),
      };

      if (isEdit) {
        await api.updateProduct(id, payload);
        showToast('Product updated successfully!', 'success');
      } else {
        await api.createProduct(payload);
        showToast('New product created successfully!', 'success');
      }
      navigate('/admin/products');
    } catch (err) {
      setError(err.message || 'Failed to save product.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <RequireAdminGuard>
      <div className="container section-block">
        <AdminNavBar />

        <div className="page-title-bar">
          <div>
            <h1>{isEdit ? `Edit Product #${id}` : 'Add New Product'}</h1>
            <p className="muted">Configure product specifications, stock levels, and secure multipart image upload</p>
          </div>
          <Link to="/admin/products" className="btn btn-outline">← Back to Products</Link>
        </div>

        {error && <div className="alert alert-error">{error}</div>}

        <div className="dashboard-two-col">
          <form onSubmit={handleSubmit} className="card-panel form-stack">
            <div className="form-group">
              <label>Product Name *</label>
              <input
                type="text"
                required
                minLength={2}
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
              />
            </div>

            <div className="form-group">
              <label>Category *</label>
              <select
                required
                value={form.categoryId}
                onChange={(e) => setForm({ ...form, categoryId: e.target.value })}
              >
                <option value="" disabled>Select Category</option>
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>{c.name}</option>
                ))}
              </select>
            </div>

            <div className="form-row-two">
              <div className="form-group">
                <label>Price (₹) *</label>
                <input
                  type="number"
                  step="0.01"
                  min="0.01"
                  required
                  value={form.price}
                  onChange={(e) => setForm({ ...form, price: e.target.value })}
                />
              </div>

              <div className="form-group">
                <label>Stock Quantity *</label>
                <input
                  type="number"
                  min="0"
                  required
                  value={form.stockQuantity}
                  onChange={(e) => setForm({ ...form, stockQuantity: e.target.value })}
                />
              </div>
            </div>

            <div className="form-group">
              <label>Detailed Description *</label>
              <textarea
                rows={4}
                required
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
              />
            </div>

            <div className="form-group">
              <label className="checkbox-label">
                <input
                  type="checkbox"
                  checked={form.active}
                  onChange={(e) => setForm({ ...form, active: e.target.checked })}
                />
                Product is Active and Visible in Storefront
              </label>
            </div>

            <button type="submit" className="btn btn-primary btn-lg" disabled={saving}>
              {saving ? 'Saving Product...' : isEdit ? 'Save Product Changes' : 'Create Product'}
            </button>
          </form>

          <div className="card-panel">
            <h3>🖼️ Secure Multipart Product Image Upload</h3>
            <p className="muted small">
              Permitted formats: <strong>JPEG, PNG, WebP</strong> (Max size: 5 MB). Validated by extension, MIME type, and binary magic signature.
            </p>

            <div className="upload-preview-box">
              <img
                src={form.imagePath || '/api/uploads/fallback-product.svg'}
                alt="Product preview"
                onError={(e) => {
                  e.target.onerror = null;
                  e.target.src = '/api/uploads/fallback-product.svg';
                }}
              />
            </div>

            <div className="form-stack" style={{ marginTop: '1rem' }}>
              <div className="form-group">
                <label>Select Image File (JPEG / PNG / WebP)</label>
                <input
                  type="file"
                  accept="image/jpeg,image/png,image/webp"
                  onChange={(e) => setSelectedFile(e.target.files?.[0] || null)}
                />
              </div>

              <button
                type="button"
                className="btn btn-outline"
                disabled={!selectedFile || uploading}
                onClick={handleImageUpload}
              >
                {uploading ? 'Uploading & Verifying Signature...' : '⬆️ Upload Image Now'}
              </button>

              <div className="form-group">
                <label>Current Image Path / Reference</label>
                <input
                  type="text"
                  value={form.imagePath}
                  onChange={(e) => setForm({ ...form, imagePath: e.target.value })}
                  placeholder="/api/uploads/..."
                />
              </div>
            </div>
          </div>
        </div>
      </div>
    </RequireAdminGuard>
  );
}

export function AdminCategoriesPage() {
  const { showToast } = useApp();
  const [categories, setCategories] = useState([]);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [editingId, setEditingId] = useState(null);
  const [confirmDeleteCat, setConfirmDeleteCat] = useState(null);

  const loadCategories = useCallback(() => {
    api.getCategories().then(setCategories).catch(() => {});
  }, []);

  useEffect(() => {
    loadCategories();
  }, [loadCategories]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (editingId) {
        await api.updateCategory(editingId, { name, description });
        showToast('Category updated!', 'success');
      } else {
        await api.createCategory({ name, description });
        showToast('Category created!', 'success');
      }
      setName('');
      setDescription('');
      setEditingId(null);
      loadCategories();
    } catch (err) {
      showToast(err.message, 'error');
    }
  };

  const handleConfirmDelete = async () => {
    if (!confirmDeleteCat) return;
    try {
      await api.deleteCategory(confirmDeleteCat.id);
      showToast('Category deleted.', 'info');
      setConfirmDeleteCat(null);
      loadCategories();
    } catch (err) {
      showToast(err.message, 'error');
      setConfirmDeleteCat(null);
    }
  };

  return (
    <RequireAdminGuard>
      <div className="container section-block">
        <AdminNavBar />

        <h1>Category Management</h1>
        <p className="muted">Categories are cached in Hibernate Second-Level Cache (<code>categoriesCache</code>) with automatic invalidation on update.</p>

        {confirmDeleteCat && (
          <div className="modal-backdrop">
            <div className="modal-card">
              <h3>Delete Category</h3>
              <p className="muted">Are you sure you want to delete category <strong>{confirmDeleteCat.name}</strong>?</p>
              <div className="modal-actions">
                <button type="button" className="btn btn-outline" onClick={() => setConfirmDeleteCat(null)}>Cancel</button>
                <button type="button" className="btn btn-danger" onClick={handleConfirmDelete}>Delete</button>
              </div>
            </div>
          </div>
        )}

        <div className="dashboard-two-col" style={{ marginTop: '1.5rem' }}>
          <form onSubmit={handleSubmit} className="card-panel form-stack">
            <h3>{editingId ? `Edit Category #${editingId}` : 'Create New Category'}</h3>
            <div className="form-group">
              <label>Category Name *</label>
              <input
                type="text"
                required
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="e.g., Robotics & Embedded Systems"
              />
            </div>
            <div className="form-group">
              <label>Description</label>
              <textarea
                rows={3}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Brief summary of products in this category"
              />
            </div>
            <div className="demo-btn-row">
              <button type="submit" className="btn btn-primary">
                {editingId ? 'Update Category' : 'Add Category'}
              </button>
              {editingId && (
                <button
                  type="button"
                  className="btn btn-outline"
                  onClick={() => {
                    setEditingId(null);
                    setName('');
                    setDescription('');
                  }}
                >
                  Cancel
                </button>
              )}
            </div>
          </form>

          <div className="card-panel">
            <h3>Existing Categories ({categories.length})</h3>
            <table className="data-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Name</th>
                  <th>Products</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {categories.map((c) => (
                  <tr key={c.id}>
                    <td>#{c.id}</td>
                    <td>
                      <strong>{c.name}</strong>
                      <div className="muted small">{c.description}</div>
                    </td>
                    <td>{c.productCount}</td>
                    <td>
                      <div className="table-btn-group">
                        <button
                          type="button"
                          className="btn btn-outline btn-sm"
                          onClick={() => {
                            setEditingId(c.id);
                            setName(c.name);
                            setDescription(c.description || '');
                          }}
                        >
                          Edit
                        </button>
                        <button
                          type="button"
                          className="btn btn-danger-outline btn-sm"
                          onClick={() => setConfirmDeleteCat(c)}
                        >
                          Delete
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </RequireAdminGuard>
  );
}

export function AdminOrdersPage() {
  const { showToast } = useApp();
  const [ordersPage, setOrdersPage] = useState({ content: [], page: 0, totalPages: 1, totalElements: 0 });
  const [statusFilter, setStatusFilter] = useState('');
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);

  const [selectedOrder, setSelectedOrder] = useState(null);
  const [nextStatus, setNextStatus] = useState('');
  const [statusDesc, setStatusDesc] = useState('');
  const [locationNote, setLocationNote] = useState('');
  const [updating, setUpdating] = useState(false);

  const loadOrders = useCallback(() => {
    setLoading(true);
    api
      .getAdminOrders(statusFilter, page, 12)
      .then(setOrdersPage)
      .catch((err) => showToast(err.message, 'error'))
      .finally(() => setLoading(false));
  }, [statusFilter, page, showToast]);

  useEffect(() => {
    loadOrders();
  }, [loadOrders]);

  const openOrderInspector = (order) => {
    setSelectedOrder(order);
    const allowed = Array.from(order.allowedNextStatuses || []);
    // Pick the natural next stage if available
    const preferredOrder = ['CONFIRMED', 'PACKED', 'SHIPPED', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED'];
    const defaultNext = preferredOrder.find((s) => allowed.includes(s)) || '';
    setNextStatus(defaultNext);
    setStatusDesc('');
    setLocationNote('');
  };

  const handleStatusUpdate = async (e) => {
    e.preventDefault();
    if (!selectedOrder || !nextStatus) return;
    setUpdating(true);
    try {
      const updated = await api.updateOrderStatus(selectedOrder.id, {
        status: nextStatus,
        description: statusDesc,
        locationNote,
      });
      setSelectedOrder(updated);
      const allowed = Array.from(updated.allowedNextStatuses || []);
      const preferredOrder = ['CONFIRMED', 'PACKED', 'SHIPPED', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED'];
      setNextStatus(preferredOrder.find((s) => allowed.includes(s)) || '');
      setStatusDesc('');
      setLocationNote('');
      showToast(`Order ${updated.orderNumber} transitioned to ${updated.currentStatus}!`, 'success');
      loadOrders();
    } catch (err) {
      showToast(err.message, 'error');
    } finally {
      setUpdating(false);
    }
  };

  return (
    <RequireAdminGuard>
      <div className="container section-block">
        <AdminNavBar />

        <div className="page-title-bar">
          <div>
            <h1>Order Lifecycle &amp; Tracking Management</h1>
            <p className="muted">
              Inspect tracking history and execute validated status transitions (<code>PLACED → CONFIRMED → PACKED → SHIPPED → OUT_FOR_DELIVERY → DELIVERED</code>)
            </p>
          </div>
          <div className="header-btn-group">
            <select
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value);
                setPage(0);
              }}
            >
              <option value="">All Statuses</option>
              <option value="PLACED">PLACED</option>
              <option value="CONFIRMED">CONFIRMED</option>
              <option value="PACKED">PACKED</option>
              <option value="SHIPPED">SHIPPED</option>
              <option value="OUT_FOR_DELIVERY">OUT_FOR_DELIVERY</option>
              <option value="DELIVERED">DELIVERED</option>
              <option value="CANCELLED">CANCELLED</option>
            </select>
            <button type="button" className="btn btn-outline btn-sm" onClick={loadOrders}>
              ↻ Refresh
            </button>
          </div>
        </div>

        {loading ? (
          <div className="loading-box">Loading customer orders...</div>
        ) : (
          <div className="card-panel">
            <div className="table-responsive">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Order #</th>
                    <th>Tracking #</th>
                    <th>Customer</th>
                    <th>Date</th>
                    <th>Total</th>
                    <th>Current Status</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {ordersPage.content.map((o) => (
                    <tr key={o.id} className={selectedOrder?.id === o.id ? 'selected-row' : ''}>
                      <td><strong>{o.orderNumber}</strong></td>
                      <td><code>{o.trackingNumber}</code></td>
                      <td>
                        {o.customerName}
                        <div className="muted small">{o.customerEmail}</div>
                      </td>
                      <td>{new Date(o.orderDate).toLocaleDateString()}</td>
                      <td><strong>₹{Number(o.totalAmount).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</strong></td>
                      <td><StatusBadge status={o.currentStatus} /></td>
                      <td>
                        <button
                          type="button"
                          className="btn btn-primary btn-sm"
                          onClick={() => openOrderInspector(o)}
                        >
                          Manage &amp; Track
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {selectedOrder && (
          <div className="inspector-section" style={{ marginTop: '2rem' }}>
            <div className="card-panel">
              <div className="panel-head">
                <h3>
                  🔍 Inspecting Order {selectedOrder.orderNumber} ({selectedOrder.trackingNumber})
                </h3>
                <button
                  type="button"
                  className="btn btn-outline btn-sm"
                  onClick={() => setSelectedOrder(null)}
                >
                  Close Inspector
                </button>
              </div>

              {selectedOrder.allowedNextStatuses && selectedOrder.allowedNextStatuses.length > 0 ? (
                <form onSubmit={handleStatusUpdate} className="status-transition-form">
                  <h4>Transition Order Status (Legal Next States Only)</h4>
                  <div className="form-row-three">
                    <div className="form-group">
                      <label>Next Legal Status *</label>
                      <select
                        required
                        value={nextStatus}
                        onChange={(e) => setNextStatus(e.target.value)}
                      >
                        {Array.from(selectedOrder.allowedNextStatuses).map((st) => (
                          <option key={st} value={st}>{st}</option>
                        ))}
                      </select>
                    </div>
                    <div className="form-group">
                      <label>Facility / Location Note (Optional)</label>
                      <input
                        type="text"
                        placeholder="e.g., Regional Hub Bay 4"
                        value={locationNote}
                        onChange={(e) => setLocationNote(e.target.value)}
                      />
                    </div>
                    <div className="form-group">
                      <label>Custom Tracking Description (Optional)</label>
                      <input
                        type="text"
                        placeholder="Leave blank for standard stage message"
                        value={statusDesc}
                        onChange={(e) => setStatusDesc(e.target.value)}
                      />
                    </div>
                  </div>
                  <button type="submit" className="btn btn-primary" disabled={updating}>
                    {updating ? 'Recording Transition...' : `Update Status to ${nextStatus}`}
                  </button>
                </form>
              ) : (
                <div className="sim-payment-notice" style={{ marginBottom: '1rem' }}>
                  This order is in terminal state <strong>{selectedOrder.currentStatus}</strong> and no further transitions are permitted.
                </div>
              )}

              <TrackingTimeline
                currentStatus={selectedOrder.currentStatus}
                trackingHistory={selectedOrder.trackingHistory}
                estimatedDeliveryDate={selectedOrder.estimatedDeliveryDate}
              />
            </div>
          </div>
        )}
      </div>
    </RequireAdminGuard>
  );
}

