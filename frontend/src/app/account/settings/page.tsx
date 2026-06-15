"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState, type ReactNode } from "react";
import { ApiError, Identity, api, githubBindUrl } from "@/lib/api";
import { useToast } from "@/components/Toast";
import TopNav from "@/components/TopNav";
import { useAuth } from "@/components/AuthProvider";

const ICON = {
  shield: (
    <svg viewBox="0 0 24 24" className="h-[18px] w-[18px]" fill="none" stroke="currentColor" strokeWidth={1.8}>
      <path d="M12 3 5 6v5c0 4.5 3 7.6 7 9 4-1.4 7-4.5 7-9V6l-7-3Z" strokeLinejoin="round" />
      <path d="m9 12 2 2 4-4" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  ),
  key: (
    <svg viewBox="0 0 24 24" className="h-[18px] w-[18px]" fill="none" stroke="currentColor" strokeWidth={1.8}>
      <rect x="5" y="10" width="14" height="9.5" rx="2" /><path d="M8 10V7.5a4 4 0 0 1 8 0V10" strokeLinecap="round" />
      <circle cx="12" cy="14.4" r="1.3" fill="currentColor" />
    </svg>
  ),
  bell: (
    <svg viewBox="0 0 24 24" className="h-[18px] w-[18px]" fill="none" stroke="currentColor" strokeWidth={1.8}>
      <path d="M6 9a6 6 0 0 1 12 0c0 5 2 6 2 6H4s2-1 2-6Z" strokeLinejoin="round" /><path d="M10 19a2 2 0 0 0 4 0" strokeLinecap="round" />
    </svg>
  ),
  lock: (
    <svg viewBox="0 0 24 24" className="h-[18px] w-[18px]" fill="none" stroke="currentColor" strokeWidth={1.8}>
      <rect x="5" y="10.5" width="14" height="9.5" rx="2" /><path d="M8 10.5V8a4 4 0 0 1 8 0v2.5" strokeLinecap="round" />
    </svg>
  ),
  sliders: (
    <svg viewBox="0 0 24 24" className="h-[18px] w-[18px]" fill="none" stroke="currentColor" strokeWidth={1.8}>
      <path d="M5 8h9M5 16h11" strokeLinecap="round" /><circle cx="17" cy="8" r="2.4" /><circle cx="9" cy="16" r="2.4" />
    </svg>
  ),
};

