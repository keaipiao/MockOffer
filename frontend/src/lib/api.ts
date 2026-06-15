// 账号体系前端 API 客户端：fetch 带 cookie，统一解析 {code,msg,data}。
const API_BASE = process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost:8080";

export class ApiError extends Error {
  constructor(public code: number, message: string) {
    super(message);
  }
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const res = await fetch(`${API_BASE}/api${path}`, {
    method,
    credentials: "include",
    headers: body !== undefined ? { "Content-Type": "application/json" } : undefined,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });
  if (res.status === 401) {
    throw new ApiError(401, "未登录或会话已过期");
  }
  const json = await res.json();
  if (json.code !== 0) {
    throw new ApiError(json.code, json.msg ?? "请求失败");
  }
  return json.data as T;
}

export interface Identity {
  provider: string;
  account: string | null;
  lastUsedAt: string | null;
}

export interface Me {
  userId: number;
  identities: Identity[];
}

export const api = {
  sendEmailCode: (email: string) => request<null>("POST", "/auth/email/code", { email }),
  emailLogin: (email: string, code: string) =>
    request<Me>("POST", "/auth/email/login", { email, code }),
  me: () => request<Me>("GET", "/auth/me"),
  logout: () => request<null>("POST", "/auth/logout"),
  refresh: () => request<null>("POST", "/auth/refresh"),
  listIdentities: () => request<Identity[]>("GET", "/account/identities"),
  unbind: (provider: string) => request<null>("DELETE", `/account/identities/${provider}`),
  sendChangeEmailCode: (newEmail: string) =>
    request<null>("POST", "/account/identities/email/code", { newEmail, code: "" }),
  changeEmail: (newEmail: string, code: string) =>
    request<null>("PUT", "/account/identities/email", { newEmail, code }),
};

// 浏览器跳转类（非 fetch）
export const githubLoginUrl = `${API_BASE}/oauth2/authorization/github`;
export const githubBindUrl = `${API_BASE}/api/account/identities/github/bind`;
