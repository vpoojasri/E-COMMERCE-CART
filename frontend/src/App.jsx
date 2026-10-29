import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AppProvider, useApp } from './context/AppContext';
import { Navbar, Footer } from './components/Layout';
import { HomePage, ProductListPage, ProductDetailPage } from './pages/CatalogPages';
import { LoginPage, RegisterPage } from './pages/AuthPages';
import { CartPage, CheckoutPage, OrderConfirmationPage } from './pages/CheckoutPages';
import {
  TrackOrderPage,
  CustomerDashboardPage,
  OrderHistoryPage,
  OrderTrackingDetailsPage,
} from './pages/CustomerPages';
import {
  AdminDashboardPage,
  AdminProductsPage,
  AdminProductFormPage,
  AdminCategoriesPage,
  AdminOrdersPage,
} from './pages/AdminPages';
import { NotFoundPage } from './pages/NotFoundPage';

function ToastBanner() {
  const { toast } = useApp();
  if (!toast) return null;
  return (
    <div className={`toast-banner toast-${toast.type}`}>
      <span>{toast.message}</span>
    </div>
  );
}

export default function App() {
  return (
    <AppProvider>
      <BrowserRouter>
        <div className="app-shell">
          <Navbar />
          <ToastBanner />
          <main className="main-content">
            <Routes>
              {/* 1. Home page */}
              <Route path="/" element={<HomePage />} />
              {/* 2. Product listing page */}
              <Route path="/products" element={<ProductListPage />} />
              {/* 3. Product details page */}
              <Route path="/products/:id" element={<ProductDetailPage />} />
              {/* 4. Login page */}
              <Route path="/login" element={<LoginPage />} />
              {/* 5. Registration page */}
              <Route path="/register" element={<RegisterPage />} />
              {/* 6. Shopping cart page */}
              <Route path="/cart" element={<CartPage />} />
              {/* 7. Checkout page */}
              <Route path="/checkout" element={<CheckoutPage />} />
              {/* 8. Order confirmation page */}
              <Route path="/order-confirmation/:id" element={<OrderConfirmationPage />} />
              {/* 9. Track order page */}
              <Route path="/track-order" element={<TrackOrderPage />} />
              {/* 10. Customer dashboard */}
              <Route path="/dashboard" element={<CustomerDashboardPage />} />
              {/* 11. Order history page */}
              <Route path="/orders" element={<OrderHistoryPage />} />
              {/* 12. Order tracking details page */}
              <Route path="/orders/:id/tracking" element={<OrderTrackingDetailsPage />} />
              {/* 13. Administrator dashboard */}
              <Route path="/admin" element={<AdminDashboardPage />} />
              {/* 14. Product management page */}
              <Route path="/admin/products" element={<AdminProductsPage />} />
              {/* 15. Add/edit product page with image upload */}
              <Route path="/admin/products/new" element={<AdminProductFormPage />} />
              <Route path="/admin/products/:id/edit" element={<AdminProductFormPage />} />
              {/* 16. Category management page */}
              <Route path="/admin/categories" element={<AdminCategoriesPage />} />
              {/* 17. Order management page */}
              <Route path="/admin/orders" element={<AdminOrdersPage />} />
              {/* 18. Not-found page */}
              <Route path="*" element={<NotFoundPage />} />
            </Routes>
          </main>
          <Footer />
        </div>
      </BrowserRouter>
    </AppProvider>
  );
}

