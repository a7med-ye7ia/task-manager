import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { UserProfileResponse } from '../types';
import {
  getProfileApi,
  loginApi,
  logoutApi,
  getAccessToken,
  setAccessToken,
  getAdminPendingCountApi,
} from '../services/api';

interface AuthContextType {
  user: UserProfileResponse | null;
  isAuthenticated: boolean;
  isManager: boolean;
  isLoading: boolean;
  pendingCount: number;
  login: (email: string, pass: string) => Promise<void>;
  logout: () => Promise<void>;
  refreshProfile: () => Promise<void>;
  refreshPendingCount: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<UserProfileResponse | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [pendingCount, setPendingCount] = useState<number>(0);

  const refreshProfile = useCallback(async () => {
    try {
      const profile = await getProfileApi();
      setUser(profile);
      if (profile.role === 'MANAGER') {
        try {
          const countRes = await getAdminPendingCountApi();
          setPendingCount(countRes.count);
        } catch {
          // ignore pending count failure
        }
      }
    } catch {
      setUser(null);
      setAccessToken(null);
    }
  }, []);

  const refreshPendingCount = useCallback(async () => {
    if (user?.role === 'MANAGER') {
      try {
        const countRes = await getAdminPendingCountApi();
        setPendingCount(countRes.count);
      } catch {
        // ignore
      }
    }
  }, [user]);

  useEffect(() => {
    const initAuth = async () => {
      const token = getAccessToken();
      if (token) {
        await refreshProfile();
      }
      setIsLoading(false);
    };

    initAuth();

    const handleExpired = () => {
      setUser(null);
    };
    window.addEventListener('auth:expired', handleExpired);
    return () => window.removeEventListener('auth:expired', handleExpired);
  }, [refreshProfile]);

  const login = async (email: string, pass: string) => {
    await loginApi(email, pass);
    await refreshProfile();
  };

  const logout = async () => {
    await logoutApi();
    setUser(null);
    setPendingCount(0);
  };

  const isAuthenticated = !!user;
  const isManager = user?.role === 'MANAGER';

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated,
        isManager,
        isLoading,
        pendingCount,
        login,
        logout,
        refreshProfile,
        refreshPendingCount,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
