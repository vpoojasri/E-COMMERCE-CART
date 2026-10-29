import React, { useState, useEffect, useCallback } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { useApp } from '../context/AppContext';
import { api } from '../api';
import { TrackingTimeline, StatusBadge } from '../components/TrackingTimeline';

export function TrackOrderPage() {
  const { user, showToast } = useApp();
  const [searchParams, setSearchParams] = useSearchParams();
  const [trackingNumber, setTrackingNumber] = useState(searchParams.get('tn') || '');
  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const performLookup = useCallback(async (tn) => {
    if (!tn || !tn.trim()) return;
    setLoading(true);
    setError('');
    try {
      const data = await api.trackByTrackingNumber(tn.trim());
      setOrder(data);
    } catch (err) {
      setOrder(null);
      setError(err.message || 'Could not locate order for this tracking number.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const initialTn = searchParams.get('tn');
    if (initialTn && user) {
      setTrackingNumber(initialTn);
      performLookup(initialTn);
    }
  }, [searchParams, user, performLookup]);

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!user) {
      showToast('Please sign in to verify ownership and track your order.', 'info');
      return;
    }
    setSearchParams({ tn: trackingNumber.trim() });
    performLookup(trackingNumber);
  };

  return (
    <div className="container section-block">
      <div className="page-title-bar">
        <div>
          <h1>Automated Order Tracking Lookup</h1>
          <p className="muted">
            Enter your unique tracking number (<code>TRK-...</code>) to inspect real-time status and event logs.
          </p>
        </div>
      </div>

      {!user ? (
        <div className="empty-state">
          <h3>Authentication Required for Privacy &amp; Security</h3>
          <p className="muted">
            To protect customer privacy and shipping details, please sign in to track your orders.
          </p>
          <Link to="/login" className="btn btn-primary">Sign In to Track Order</Link>
        </div>
      ) : (
        <>
          <form onSubmit={handleSubmit} className="track-lookup-bar">
            <input
              type="text"
              placeholder="Enter Tracking Number (e.g., TRK-A1B2C3D4E5)"
              value={trackingNumber}
              onChange={(e) => setTrackingNumber(e.target.value)}
              required
            />
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? 'Searching...' : '🔍 Track Order'}
            </button>
            {order && (
              <button
                type="button"
                className="btn btn-outline"
                onClick={() => performLookup(order.trackingNumber)}
              >
                ↻ Refresh Live Status
              </button>
            )}
          </form>

          {error && <div className="alert alert-error">{error}</div>}

          {order && (
            <div className="tracked-order-result">
              <div className="order-overview-banner">
                <div>
                  <span className="muted small">Order Number</span>
                  <h3>{order.orderNumber}</h3>
                </div>
                <div>
                  <span className="muted small">Tracking Number</span>
                  <h3 className="accent-code">{order.trackingNumber}</h3>
                </div>
                <div>
                  <span className="muted small">Order Total</span>
                  <h3>₹{Number(order.totalAmount).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</h3>
                </div>
                <div>
                  <Link to={`/orders/${order.id}/tracking`} className="btn btn-outline btn-sm">
                    Full Order Details →
                  </Link>
                </div>
              </div>

              <TrackingTimeline
                currentStatus={order.currentStatus}
                trackingHistory={order.trackingHistory}
                estimatedDeliveryDate={order.estimatedDeliveryDate}
              />
            </div>
          )}
        </>
      )}
    </div>
  );
}

