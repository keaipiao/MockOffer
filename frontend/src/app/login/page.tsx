"use client";

import Image from "next/image";
import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { ApiError, api, githubLoginUrl } from "@/lib/api";
import { useToast } from "@/components/Toast";
import { useAuth } from "@/components/AuthProvider";

export default function LoginPage() {
  const router = useRouter();
  const toast = useToast();
  const { setUser } = useAuth();
  const [step, setStep] = useState<1 | 2>(1);
  const [email, setEmail] = useState("");
  const [digits, setDigits] = useState(["", "", "", "", "", ""]);
  const [loading, setLoading] = useState(false);
  const [countdown, setCountdown] = useState(0);
  const refs = useRef<Array<HTMLInputElement | null>>([]);

  useEffect(() => {
    if (countdown <= 0) return;
    const t = setTimeout(() => setCountdown((c) => c - 1), 1000);
    return () => clearTimeout(t);
  }, [countdown]);

  async function sendCode() {
    if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email)) {
      toast("请输入正确的邮箱");
      return;
    }
    setLoading(true);
    try {
      await api.sendEmailCode(email);
      setStep(2);
      setCountdown(60);
      setDigits(["", "", "", "", "", ""]);
      setTimeout(() => refs.current[0]?.focus(), 50);
    } catch (e) {
      toast(e instanceof ApiError ? e.message : "发送失败，请稍后再试");
    } finally {
      setLoading(false);
    }
  }

  async function login() {
    const code = digits.join("");
    if (code.length !== 6) {
      toast("请输入 6 位验证码");
      return;
    }
    setLoading(true);
    try {
      const me = await api.emailLogin(email, code);
      setUser({ userId: me.userId, email: me.identities.find((i) => i.provider === "email_otp")?.account ?? null });
      router.push("/account/settings");
    } catch (e) {
      toast(e instanceof ApiError ? e.message : "登录失败，请稍后再试");
    } finally {
      setLoading(false);
    }
  }

  function onDigit(i: number, v: string) {
    const d = v.replace(/\D/g, "").slice(-1);
    setDigits((prev) => {
      const n = [...prev];
      n[i] = d;
      return n;
    });
    if (d && i < 5) refs.current[i + 1]?.focus();
  }

  function onKey(i: number, e: React.KeyboardEvent<HTMLInputElement>) {
    if (e.key === "Backspace" && !digits[i] && i > 0) refs.current[i - 1]?.focus();
  }

  return (
    <div className="scene-bg flex flex-1 items-center justify-center px-4 py-12">
      <div className="w-full max-w-[420px] rounded-3xl border border-white/90 bg-white/65 p-10 shadow-[0_8px_32px_rgba(15,23,42,0.10)] backdrop-blur-xl">
        {step === 1 ? (
          <>
            <div className="mb-6 flex justify-center">
              <Image src="/logo.png" alt="模考Offer" width={132} height={24} priority />
            </div>
            <h1 className="text-center text-3xl font-bold text-[#0F172A]">登录 / 注册</h1>
            <p className="mt-3 mb-8 text-center text-sm text-[#64748B]">首次登录将自动为你注册</p>

            <div className="flex h-14 items-center gap-3 rounded-xl border border-[#E2E8F0] bg-white/50 px-4 transition focus-within:border-[#4F46E5] focus-within:ring-4 focus-within:ring-[#5B6CFF]/15">
              <svg viewBox="0 0 24 24" className="h-5 w-5 shrink-0 text-[#94A3B8]" fill="none" stroke="currentColor" strokeWidth={1.8}>
                <rect x="3" y="5.5" width="18" height="13" rx="2.5" />
                <path d="m4 7 8 6 8-6" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="邮箱"
                className="h-full w-full bg-transparent text-base text-[#0F172A] outline-none placeholder:text-[#94A3B8]"
              />
            </div>

            <button
              onClick={sendCode}
              disabled={loading}
              className="mt-6 h-14 w-full rounded-xl bg-gradient-to-r from-[#5B6CFF] to-[#22D3EE] text-base font-semibold text-white shadow-[0_12px_24px_rgba(79,70,229,0.18)] transition hover:-translate-y-0.5 disabled:opacity-60"
            >
              {loading ? "发送中…" : "发送验证码"}
            </button>

            <div className="my-6 flex items-center gap-4 text-sm text-[#94A3B8]">
              <span className="h-px flex-1 bg-[#E2E8F0]" />或<span className="h-px flex-1 bg-[#E2E8F0]" />
            </div>

            <a
              href={githubLoginUrl}
              className="flex h-14 w-full items-center justify-center gap-3 rounded-xl border border-[#E2E8F0] bg-white/55 text-base font-semibold text-[#0F172A] transition hover:bg-white/90"
            >
              <svg viewBox="0 0 24 24" className="h-5 w-5" fill="currentColor" aria-hidden>
                <path d="M12 .5A11.5 11.5 0 0 0 8.36 22.9c.58.11.79-.25.79-.56v-2.1c-3.23.7-3.91-1.38-3.91-1.38-.53-1.34-1.29-1.7-1.29-1.7-1.06-.72.08-.71.08-.71 1.17.08 1.78 1.2 1.78 1.2 1.04 1.78 2.73 1.27 3.4.97.1-.75.41-1.27.74-1.56-2.58-.29-5.29-1.29-5.29-5.74 0-1.27.45-2.3 1.2-3.12-.12-.29-.52-1.48.11-3.08 0 0 .98-.31 3.2 1.19a11.1 11.1 0 0 1 5.82 0c2.22-1.5 3.2-1.19 3.2-1.19.63 1.6.23 2.79.11 3.08.75.82 1.2 1.85 1.2 3.12 0 4.46-2.72 5.45-5.31 5.74.42.36.79 1.07.79 2.16v3.12c0 .31.21.68.8.56A11.5 11.5 0 0 0 12 .5Z" />
              </svg>
              使用 GitHub 登录
            </a>

            <p className="mt-5 text-center text-[13px] text-[#64748B]">
              登录即代表同意
              <a href="#" className="font-semibold text-[#4F46E5]">服务条款</a>和
              <a href="#" className="font-semibold text-[#4F46E5]">隐私政策</a>
            </p>
          </>
        ) : (
          <>
            <button
              onClick={() => { setStep(1); setDigits(["", "", "", "", "", ""]); }}
              className="grid h-10 w-10 place-items-center rounded-full border border-[#E2E8F0] bg-white/60 text-[#0F172A] transition hover:bg-white/90"
              aria-label="返回修改邮箱"
            >
              <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth={2}>
                <path d="M15 18 9 12l6-6" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            </button>
            <h1 className="mt-4 text-center text-3xl font-bold text-[#0F172A]">输入验证码</h1>
            <p className="mt-3 mb-8 text-center text-sm text-[#64748B]">验证码已发送至 {email}</p>

            <div className="grid grid-cols-6 gap-3">
              {digits.map((d, i) => (
                <input
                  key={i}
                  ref={(el) => { refs.current[i] = el; }}
                  inputMode="numeric"
                  maxLength={1}
                  value={d}
                  onChange={(e) => onDigit(i, e.target.value)}
                  onKeyDown={(e) => onKey(i, e)}
                  className="aspect-square w-full rounded-xl border border-[#CBD5E1] bg-white/50 text-center text-2xl font-semibold text-[#0F172A] outline-none transition focus:border-[#4F46E5] focus:ring-4 focus:ring-[#5B6CFF]/15"
                />
              ))}
            </div>

            <button
              onClick={() => countdown <= 0 && sendCode()}
              disabled={countdown > 0}
              className="mt-4 block w-full text-center text-sm font-semibold text-[#4F46E5] disabled:text-[#94A3B8]"
            >
              {countdown > 0 ? `重新发送验证码 (${countdown}s)` : "重新发送验证码"}
            </button>

            <button
              onClick={login}
              disabled={loading}
              className="mt-4 h-14 w-full rounded-xl bg-gradient-to-r from-[#5B6CFF] to-[#22D3EE] text-base font-semibold text-white shadow-[0_12px_24px_rgba(79,70,229,0.18)] transition hover:-translate-y-0.5 disabled:opacity-60"
            >
              {loading ? "登录中…" : "登录 / 注册"}
            </button>
          </>
        )}
      </div>
    </div>
  );
}
