import { cookies } from "next/headers";
import type { Me } from "@/lib/api";

/** 仅服务端用：SSR 时 Next 服务器 → 后端的内网地址（dev 同为 localhost:8080，prod 容器内为 http://backend:8080）。 */
const BACKEND_INTERNAL_URL = process.env.BACKEND_INTERNAL_URL ?? "http://localhost:8080";

export type AuthUser = { userId: number; email: string | null } | null;

/**
 * 在服务端读 httpOnly cookie 并转发给后端 /me，解析当前用户。
 * 首屏渲染前就拿到登录态 → 导航直接带头像、零闪烁。失败一律当未登录。
 */
export async function getUser(): Promise<AuthUser> {
  const cookieHeader = (await cookies()).toString();
  if (!cookieHeader) return null;
  try {
    const res = await fetch(`${BACKEND_INTERNAL_URL}/api/auth/me`, {
      headers: { cookie: cookieHeader },
      cache: "no-store",
    });
    if (!res.ok) return null;
    const json = (await res.json()) as { code: number; data: Me | null };
    if (json.code !== 0 || !json.data) return null;
    const email = json.data.identities.find((i) => i.provider === "email_otp")?.account ?? null;
    return { userId: json.data.userId, email };
  } catch {
    return null;
  }
}
