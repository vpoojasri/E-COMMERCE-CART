import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useApp } from '../context/AppContext';
import { api } from '../api';
import { StatusBadge } from '../components/TrackingTimeline';

export function CartPage() {
  const { user, cart, refreshCart, showToast } = useApp();
  const navigate = useNavigate();
  const [updatingId, setUpdatingId] = useState(null);

  useEffect(() => {
    if (user) {
      refreshCart();
    }
  }, [user, refreshCart]);

  if (!user) {
    return (
      <div className="container section-block empty-state">
        <h2>Sign In to View Your Shopping Cart</h2>
        <p className="muted">Your cart is persisted securely in the database for your account.</p>
        <Link to="/login" className="btn btn-primary">Go to Login</Link>
      </div>
    );
  }

  const handleQtyChange = async (item, delta) => {
    const nextQty = item.quantity + delta;
    setUpdatingId(item.id);
    try {
      if (nextQty <= 0) {
        await api.removeCartItem(item.id);
        showToast(`Removed "${item.productName}" from cart.`, 'info');
      } else {
        await api.updateCartItem(item.id, nextQty);
      }
      await refreshCart();
    } catch (err) {
      showToast(err.message, 'error');
    } finally {
      setUpdatingId(null);
    }
  };

  const handleRemove = async (item) => {
    setUpdatingId(item.id);
    try {
      await api.removeCartItem(item.id);
      await refreshCart();
      showToast(`Removed "${item.productName}" from cart.`, 'info');
    } catch (err) {
      showToast(err.message, 'error');
    } finally {
      setUpdatingId(null);
    }
  };

  const items = cart?.items || [];

  return (
    <div className="container section-block">
      <h1>Shopping Cart</h1>
      <p className="muted">All item prices and totals are recalculated and verified on the server.</p>

      {items.length === 0 ? (
        <div className="empty-state">
          <h3>Your cart is currently empty</h3>
          <p className="muted">Explore our product catalog to add electronics, smart home devices, and lab gear.</p>
          <Link to="/products" className="btn btn-primary">Browse Catalog</Link>
        </div>
      ) : (
        <div className="cart-layout">
          <div className="cart-items-col">
            {items.map((item) => (
              <div key={item.id} className="cart-item-card">
                <img
                  src={item.imagePath || '/api/uploads/fallback-product.svg'}
                  alt={item.productName}
                  className="cart-item-img"
                />
                <div className="cart-item-info">
                  <Link to={`/products/${item.productId}`} className="cart-item-title">
                    {item.productName}
                  </Link>
                  <div className="muted small">
                    Unit Price: <strong>₹{Number(item.unitPrice).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</strong> | Available Stock: {item.availableStock}
                  </div>
                  {!item.stockValid && (
                    <div className="stock-warning">
                      ⚠️ Requested quantity exceeds current available stock ({item.availableStock}).
                    </div>
                  )}
                </div>

                <div className="cart-qty-controls">
                  <button
                    type="button"
                    className="qty-btn"
                    disabled={updatingId === item.id}
                    onClick={() => handleQtyChange(item, -1)}
                  >
                    −
                  </button>
                  <span className="qty-val">{item.quantity}</span>
                  <button
                    type="button"
                    className="qty-btn"
                    disabled={updatingId === item.id || item.quantity >= item.availableStock}
                    onClick={() => handleQtyChange(item, 1)}
                  >
                    +
                  </button>
                </div>

                <div className="cart-item-subtotal">
                  ₹{Number(item.subtotal).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                </div>

                <button
                  type="button"
                  className="btn btn-danger-outline btn-sm"
                  disabled={updatingId === item.id}
                  onClick={() => handleRemove(item)}
                >
                  Remove
                </button>
              </div>
            ))}
          </div>

          <div className="cart-summary-card">
            <h3>Order Summary</h3>
            <div className="summary-row">
              <span>Items ({cart.totalItems})</span>
              <span>₹{Number(cart.subtotal).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</span>
            </div>
            <div className="summary-row">
              <span>Standard Shipping</span>
              <span>FREE</span>
            </div>
            <hr />
            <div className="summary-row total">
              <span>Server-Verified Total</span>
              <span>₹{Number(cart.totalAmount).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</span>
            </div>

            {!cart.allItemsInStock && (
              <div className="alert alert-error small">
                Please adjust quantities for out-of-stock items before proceeding to checkout.
              </div>
            )}

            <button
              type="button"
              className="btn btn-primary btn-block btn-lg"
              disabled={!cart.allItemsInStock}
              onClick={() => navigate('/checkout')}
            >
              Proceed to Checkout →
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

export function CheckoutPage() {
  const { user, cart, refreshCart, refreshNotifications, showToast } = useApp();
  const navigate = useNavigate();

  const [shippingAddress, setShippingAddress] = useState(user?.shippingAddress || '');
  const [contactPhone, setContactPhone] = useState(user?.phone || '');
  const [paymentMethod, setPaymentMethod] = useState('SIMULATED_CARD_DEMO');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (user) {
      setShippingAddress((prev) => prev || user.shippingAddress || '');
      setContactPhone((prev) => prev || user.phone || '');
    }
  }, [user]);

  if (!user) {
    return (
      <div className="container section-block empty-state">
        <h2>Please Log In to Checkout</h2>
        <Link to="/login" className="btn btn-primary">Sign In</Link>
      </div>
    );
  }

  if (!cart?.items || cart.items.length === 0) {
    return (
      <div className="container section-block empty-state">
        <h2>Your Cart is Empty</h2>
        <p className="muted">Add items to your cart before placing an order.</p>
        <Link to="/products" className="btn btn-primary">Browse Products</Link>
      </div>
    );
  }

  const handlePlaceOrder = async (e) => {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      const createdOrder = await api.placeOrder({
        shippingAddress,
        contactPhone,
        paymentMethod,
      });
      await Promise.all([refreshCart(), refreshNotifications()]);
      showToast(`Order ${createdOrder.orderNumber} placed successfully!`, 'success');
      navigate(`/order-confirmation/${createdOrder.id}`);
    } catch (err) {
      setError(err.message || 'Checkout failed.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="container section-block">
      <h1>Checkout &amp; Order Placement</h1>
      <p className="muted">
        Inventory is validated and locked transactionally upon confirmation.
      </p>

      {error && <div className="alert alert-error">{error}</div>}

      <div className="checkout-grid">
        <form onSubmit={handlePlaceOrder} className="checkout-form-card">
          <h3>1. Shipping &amp; Contact Information</h3>
          <div className="form-group">
            <label>Recipient Full Name</label>
            <input type="text" value={user.fullName} disabled />
          </div>

          <div className="form-group">
            <label>Contact Phone Number *</label>
            <input
              type="text"
              required
              minLength={6}
              placeholder="+1-555-234-5678"
              value={contactPhone}
              onChange={(e) => setContactPhone(e.target.value)}
            />
          </div>

          <div className="form-group">
            <label>Full Delivery Address *</label>
            <textarea
              rows={3}
              required
              minLength={5}
              placeholder="Street address, apartment/lab room, city, state, postal code"
              value={shippingAddress}
              onChange={(e) => setShippingAddress(e.target.value)}
            />
          </div>

          <h3>2. Simulated Payment Method (College Lab Demonstration)</h3>
          <div className="sim-payment-notice">
            <strong>ℹ️ Simulation Notice:</strong> This payment step is a clearly labeled <em>College Laboratory Simulation</em> and does not connect to a live financial gateway or charge real cards.
          </div>

          <div className="payment-options">
            <label className="radio-card">
              <input
                type="radio"
                name="paymentMethod"
                value="SIMULATED_CARD_DEMO"
                checked={paymentMethod === 'SIMULATED_CARD_DEMO'}
                onChange={(e) => setPaymentMethod(e.target.value)}
              />
              <div>
                <strong>💳 Simulated Credit / Debit Card (Instant Lab Approval)</strong>
                <p className="muted small">Simulates an authorized payment token for order lifecycle testing.</p>
              </div>
            </label>

            <label className="radio-card">
              <input
                type="radio"
                name="paymentMethod"
                value="SIMULATED_CAMPUS_WALLET"
                checked={paymentMethod === 'SIMULATED_CAMPUS_WALLET'}
                onChange={(e) => setPaymentMethod(e.target.value)}
              />
              <div>
                <strong>🎓 Simulated University Lab Wallet</strong>
                <p className="muted small">Demonstrates immediate order placement and tracking number generation.</p>
              </div>
            </label>
          </div>

          <button type="submit" className="btn btn-primary btn-lg btn-block" disabled={submitting}>
            {submitting ? 'Processing Transaction...' : `Confirm & Place Order (₹${Number(cart.totalAmount).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })})`}
          </button>
        </form>

        <div className="cart-summary-card">
          <h3>Order Items ({cart.totalItems})</h3>
          <div className="checkout-item-list">
            {cart.items.map((i) => (
              <div key={i.id} className="checkout-mini-item">
                <span>{i.quantity} × {i.productName}</span>
                <strong>₹{Number(i.subtotal).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</strong>
              </div>
            ))}
          </div>
          <hr />
          <div className="summary-row total">
            <span>Total Payable</span>
            <span>₹{Number(cart.totalAmount).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</span>
          </div>
        </div>
      </div>
    </div>
  );
}

export function OrderConfirmationPage() {
  const { id } = useParams();
  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api
      .getOrderById(id)
      .then(setOrder)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) return <div className="container section-block loading-box">Loading order confirmation...</div>;
  if (!order) {
    return (
      <div className="container section-block empty-state">
        <h2>Order Not Found</h2>
        <Link to="/orders" className="btn btn-primary">View My Orders</Link>
      </div>
    );
  }

  return (
    <div className="container section-block">
      <div className="confirmation-box">
        <div className="confirm-icon">🎉</div>
        <h1>Thank You! Your Order Has Been Placed</h1>
        <p className="muted">
          Your inventory has been reserved and your automated order tracking timeline has been initialized.
        </p>

        <div className="confirm-meta-grid">
          <div className="meta-box">
            <span className="muted small">Order Number</span>
            <strong>{order.orderNumber}</strong>
          </div>
          <div className="meta-box">
            <span className="muted small">Automated Tracking Number</span>
            <strong className="accent-code">{order.trackingNumber}</strong>
          </div>
          <div className="meta-box">
            <span className="muted small">Current Status</span>
            <StatusBadge status={order.currentStatus} />
          </div>
          <div className="meta-box">
            <span className="muted small">Estimated Delivery</span>
            <strong>{order.estimatedDeliveryDate}</strong>
          </div>
        </div>

        <div className="confirm-actions">
          <Link to={`/orders/${order.id}/tracking`} className="btn btn-primary btn-lg">
            🚚 Open Live Tracking Timeline
          </Link>
          <Link to="/orders" className="btn btn-outline btn-lg">
            View Order History
          </Link>
        </div>
      </div>
    </div>
  );
}

