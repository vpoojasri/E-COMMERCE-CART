import React, { useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useApp } from '../context/AppContext';
import { api } from '../api';

function BrandLogo({ size = 38 }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 44 44"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className="brand-logo-svg"
      aria-hidden="true"
    >
      {/* Rounded Cream badge background */}
      <rect width="44" height="44" rx="11" fill="#F5DABF" />
      {/* Isometric 3D Parcel Box in Deep Green & Bordo */}
      <path
        d="M22 8L35 15.2V28.8L22 36L9 28.8V15.2L22 8Z"
        fill="#0F3D3A"
        stroke="#0F3D3A"
        strokeWidth="1.5"
        strokeLinejoin="round"
      />
      {/* Top right parcel flap in Bordo */}
      <path
        d="M22 8L35 15.2L22 22.4L9 15.2L22 8Z"
        fill="#6C151E"
      />
      {/* Parcel center tape & edges in Cream */}
      <path
        d="M22 22.4V36M9 15.2L22 22.4L35 15.2M15.5 11.6L28.5 18.8"
        stroke="#F5DABF"
        strokeWidth="2.2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      {/* Tracking GPS Pulse Dot */}
      <circle cx="33" cy="31" r="6" fill="#6C151E" stroke="#F5DABF" strokeWidth="2" />
      <path
        d="M30.8 31L32.3 32.5L35.4 29.4"
        stroke="#F5DABF"
        strokeWidth="1.8"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

export function Navbar() {
  const { user, cart, notifications, refreshNotifications, handleLogout } = useApp();
  const [showNotif, setShowNotif] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const navigate = useNavigate();

  const unreadCount = notifications.filter((n) => !n.readStatus).length;

  const onLogoutClick = async () => {
    await handleLogout();
    navigate('/login');
  };

  const handleMarkAllRead = async () => {
    await api.markNotificationsRead();
    await refreshNotifications();
  };

  const isAdmin = user?.role === 'ADMIN';

  return (
    <header className="navbar">
      <div className="container navbar-inner">
        <Link to={isAdmin ? '/admin' : '/'} className="brand">
          <BrandLogo size={40} />
          <div>
            <span className="brand-title">PulseTrack</span>
            <span className="brand-sub">{isAdmin ? 'Admin Console' : 'Order Tracking Engine'}</span>
          </div>
        </Link>

        <button
          className="mobile-menu-btn"
          onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
          aria-label="Toggle navigation menu"
        >
          ☰
        </button>

        <nav className={`nav-links ${mobileMenuOpen ? 'open' : ''}`}>
          {isAdmin ? (
            <>
              <NavLink to="/admin" end onClick={() => setMobileMenuOpen(false)}>📊 Dashboard</NavLink>
              <NavLink to="/admin/orders" onClick={() => setMobileMenuOpen(false)}>🚚 Order &amp; Tracking Control</NavLink>
              <NavLink to="/admin/products" end onClick={() => setMobileMenuOpen(false)}>📦 Products &amp; Inventory</NavLink>
              <NavLink to="/admin/products/new" onClick={() => setMobileMenuOpen(false)}>➕ Add Product</NavLink>
              <NavLink to="/admin/categories" onClick={() => setMobileMenuOpen(false)}>🗂️ Categories</NavLink>
            </>
          ) : (
            <>
              <NavLink to="/" end onClick={() => setMobileMenuOpen(false)}>Home</NavLink>
              <NavLink to="/products" onClick={() => setMobileMenuOpen(false)}>Catalog</NavLink>
              <NavLink to="/track-order" onClick={() => setMobileMenuOpen(false)}>Track Order</NavLink>
              {user && (
                <>
                  <NavLink to="/orders" onClick={() => setMobileMenuOpen(false)}>My Orders</NavLink>
                  <NavLink to="/dashboard" onClick={() => setMobileMenuOpen(false)}>Account</NavLink>
                </>
              )}
            </>
          )}
        </nav>

        <div className="nav-actions">
          {user && !isAdmin && (
            <div className="notif-wrapper">
              <button
                type="button"
                className="icon-btn"
                onClick={() => setShowNotif(!showNotif)}
                title="Order Notifications"
              >
                🔔
                {unreadCount > 0 && <span className="badge-count">{unreadCount}</span>}
              </button>
              {showNotif && (
                <div className="notif-dropdown">
                  <div className="notif-header">
                    <strong>Notifications</strong>
                    {unreadCount > 0 && (
                      <button type="button" className="link-btn" onClick={handleMarkAllRead}>
                        Mark all read
                      </button>
                    )}
                  </div>
                  <div className="notif-list">
                    {notifications.length === 0 ? (
                      <p className="notif-empty">No notifications yet.</p>
                    ) : (
                      notifications.slice(0, 8).map((n) => (
                        <div
                          key={n.id}
                          className={`notif-item ${n.readStatus ? '' : 'unread'}`}
                          onClick={() => {
                            setShowNotif(false);
                            if (n.orderId) navigate(`/orders/${n.orderId}/tracking`);
                          }}
                        >
                          <p>{n.message}</p>
                          <small>{new Date(n.createdAt).toLocaleString()}</small>
                        </div>
                      ))
                    )}
                  </div>
                </div>
              )}
            </div>
          )}

          {!isAdmin && (
            <Link to="/cart" className="cart-pill" title="Shopping Cart">
              🛒 <span>Cart</span>
              <span className="cart-badge">{cart?.totalItems || 0}</span>
            </Link>
          )}

          {user ? (
            <div className="user-menu">
              <span className="user-chip" title={user.email}>
                {isAdmin ? '🛡️ ' : '👤 '}
                {user.fullName.split(' ')[0]}
              </span>
              <button type="button" className="btn btn-outline btn-sm" onClick={onLogoutClick}>
                Logout
              </button>
            </div>
          ) : (
            <div className="auth-btns">
              <Link to="/login" className="btn btn-outline btn-sm">Login</Link>
              <Link to="/register" className="btn btn-primary btn-sm">Register</Link>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}

export function Footer() {
  return (
    <footer className="footer">
      <div className="container footer-grid">
        <div className="footer-brand-col">
          <div className="brand">
            <BrandLogo size={34} />
            <h4>PulseTrack E-Commerce Engine</h4>
          </div>
        </div>
        <div>
          <h5>Quick Navigation</h5>
          <ul className="footer-links">
            <li><Link to="/products">Product Catalog</Link></li>
            <li><Link to="/track-order">Automated Order Tracker</Link></li>
            <li><Link to="/cart">Shopping Cart</Link></li>
            <li><Link to="/dashboard">Customer Portal</Link></li>
          </ul>
        </div>
        <div>
          <h5>Order Lifecycle</h5>
          <p className="muted small">
            PLACED → CONFIRMED → PACKED → SHIPPED → OUT_FOR_DELIVERY → DELIVERED
          </p>
          <span className="sim-badge">College Laboratory Edition</span>
        </div>
      </div>
    </footer>
  );
}