export function CustomerDashboardPage() {
  const { user, setUser, notifications, refreshNotifications, showToast } = useApp();
  const [recentOrders, setRecentOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [editingProfile, setEditingProfile] = useState(false);
  const [profileForm, setProfileForm] = useState({
    fullName: '',
    phone: '',
    shippingAddress: '',
  });

  useEffect(() => {
    if (user) {
      setProfileForm({
        fullName: user.fullName || '',
        phone: user.phone || '',
        shippingAddress: user.shippingAddress || '',
      });
      api
        .getMyOrders(0, 5)
        .then((res) => setRecentOrders(res.content || []))
        .catch(() => {})
        .finally(() => setLoading(false));
    }
  }, [user]);

  if (!user) {
    return (
      <div className="container section-block empty-state">
        <h2>Please Sign In to Access Your Customer Dashboard</h2>
        <Link to="/login" className="btn btn-primary">Sign In</Link>
      </div>
    );
  }

  const handleSaveProfile = async (e) => {
    e.preventDefault();
    try {
      const updated = await api.updateProfile(profileForm);
      setUser(updated);
      setEditingProfile(false);
      showToast('Profile and shipping address updated!', 'success');
    } catch (err) {
      showToast(err.message, 'error');
    }
  };

  return (
    <div className="container section-block">
      <div className="page-title-bar">
        <div>
          <h1>Welcome, {user.fullName}</h1>
          <p className="muted">Manage your shipping profile, active deliveries, and order notifications</p>
        </div>
        <Link to="/orders" className="btn btn-primary">View Full Order History</Link>
      </div>

      <div className="dashboard-two-col">
        <div className="card-panel">
          <div className="panel-head">
            <h3>👤 Profile &amp; Default Shipping Details</h3>
            <button
              type="button"
              className="btn btn-outline btn-sm"
              onClick={() => setEditingProfile(!editingProfile)}
            >
              {editingProfile ? 'Cancel' : 'Edit Profile'}
            </button>
          </div>

          {editingProfile ? (
            <form onSubmit={handleSaveProfile} className="form-stack">
              <div className="form-group">
                <label>Full Name</label>
                <input
                  type="text"
                  required
                  value={profileForm.fullName}
                  onChange={(e) => setProfileForm({ ...profileForm, fullName: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label>Phone Number</label>
                <input
                  type="text"
                  value={profileForm.phone}
                  onChange={(e) => setProfileForm({ ...profileForm, phone: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label>Default Shipping Address</label>
                <textarea
                  rows={3}
                  value={profileForm.shippingAddress}
                  onChange={(e) => setProfileForm({ ...profileForm, shippingAddress: e.target.value })}
                />
              </div>
              <button type="submit" className="btn btn-primary">Save Changes</button>
            </form>
          ) : (
            <div className="profile-details-list">
              <div><strong>Full Name:</strong> {user.fullName}</div>
              <div><strong>Email:</strong> {user.email}</div>
              <div><strong>Role:</strong> <span className="role-tag">{user.role}</span></div>
              <div><strong>Phone:</strong> {user.phone || 'Not provided'}</div>
              <div><strong>Shipping Address:</strong> {user.shippingAddress || 'Not provided'}</div>
            </div>
          )}
        </div>

        <div className="card-panel">
          <div className="panel-head">
            <h3>🔔 Recent Order Status Notifications</h3>
            {notifications.some((n) => !n.readStatus) && (
              <button
                type="button"
                className="btn btn-outline btn-sm"
                onClick={async () => {
                  await api.markNotificationsRead();
                  await refreshNotifications();
                }}
              >
                Mark All Read
              </button>
            )}
          </div>
          {notifications.length === 0 ? (
            <p className="muted">No notifications yet. Status updates will appear here automatically.</p>
          ) : (
            <div className="dash-notif-list">
              {notifications.slice(0, 6).map((n) => (
                <div key={n.id} className={`dash-notif-item ${n.readStatus ? '' : 'unread'}`}>
                  <p>{n.message}</p>
                  <div className="dash-notif-meta">
                    <small>{new Date(n.createdAt).toLocaleString()}</small>
                    {n.orderId && (
                      <Link to={`/orders/${n.orderId}/tracking`} className="link-accent small">
                        Inspect Timeline →
                      </Link>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      <div className="card-panel" style={{ marginTop: '2rem' }}>
        <div className="panel-head">
          <h3>📦 Recent Orders &amp; Live Tracking</h3>
          <Link to="/orders" className="link-accent">See All Orders →</Link>
        </div>

        {loading ? (
          <div className="loading-box">Loading recent orders...</div>
        ) : recentOrders.length === 0 ? (
          <div className="empty-state">
            <h4>You haven&apos;t placed any orders yet</h4>
            <p className="muted">Place an order from our catalog to experience automated delivery tracking.</p>
            <Link to="/products" className="btn btn-primary">Start Shopping</Link>
          </div>
        ) : (
          <div className="table-responsive">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Order #</th>
                  <th>Tracking #</th>
                  <th>Placed Date</th>
                  <th>Est. Delivery</th>
                  <th>Total</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {recentOrders.map((o) => (
                  <tr key={o.id}>
                    <td><strong>{o.orderNumber}</strong></td>
                    <td><code>{o.trackingNumber}</code></td>
                    <td>{new Date(o.orderDate).toLocaleDateString()}</td>
                    <td>{o.estimatedDeliveryDate}</td>
                    <td><strong>₹{Number(o.totalAmount).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</strong></td>
                    <td><StatusBadge status={o.currentStatus} /></td>
                    <td>
                      <Link to={`/orders/${o.id}/tracking`} className="btn btn-primary btn-sm">
                        Track &amp; Details
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}

export function OrderHistoryPage() {
  const { user, showToast } = useApp();
  const [ordersPage, setOrdersPage] = useState({ content: [], page: 0, totalPages: 1, totalElements: 0 });
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);

  const loadOrders = useCallback(() => {
    if (!user) return;
    setLoading(true);
    api
      .getMyOrders(page, 8)
      .then(setOrdersPage)
      .catch((err) => showToast(err.message, 'error'))
      .finally(() => setLoading(false));
  }, [user, page, showToast]);

  useEffect(() => {
    loadOrders();
  }, [loadOrders]);

  if (!user) {
    return (
      <div className="container section-block empty-state">
        <h2>Sign In to View Your Order History</h2>
        <Link to="/login" className="btn btn-primary">Sign In</Link>
      </div>
    );
  }

  return (
    <div className="container section-block">
      <div className="page-title-bar">
        <div>
          <h1>My Order History</h1>
          <p className="muted">View all past and active orders, tracking numbers, and delivery timelines</p>
        </div>
        <button type="button" className="btn btn-outline btn-sm" onClick={loadOrders}>
          ↻ Refresh
        </button>
      </div>

      {loading ? (
        <div className="loading-box">Loading your orders...</div>
      ) : ordersPage.content.length === 0 ? (
        <div className="empty-state">
          <h3>No orders found</h3>
          <p className="muted">Your order history is empty. Browse products to place your first order!</p>
          <Link to="/products" className="btn btn-primary">Browse Catalog</Link>
        </div>
      ) : (
        <>
          <div className="orders-stack">
            {ordersPage.content.map((order) => (
              <div key={order.id} className="order-history-card">
                <div className="order-history-top">
                  <div>
                    <span className="muted small">Order Number</span>
                    <h4>{order.orderNumber}</h4>
                  </div>
                  <div>
                    <span className="muted small">Tracking Number</span>
                    <h4><code className="accent-code">{order.trackingNumber}</code></h4>
                  </div>
                  <div>
                    <span className="muted small">Order Date</span>
                    <h4>{new Date(order.orderDate).toLocaleString()}</h4>
                  </div>
                  <div>
                    <span className="muted small">Estimated Delivery</span>
                    <h4>{order.estimatedDeliveryDate}</h4>
                  </div>
                  <div>
                    <span className="muted small">Total</span>
                    <h4>₹{Number(order.totalAmount).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</h4>
                  </div>
                  <div>
                    <StatusBadge status={order.currentStatus} />
                  </div>
                </div>

                <div className="order-history-items">
                  {order.items.map((item) => (
                    <span key={item.id} className="item-chip">
                      {item.quantity} × {item.productNameSnapshot} (₹{Number(item.unitPrice).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })})
                    </span>
                  ))}
                </div>

                <div className="order-history-footer">
                  <span className="muted small">Shipping to: {order.shippingAddress}</span>
                  <Link to={`/orders/${order.id}/tracking`} className="btn btn-primary btn-sm">
                    🚚 View Live Tracking Timeline →
                  </Link>
                </div>
              </div>
            ))}
          </div>

          {ordersPage.totalPages > 1 && (
            <div className="pagination-bar">
              <button
                type="button"
                className="btn btn-outline btn-sm"
                disabled={page <= 0}
                onClick={() => setPage((p) => p - 1)}
              >
                ← Previous
              </button>
              <span>Page {page + 1} of {ordersPage.totalPages}</span>
              <button
                type="button"
                className="btn btn-outline btn-sm"
                disabled={page + 1 >= ordersPage.totalPages}
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

export function OrderTrackingDetailsPage() {
  const { id } = useParams();
  const { user, refreshNotifications, showToast } = useApp();
  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [cancelling, setCancelling] = useState(false);
  const [showCancelModal, setShowCancelModal] = useState(false);
  const [cancelReason, setCancelReason] = useState('');

  const fetchOrder = useCallback(() => {
    setLoading(true);
    api
      .getOrderTracking(id)
      .then(setOrder)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [id]);

  useEffect(() => {
    if (user) {
      fetchOrder();
    }
  }, [user, fetchOrder]);

  const handleConfirmCancel = async (e) => {
    e.preventDefault();
    setCancelling(true);
    try {
      const updated = await api.cancelOrder(order.id, cancelReason || 'Cancelled by customer');
      setOrder(updated);
      setShowCancelModal(false);
      await refreshNotifications();
      showToast('Order cancelled and reserved stock restored.', 'info');
    } catch (err) {
      showToast(err.message, 'error');
    } finally {
      setCancelling(false);
    }
  };

  if (!user) {
    return (
      <div className="container section-block empty-state">
        <h2>Authentication Required</h2>
        <Link to="/login" className="btn btn-primary">Sign In</Link>
      </div>
    );
  }

  if (loading) return <div className="container section-block loading-box">Loading order &amp; tracking history...</div>;
  if (error || !order) {
    return (
      <div className="container section-block empty-state">
        <h2>Unable to Load Order</h2>
        <p className="muted">{error || 'Order not found.'}</p>
        <Link to="/orders" className="btn btn-primary">Back to My Orders</Link>
      </div>
    );
  }

  return (
    <div className="container section-block">
      <div className="page-title-bar">
        <div>
          <Link to="/orders" className="link-accent small">← Back to My Orders</Link>
          <h1>Order {order.orderNumber} — Tracking &amp; Details</h1>
          <p className="muted">
            Tracking Number: <code className="accent-code">{order.trackingNumber}</code> | Placed on {new Date(order.orderDate).toLocaleString()}
          </p>
        </div>

        <div className="header-btn-group">
          <button type="button" className="btn btn-outline btn-sm" onClick={fetchOrder}>
            ↻ Refresh Timeline
          </button>
          {order.cancellableByCustomer && (
            <button
              type="button"
              className="btn btn-danger-outline btn-sm"
              onClick={() => setShowCancelModal(true)}
            >
              ✕ Cancel Order
            </button>
          )}
        </div>
      </div>

      {showCancelModal && (
        <div className="modal-backdrop">
          <div className="modal-card">
            <h3>Confirm Order Cancellation</h3>
            <p className="muted">
              Are you sure you want to cancel order <strong>{order.orderNumber}</strong>? Reserved inventory will be returned to stock immediately.
            </p>
            <form onSubmit={handleConfirmCancel} className="form-stack">
              <div className="form-group">
                <label>Reason for Cancellation (Optional)</label>
                <input
                  type="text"
                  placeholder="e.g., Ordered by mistake, changing items..."
                  value={cancelReason}
                  onChange={(e) => setCancelReason(e.target.value)}
                />
              </div>
              <div className="modal-actions">
                <button type="button" className="btn btn-outline" onClick={() => setShowCancelModal(false)}>
                  Keep Order
                </button>
                <button type="submit" className="btn btn-danger" disabled={cancelling}>
                  {cancelling ? 'Cancelling...' : 'Yes, Cancel Order'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      <TrackingTimeline
        currentStatus={order.currentStatus}
        trackingHistory={order.trackingHistory}
        estimatedDeliveryDate={order.estimatedDeliveryDate}
      />

      <div className="dashboard-two-col" style={{ marginTop: '2rem' }}>
        <div className="card-panel">
          <h3>🛍️ Purchased Items (Unit Prices Snapshotted)</h3>
          <div className="order-items-table">
            {order.items.map((item) => (
              <div key={item.id} className="order-detail-item-row">
                <div>
                  <strong>{item.productNameSnapshot}</strong>
                  <div className="muted small">
                    {item.quantity} × ₹{Number(item.unitPrice).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} (Price at purchase)
                  </div>
                </div>
                <strong>₹{Number(item.lineTotal).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</strong>
              </div>
            ))}
            <hr />
            <div className="summary-row total">
              <span>Order Total</span>
              <span>₹{Number(order.totalAmount).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</span>
            </div>
          </div>
        </div>

        <div className="card-panel">
          <h3>📍 Delivery &amp; Payment Summary</h3>
          <div className="profile-details-list">
            <div><strong>Customer:</strong> {order.customerName} ({order.customerEmail})</div>
            <div><strong>Contact Phone:</strong> {order.contactPhone}</div>
            <div><strong>Shipping Address:</strong> {order.shippingAddress}</div>
            <div><strong>Payment Method:</strong> {order.paymentMethod}</div>
            <div><strong>Payment Status:</strong> <span className="role-tag">{order.paymentStatus}</span></div>
            <div><strong>Estimated Delivery Date:</strong> {order.estimatedDeliveryDate}</div>
          </div>
        </div>
      </div>
    </div>
  );
}

