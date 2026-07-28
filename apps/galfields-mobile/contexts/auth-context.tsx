import React, { createContext, useContext, useEffect, useState } from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { router } from 'expo-router';
import { User } from '@/types/user';
import { login as loginRequest } from '@/services/auth-api';
import { onUnauthorized, setAuthToken } from '@/services/auth-token';

const JWT_KEY = 'auth.jwt';
const EMPLOYEE_KEY = 'auth.employee';

interface AuthContextValue {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  login: (username: string, password: string) => Promise<boolean>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export interface AuthSession {
  token: string;
  user: User;
}

/** Reads whatever `login()` below persisted on a previous run - call once
 * at app boot (see `app/_layout.tsx`, same `initApiBaseUrl()`-style pattern)
 * and pass the result into `AuthProvider`'s `initialSession`, so a valid
 * session survives a reload without ever flashing the login screen. Returns
 * null if nothing was ever persisted, or the stored employee JSON is
 * corrupt (e.g. an interrupted write). */
export async function restoreAuthSession(): Promise<AuthSession | null> {
  const [token, employeeJson] = await Promise.all([
    AsyncStorage.getItem(JWT_KEY),
    AsyncStorage.getItem(EMPLOYEE_KEY),
  ]);
  if (!token || !employeeJson) return null;
  try {
    return { token, user: JSON.parse(employeeJson) as User };
  } catch {
    return null;
  }
}

export function AuthProvider({
  children,
  initialSession = null,
}: {
  children: React.ReactNode;
  initialSession?: AuthSession | null;
}) {
  const [user, setUser] = useState<User | null>(initialSession?.user ?? null);
  const [token, setToken] = useState<string | null>(initialSession?.token ?? null);

  // Keeps services/auth-token.ts's synchronous module cache in sync with
  // this state - every services/*-api.ts call (via authenticatedFetch)
  // reads the token from there, since a plain module can't call useAuth().
  // Runs on mount too, so the restored session (initialSession) is in the
  // cache before any child component's effect could fire a request.
  useEffect(() => {
    setAuthToken(token);
  }, [token]);

  // Global 401 handler (services/authenticated-fetch.ts) - any authenticated
  // call that comes back 401 (expired/invalidated JWT) forces a clean logout
  // instead of surfacing a raw error somewhere deep in a screen.
  useEffect(() => {
    onUnauthorized(() => {
      logout();
      router.replace('/login');
    });
    return () => onUnauthorized(null);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const login = async (username: string, password: string): Promise<boolean> => {
    let response;
    try {
      response = await loginRequest(username, password);
    } catch {
      return false;
    }

    const nextUser: User = { id: String(response.employeeId), username: response.username, role: response.roleName };
    await Promise.all([
      AsyncStorage.setItem(JWT_KEY, response.token),
      AsyncStorage.setItem(EMPLOYEE_KEY, JSON.stringify(nextUser)),
    ]);
    setUser(nextUser);
    setToken(response.token);
    return true;
  };

  const logout = async () => {
    await Promise.all([AsyncStorage.removeItem(JWT_KEY), AsyncStorage.removeItem(EMPLOYEE_KEY)]);
    setUser(null);
    setToken(null);
  };

  return (
    <AuthContext.Provider value={{ user, token, isAuthenticated: !!user, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
