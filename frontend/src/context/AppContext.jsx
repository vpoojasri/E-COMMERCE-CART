import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { api } from '../api';

const AppContext = createContext(null);

export function AppProvider({ children }) {
  const [user, setUser] = useState(null);
  const [authLoading, setAuthLoading] = useState(true);
  const [cart, setCart] = useState({ items: [], totalItems: 0, subtotal: 0, totalAmount: 0, allItemsInStock: false });
  const [notifications, setNotifications] = useState([]);
  const [toast, setToast] = useState(null);

  const showToast = useCallback((message, type = 'success') => {
    setToast({ message, type, id: Date.now() });
    setTimeout(() => {
      setToast((prev) => (prev && Date.now() - prev.id >= 3800 ? null : prev));
    }, 4000);
  }, []);

  const refreshCart = useCallback(async () => {
    try {
      const data = await api.getCart();
      setCart(data);
    } catch {
      setCart({ items: [], totalItems: 0, subtotal: 0, totalAmount: 0, allItemsInStock: false });
    }
  }, []);

  const refreshNotifications = useCallback(async () => {
    try {
      const list = await api.getNotifications();
      setNotifications(list || []);
    } catch {
      setNotifications([]);
    }
  }, []);

  const checkSession = useCallback(async () => {
    setAuthLoading(true);
    try {
      const me = await api.getMe();
      setUser(me);
      await Promise.all([refreshCart(), refreshNotifications()]);
    } catch {
      setUser(null);
      setCart({ items: [], totalItems: 0, subtotal: 0, totalAmount: 0, allItemsInStock: false });
      setNotifications([]);
    } finally {
      setAuthLoading(false);
    }
  }, [refreshCart, refreshNotifications]);

  useEffect(() => {
    checkSession();
  }, [checkSession]);

  const handleLogin = async (credentials) => {
    const loggedInUser = await api.login(credentials);
    setUser(loggedInUser);
    await Promise.all([refreshCart(), refreshNotifications()]);
    showToast(`Welcome back, ${loggedInUser.fullName}!`, 'success');
    return loggedInUser;
  };

  const handleRegister = async (payload) => {
    const newUser = await api.register(payload);
    setUser(newUser);
    await Promise.all([refreshCart(), refreshNotifications()]);
    showToast(`Account created! Welcome, ${newUser.fullName}.`, 'success');
    return newUser;
  };

  const handleLogout = async () => {
    try {
      await api.logout();
    } catch {
      // ignore
    }
    setUser(null);
    setCart({ items: [], totalItems: 0, subtotal: 0, totalAmount: 0, allItemsInStock: false });
    setNotifications([]);
    showToast('You have been logged out safely.', 'info');
  };

  return (
    <AppContext.Provider
      value={{
        user,
        setUser,
        authLoading,
        cart,
        refreshCart,
        notifications,
        refreshNotifications,
        handleLogin,
        handleRegister,
        handleLogout,
        toast,
        showToast,
      }}
    >
      {children}
    </AppContext.Provider>
  );
}

export function useApp() {
  return useContext(AppContext);
}

