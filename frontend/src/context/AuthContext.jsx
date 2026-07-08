import { createContext, useContext, useEffect, useMemo, useState, useCallback } from 'react';
import { AuthAPI } from '../api/endpoints';
import { tokenStore } from '../api/client';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const stored = localStorage.getItem('currentUser');
    return stored ? JSON.parse(stored) : null;
  });
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (user) {
      localStorage.setItem('currentUser', JSON.stringify(user));
    }
  }, [user]);

  const login = useCallback(async (username, password) => {
    setLoading(true);
    try {
      const res = await AuthAPI.login({ username, password });
      const data = res.data.data;
      tokenStore.setTokens({ accessToken: data.accessToken, refreshToken: data.refreshToken });
      setUser(data.user);
      return data.user;
    } finally {
      setLoading(false);
    }
  }, []);

  const logout = useCallback(async () => {
    const { refreshToken } = tokenStore.getTokens();
    try {
      await AuthAPI.logout({ refreshToken });
    } catch {
      // best-effort; clear local state regardless
    }
    tokenStore.clearTokens();
    setUser(null);
  }, []);

  const hasRole = useCallback((...roles) => {
    if (!user?.roles) return false;
    return roles.some((r) => user.roles.includes(r));
  }, [user]);

  const value = useMemo(() => ({
    user, loading, login, logout, hasRole, isAuthenticated: !!user
  }), [user, loading, login, logout, hasRole]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
