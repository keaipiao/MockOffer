"use client";

import { createContext, useContext, useState, type ReactNode } from "react";
import { api } from "@/lib/api";
import type { AuthUser } from "@/lib/auth-server";

export type { AuthUser };

type AuthContextValue = {
  user: AuthUser;
  setUser: (u: AuthUser) => void;
  /** 主动重新拉取并刷新缓存（如绑定/解绑后）。 */
  refresh: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

/**
 * 全局登录态。initialUser 由根 layout 在服务端解析后传入 → 首屏 state 即正确、零闪烁。
 * 之后导航读 Context、不再调后端；登录/登出/改信息只需 setUser，UI 自动同步。
 */
export function AuthProvider({ initialUser, children }: { initialUser: AuthUser; children: ReactNode }) {
  const [user, setUser] = useState<AuthUser>(initialUser);

  async function refresh() {
    try {
      const me = await api.me();
      setUser({ userId: me.userId, email: me.identities.find((i) => i.provider === "email_otp")?.account ?? null });
    } catch {
      setUser(null);
    }
  }

  return <AuthContext.Provider value={{ user, setUser, refresh }}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth 必须在 AuthProvider 内使用");
  return ctx;
}
