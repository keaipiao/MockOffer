"use client";

import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { api } from "@/lib/api";
import { useAuth } from "@/components/AuthProvider";

const ICON = {
  grid: (
    <svg viewBox="0 0 24 24" className="h-[18px] w-[18px]" fill="none" stroke="currentColor" strokeWidth={1.8}>
      <rect x="3.5" y="3.5" width="7" height="7" rx="1.6" /><rect x="13.5" y="3.5" width="7" height="7" rx="1.6" />
      <rect x="3.5" y="13.5" width="7" height="7" rx="1.6" /><rect x="13.5" y="13.5" width="7" height="7" rx="1.6" />
    </svg>
  ),
  gear: (
    <svg viewBox="0 0 24 24" className="h-[18px] w-[18px]" fill="none" stroke="currentColor" strokeWidth={1.8}>
      <path d="M10.5 3.5h3l.5 2.3 2.1.9 2-1.1 2.1 2.1-1.1 2 .9 2.1 2.3.5v3l-2.3.5-.9 2.1 1.1 2-2.1 2.1-2-1.1-2.1.9-.5 2.3h-3l-.5-2.3-2.1-.9-2 1.1-2.1-2.1 1.1-2-.9-2.1L2.7 13.5v-3l2.3-.5.9-2.1-1.1-2L6.9 3.8l2 1.1 2.1-.9z" strokeLinejoin="round" />
      <circle cx="12" cy="12" r="2.6" />
    </svg>
  ),
  logout: (
    <svg viewBox="0 0 24 24" className="h-[18px] w-[18px]" fill="none" stroke="currentColor" strokeWidth={1.8}>
      <path d="M15 4H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h8" strokeLinecap="round" />
      <path d="m16 15 4-3-4-3M11 12h9" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  ),
};

function Avatar({ size }: { size: number }) {
  return (
    <span className="grid place-items-center overflow-hidden rounded-full bg-gradient-to-br from-[#C7D2FE] to-[#A5F3FC]" style={{ width: size, height: size }}>
      <svg viewBox="0 0 24 24" style={{ width: size * 0.66, height: size * 0.66 }} className="text-white" fill="currentColor">
        <circle cx="12" cy="9" r="4" /><path d="M4 21c0-4.4 3.6-7 8-7s8 2.6 8 7" />
      </svg>
    </span>
  );
}

/** 全站顶部导航：玻璃胶囊选中态；登录态显示头像菜单，未登录显示「登录」。登录态来自 AuthProvider（SSR 注入，零闪烁）。 */
export default function TopNav({ active }: { active?: "workspace" | "account" }) {
  const router = useRouter();
  const { user, setUser } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!menuOpen) return;
    function onDown(e: MouseEvent) {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) setMenuOpen(false);
    }
    document.addEventListener("mousedown", onDown);
    return () => document.removeEventListener("mousedown", onDown);
  }, [menuOpen]);

  async function logout() {
    try {
      await api.logout();
    } finally {
      setUser(null);
      router.push("/login");
    }
  }

  const workspaceActive = active === "workspace";

  return (
    <header className="relative z-30 flex justify-center px-6 pt-5">
      <nav className="flex h-[60px] items-center gap-2 rounded-[30px] border border-white/90 bg-white/65 px-5 shadow-[0_8px_32px_rgba(15,23,42,0.10)] backdrop-blur-xl">
        <Link href="/"><Image src="/logo.png" alt="模考Offer" width={120} height={22} /></Link>
        <span className="mx-2 h-6 w-px bg-[#E2E8F0]" />
        <Link
          href="/"
          className={`flex items-center gap-1.5 px-4 py-2 text-[15px] font-semibold transition ${
            workspaceActive
              ? "rounded-full bg-white/80 text-[#4F46E5] shadow-[0_4px_14px_rgba(79,70,229,0.18)] ring-1 ring-white/90"
              : "rounded-full text-[#64748B] hover:text-[#0F172A]"
          }`}
        >
          {ICON.grid}{user ? "工作台" : "首页"}
        </Link>
        <div className="relative ml-2" ref={menuRef}>
          {user ? (
            <>
              <button onClick={() => setMenuOpen((o) => !o)} aria-label="用户菜单">
                <Avatar size={36} />
              </button>
              {menuOpen && (
                <div className="absolute right-0 top-12 z-50 w-60 rounded-2xl border border-white/90 bg-white/95 p-2 shadow-[0_8px_32px_rgba(15,23,42,0.14)] backdrop-blur-xl">
                  <div className="flex items-center gap-3 px-3 py-2.5">
                    <Avatar size={40} />
                    <div className="min-w-0">
                      <div className="truncate text-sm font-bold text-[#0F172A]">用户 #{user.userId}</div>
                      <div className="truncate text-xs text-[#64748B]">{user.email ?? "未绑定邮箱"}</div>
                    </div>
                  </div>
                  <div className="my-1.5 h-px bg-[#E2E8F0]" />
                  <Link
                    href="/account/settings"
                    className={`flex items-center gap-2.5 rounded-xl px-3 py-2.5 text-sm font-semibold ${
                      active === "account" ? "bg-[#4F46E5]/10 text-[#4F46E5]" : "text-[#0F172A] hover:bg-black/5"
                    }`}
                  >
                    {ICON.gear}账号设置
                  </Link>
                  <button onClick={logout} className="flex w-full items-center gap-2.5 rounded-xl px-3 py-2.5 text-sm font-semibold text-[#0F172A] hover:bg-black/5">
                    {ICON.logout}退出登录
                  </button>
                </div>
              )}
            </>
          ) : (
            <Link
              href="/login"
              className="inline-flex h-9 items-center rounded-full bg-gradient-to-r from-[#5B6CFF] to-[#22D3EE] px-5 text-sm font-semibold text-white shadow-[0_8px_18px_rgba(79,70,229,0.18)] transition hover:-translate-y-0.5"
            >
              登录
            </Link>
          )}
        </div>
      </nav>
    </header>
  );
}