function Chevron({ up }: { up: boolean }) {
  return (
    <svg viewBox="0 0 24 24" className={`ml-auto h-4 w-4 text-[#94A3B8] transition-transform ${up ? "" : "rotate-180"}`} fill="none" stroke="currentColor" strokeWidth={2}>
      <path d="m6 14 6-6 6 6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

const METHOD_ICON: Record<string, ReactNode> = {
  email_otp: (
    <svg viewBox="0 0 24 24" className="h-[22px] w-[22px]" fill="none" stroke="currentColor" strokeWidth={1.8}>
      <rect x="3" y="5.5" width="18" height="13" rx="2.5" /><path d="m4 7 8 6 8-6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  ),
  github: (
    <svg viewBox="0 0 24 24" className="h-[22px] w-[22px]" fill="currentColor">
      <path d="M12 2.2c-5.4 0-9.8 4.4-9.8 9.8 0 4.3 2.8 8 6.7 9.3.5.1.7-.2.7-.5v-1.7c-2.7.6-3.3-1.3-3.3-1.3-.5-1.1-1.1-1.5-1.1-1.5-.9-.6.1-.6.1-.6 1 .1 1.5 1 1.5 1 .9 1.5 2.3 1.1 2.9.8.1-.6.3-1.1.6-1.3-2.2-.2-4.5-1.1-4.5-4.9 0-1.1.4-2 1-2.7-.1-.3-.4-1.3.1-2.6 0 0 .8-.3 2.7 1a9.3 9.3 0 0 1 4.9 0c1.9-1.3 2.7-1 2.7-1 .5 1.3.2 2.3.1 2.6.6.7 1 1.6 1 2.7 0 3.8-2.3 4.7-4.5 4.9.4.3.7.9.7 1.9v2.7c0 .3.2.6.7.5 3.9-1.3 6.7-5 6.7-9.3 0-5.4-4.4-9.8-9.8-9.8Z" />
    </svg>
  ),
  phone: (
    <svg viewBox="0 0 24 24" className="h-[22px] w-[22px]" fill="none" stroke="currentColor" strokeWidth={1.8}>
      <rect x="7" y="2.5" width="10" height="19" rx="2.5" /><path d="M11 18.5h2" strokeLinecap="round" />
    </svg>
  ),
  wechat: (
    <svg viewBox="0 0 24 24" className="h-[26px] w-[26px]" fill="currentColor">
      <path d="M9 4.5C5.4 4.5 2.5 7 2.5 10.1c0 1.8 1 3.4 2.5 4.5L4.4 17l2.7-1.3c.6.2 1.3.3 1.9.3l.5-.0a5 5 0 0 1-.1-1.1c0-3 2.8-5.3 6.4-5.3l.6.0C15.9 6.5 12.8 4.5 9 4.5Z" />
      <path d="M21.5 14.4c0-2.4-2.4-4.3-5.4-4.3s-5.4 1.9-5.4 4.3 2.4 4.3 5.4 4.3c.6 0 1.2-.1 1.8-.3l1.9 1-.5-1.7c1.4-.8 2.2-2 2.2-3.3Z" />
    </svg>
  ),
};

type Row = { key: string; label: string; soon?: boolean };
const ROWS: Row[] = [
  { key: "email_otp", label: "邮箱" },
  { key: "github", label: "GitHub" },
  { key: "phone", label: "手机号", soon: true },
  { key: "wechat", label: "微信", soon: true },
];

export default function SettingsPage() {
  const router = useRouter();
  const toast = useToast();
  const { setUser } = useAuth();
  const [identities, setIdentities] = useState<Identity[] | null>(null);
  const [expanded, setExpanded] = useState(true);
  const [confirmProvider, setConfirmProvider] = useState<string | null>(null);
  const [bindEmailOpen, setBindEmailOpen] = useState(false);

  async function load() {
    try {
      const me = await api.me();
      setIdentities(me.identities);
      // 顺便用最新数据刷新全局缓存，导航头像/邮箱保持同步
      setUser({ userId: me.userId, email: me.identities.find((i) => i.provider === "email_otp")?.account ?? null });
    } catch (e) {
      if (e instanceof ApiError && e.code === 401) {
        setUser(null);
        router.push("/login");
        return;
      }
      toast("加载失败，请稍后再试");
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function unbind(provider: string) {
    try {
      await api.unbind(provider);
      await load();
    } catch (e) {
      toast(e instanceof ApiError ? e.message : "操作失败");
    }
  }

  const find = (k: string) => identities?.find((i) => i.provider === k) ?? null;

  return (
    <div className="scene-bg flex flex-1 flex-col">
      <TopNav active="account" />

      <div className="mx-auto flex w-full max-w-[1040px] flex-1 flex-col px-6 py-6">
        <div className="grid flex-1 grid-cols-[232px_1fr] overflow-hidden rounded-3xl border border-white/90 bg-white/65 shadow-[0_8px_32px_rgba(15,23,42,0.10)] backdrop-blur-xl max-md:grid-cols-1">
          <aside className="border-r border-[#E2E8F0] p-3.5 max-md:border-b max-md:border-r-0">
            <button onClick={() => setExpanded((e) => !e)} className="flex w-full min-h-[44px] items-center gap-2.5 px-3 text-sm font-bold text-[#475569]">
              {ICON.shield}<span>账号与安全</span><Chevron up={expanded} />
            </button>
            {expanded && (
              <div className="ml-3 flex min-h-[46px] items-center gap-2.5 rounded-xl bg-[#4F46E5]/10 px-3 text-sm font-bold text-[#4F46E5]">{ICON.key}登录方式</div>
            )}
            <div className="flex min-h-[52px] items-center gap-2.5 px-3 text-sm font-semibold text-[#94A3B8]">{ICON.bell}<span className="flex flex-col leading-tight">消息通知<span className="text-[11px]">即将支持</span></span></div>
            <div className="flex min-h-[52px] items-center gap-2.5 px-3 text-sm font-semibold text-[#94A3B8]">{ICON.lock}<span className="flex flex-col leading-tight">隐私<span className="text-[11px]">即将支持</span></span></div>
            <div className="flex min-h-[52px] items-center gap-2.5 px-3 text-sm font-semibold text-[#94A3B8]">{ICON.sliders}<span className="flex flex-col leading-tight">偏好设置<span className="text-[11px]">即将支持</span></span></div>
          </aside>

          <section className="p-8 max-md:p-5">
            <h2 className="text-xl font-bold text-[#0F172A]">登录方式</h2>
            <p className="mt-2.5 mb-3.5 text-sm text-[#64748B]">至少保留一种登录方式</p>

            {identities === null ? (
              <p className="py-10 text-center text-sm text-[#94A3B8]">加载中…</p>
            ) : (
              <div className="border-t border-[#E2E8F0]">
                {ROWS.map((row) => {
                  const bound = find(row.key);
                  return (
                    <div key={row.key} className="grid min-h-[88px] grid-cols-[1fr_auto] items-center gap-5 border-b border-[#E2E8F0]">
                      <div className="flex items-center gap-4">
                        <span className={`grid h-11 w-11 place-items-center rounded-2xl border border-[#E2E8F0] bg-white/60 ${row.soon ? "text-[#94A3B8]" : "text-[#0F172A]"}`}>
                          {METHOD_ICON[row.key]}
                        </span>
                        <div>
                          <div className="flex items-center gap-2 text-[15px] font-bold text-[#0F172A]">
                            {row.label}
                            {!bound && <span className="rounded-full bg-[#64748B]/12 px-2.5 py-0.5 text-xs font-semibold text-[#64748B]">未绑定</span>}
                          </div>
                          {bound?.account && <div className="mt-0.5 text-[13px] text-[#64748B]">{bound.account}</div>}
                          {row.soon && <div className="mt-0.5 text-[13px] text-[#94A3B8]">即将支持</div>}
                        </div>
                      </div>
                      <div>
                        {bound ? (
                          <button onClick={() => setConfirmProvider(row.key)} className="h-9 rounded-xl border border-[#E2E8F0] bg-white/60 px-4 text-sm font-semibold text-[#64748B] transition hover:bg-white/90">解绑</button>
                        ) : row.soon ? (
                          <button disabled className="h-9 rounded-xl border border-[#E2E8F0] bg-[#E2E8F0]/40 px-4 text-sm font-semibold text-[#94A3B8]">即将支持</button>
                        ) : row.key === "github" ? (
                          <a href={githubBindUrl} className="inline-flex h-9 items-center rounded-xl border border-[#4F46E5] bg-[#4F46E5]/10 px-4 text-sm font-semibold text-[#4F46E5] transition hover:bg-[#4F46E5]/16">绑定</a>
                        ) : (
                          <button onClick={() => setBindEmailOpen(true)} className="inline-flex h-9 items-center rounded-xl border border-[#4F46E5] bg-[#4F46E5]/10 px-4 text-sm font-semibold text-[#4F46E5] transition hover:bg-[#4F46E5]/16">绑定</button>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </section>
        </div>
      </div>

      {confirmProvider && (
        <div className="fixed inset-0 z-[90] grid place-items-center bg-black/30 px-4 backdrop-blur-sm" onClick={() => setConfirmProvider(null)}>
          <div className="w-full max-w-sm rounded-2xl border border-white/90 bg-white/95 p-6 shadow-[0_16px_48px_rgba(15,23,42,0.20)] backdrop-blur-xl" onClick={(e) => e.stopPropagation()}>
            <h3 className="text-lg font-bold text-[#0F172A]">解绑确认</h3>
            <p className="mt-2 text-sm leading-relaxed text-[#64748B]">
              确定解绑「{ROWS.find((r) => r.key === confirmProvider)?.label}」吗？解绑后将无法用它登录。
            </p>
            <div className="mt-6 flex justify-end gap-3">
              <button onClick={() => setConfirmProvider(null)} className="h-10 rounded-xl border border-[#E2E8F0] bg-white px-5 text-sm font-semibold text-[#64748B] transition hover:bg-black/5">取消</button>
              <button
                onClick={() => { const p = confirmProvider; setConfirmProvider(null); unbind(p); }}
                className="h-10 rounded-xl bg-[#EF4444] px-5 text-sm font-semibold text-white transition hover:bg-[#dc2626]"
              >
                解绑
              </button>
            </div>
          </div>
        </div>
      )}

      {bindEmailOpen && (
        <BindEmailModal onClose={() => setBindEmailOpen(false)} onDone={() => { setBindEmailOpen(false); load(); }} />
      )}
    </div>
  );
}

/** 绑定 / 换绑邮箱弹窗：一步式（邮箱 + 验证码 + 绑定）。复用换绑邮箱接口（无邮箱时自动新增）。 */
function BindEmailModal({ onClose, onDone }: { onClose: () => void; onDone: () => void }) {
  const toast = useToast();
  const [email, setEmail] = useState("");
  const [code, setCode] = useState("");
  const [sending, setSending] = useState(false);
  const [binding, setBinding] = useState(false);
  const [countdown, setCountdown] = useState(0);

  useEffect(() => {
    if (countdown <= 0) return;
    const t = setTimeout(() => setCountdown((c) => c - 1), 1000);
    return () => clearTimeout(t);
  }, [countdown]);

  const emailValid = /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email);

  async function sendCode() {
    if (!emailValid) {
      toast("请输入正确的邮箱");
      return;
    }
    setSending(true);
    try {
      await api.sendChangeEmailCode(email);
      setCountdown(60);
    } catch (e) {
      toast(e instanceof ApiError ? e.message : "发送失败，请稍后再试");
    } finally {
      setSending(false);
    }
  }

  async function submit() {
    if (!emailValid) {
      toast("请输入正确的邮箱");
      return;
    }
    if (code.length !== 6) {
      toast("请输入 6 位验证码");
      return;
    }
    setBinding(true);
    try {
      await api.changeEmail(email, code);
      onDone();
    } catch (e) {
      toast(e instanceof ApiError ? e.message : "绑定失败，请稍后再试");
    } finally {
      setBinding(false);
    }
  }

  return (
    <div className="fixed inset-0 z-[90] grid place-items-center bg-black/30 px-4 backdrop-blur-sm">
      <div className="relative w-full max-w-sm rounded-2xl border border-white/90 bg-white/95 p-6 shadow-[0_16px_48px_rgba(15,23,42,0.20)] backdrop-blur-xl">
        <button onClick={onClose} aria-label="关闭" className="absolute right-4 top-4 grid h-8 w-8 place-items-center rounded-full text-[#94A3B8] transition hover:bg-black/5 hover:text-[#0F172A]">
          <svg viewBox="0 0 24 24" className="h-4 w-4" fill="none" stroke="currentColor" strokeWidth={2}><path d="M6 6l12 12M18 6 6 18" strokeLinecap="round" /></svg>
        </button>
        <h3 className="text-lg font-bold text-[#0F172A]">绑定邮箱</h3>
        <p className="mt-2 mb-4 text-sm text-[#64748B]">绑定后可用邮箱验证码登录。</p>

        <div className="flex h-12 items-center gap-3 rounded-xl border border-[#E2E8F0] bg-white/60 px-3.5 transition focus-within:border-[#4F46E5] focus-within:ring-4 focus-within:ring-[#5B6CFF]/15">
          <svg viewBox="0 0 24 24" className="h-5 w-5 shrink-0 text-[#94A3B8]" fill="none" stroke="currentColor" strokeWidth={1.8}>
            <rect x="3" y="5.5" width="18" height="13" rx="2.5" /><path d="m4 7 8 6 8-6" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="邮箱"
            className="h-full w-full bg-transparent text-base text-[#0F172A] outline-none placeholder:text-[#94A3B8]"
          />
        </div>

        <div className="mt-3 flex gap-3">
          <div className="flex h-12 flex-1 items-center gap-3 rounded-xl border border-[#E2E8F0] bg-white/60 px-3.5 transition focus-within:border-[#4F46E5] focus-within:ring-4 focus-within:ring-[#5B6CFF]/15">
            <svg viewBox="0 0 24 24" className="h-5 w-5 shrink-0 text-[#94A3B8]" fill="none" stroke="currentColor" strokeWidth={1.8}>
              <rect x="5" y="10.5" width="14" height="9.5" rx="2" /><path d="M8 10.5V8a4 4 0 0 1 8 0v2.5" strokeLinecap="round" />
            </svg>
            <input
              inputMode="numeric"
              maxLength={6}
              value={code}
              onChange={(e) => setCode(e.target.value.replace(/\D/g, "").slice(0, 6))}
              placeholder="验证码"
              className="h-full w-full bg-transparent text-base text-[#0F172A] outline-none placeholder:text-[#94A3B8]"
            />
          </div>
          <button
            onClick={sendCode}
            disabled={sending || countdown > 0 || !emailValid}
            className="h-12 w-[108px] shrink-0 rounded-xl border border-[#4F46E5] bg-[#4F46E5]/10 text-center text-sm font-semibold text-[#4F46E5] transition hover:bg-[#4F46E5]/16 disabled:border-[#E2E8F0] disabled:bg-transparent disabled:text-[#94A3B8]"
          >
            {countdown > 0 ? `${countdown}秒后重试` : sending ? "发送中…" : "获取验证码"}
          </button>
        </div>

        <button
          onClick={submit}
          disabled={binding}
          className="mt-5 h-12 w-full rounded-xl bg-gradient-to-r from-[#5B6CFF] to-[#22D3EE] text-base font-semibold text-white shadow-[0_12px_24px_rgba(79,70,229,0.18)] transition hover:-translate-y-0.5 disabled:opacity-60"
        >
          {binding ? "绑定中…" : "绑定邮箱"}
        </button>
      </div>
    </div>
  );
}
