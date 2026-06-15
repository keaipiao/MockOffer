"use client";

import { createContext, useCallback, useContext, useState } from "react";

type Toast = { id: number; msg: string; exiting?: boolean };

const ToastContext = createContext<(msg: string) => void>(() => {});

export function useToast() {
  return useContext(ToastContext);
}

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);

  // 先标记 exiting 触发退出动画，动画结束后再真正移除
  const dismiss = useCallback((id: number) => {
    setToasts((t) => t.map((x) => (x.id === id ? { ...x, exiting: true } : x)));
    setTimeout(() => setToasts((t) => t.filter((x) => x.id !== id)), 260);
  }, []);

  const show = useCallback((msg: string) => {
    const id = Date.now() + Math.random();
    setToasts((t) => [...t, { id, msg }]);
    setTimeout(() => dismiss(id), 5000);
  }, [dismiss]);

  return (
    <ToastContext.Provider value={show}>
      {children}
      <div className="pointer-events-none fixed left-1/2 top-5 z-[100] flex w-full max-w-[440px] -translate-x-1/2 flex-col items-center gap-2 px-4">
        {toasts.map((t) => (
          <div
            key={t.id}
            className={`${t.exiting ? "toast-out" : "toast-in"} pointer-events-auto flex w-full items-center gap-3 rounded-2xl border border-[#FECACA] bg-white/95 px-4 py-3.5 text-sm font-medium text-[#0F172A] shadow-[0_16px_48px_rgba(15,23,42,0.20)] backdrop-blur-xl`}
          >
            <span className="grid h-7 w-7 shrink-0 place-items-center rounded-full bg-[#FEE2E2] text-[#EF4444]">
              <svg viewBox="0 0 24 24" className="h-4 w-4" fill="none" stroke="currentColor" strokeWidth={2.2}>
                <path d="M12 8v5" strokeLinecap="round" />
                <circle cx="12" cy="16.5" r="0.6" fill="currentColor" />
                <circle cx="12" cy="12" r="9" />
              </svg>
            </span>
            <span className="flex-1">{t.msg}</span>
            <button
              onClick={() => dismiss(t.id)}
              className="shrink-0 text-[#94A3B8] transition hover:text-[#0F172A]"
              aria-label="关闭"
            >
              ✕
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}
