import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { api, setToken } from "./api";
import type { UserProfile } from "./types";

interface AuthState {
  user: UserProfile | null;
  loading: boolean;
  googleEnabled: boolean;
  logout: () => void;
  reload: () => Promise<void>;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [googleEnabled, setGoogleEnabled] = useState(false);

  const reload = async () => {
    try {
      const config = await api.authConfig();
      setGoogleEnabled(config.googleEnabled);
      if (localStorage.getItem("ibt_token")) {
        setUser(await api.me());
      } else {
        setUser(null);
      }
    } catch {
      setUser(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void reload();
  }, []);

  const value = useMemo<AuthState>(
    () => ({
      user,
      loading,
      googleEnabled,
      logout: () => {
        setToken(null);
        setUser(null);
      },
      reload,
    }),
    [user, loading, googleEnabled]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth outside provider");
  }
  return ctx;
}
