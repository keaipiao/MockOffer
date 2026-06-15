"use client";

import TopNav from "@/components/TopNav";
import { useAuth } from "@/components/AuthProvider";

export default function Home() {
  const { user } = useAuth();

  return (
    <div className="scene-bg flex flex-1 flex-col">
      <TopNav active="workspace" />
      {user ? <Workspace /> : <Landing />}
    </div>
  );
}

/** 登录后的工作台：M1 仅账号体系，具体功能区后续里程碑填充。 */
function Workspace() {
  return (
    <main className="flex flex-1 flex-col items-center justify-center px-6 text-center">
      <h1 className="text-3xl font-bold text-[#0F172A] sm:text-4xl">工作台</h1>
      <p className="mt-4 max-w-md text-[15px] text-[#64748B]">
        简历制作、人岗匹配、AI 模拟面试等功能区正在路上，敬请期待。
      </p>
    </main>
  );
}

/** 未登录落地页。 */
function Landing() {
  return (
    <main className="flex flex-1 flex-col items-center justify-center px-6 text-center">
      <span className="mb-6 rounded-full border border-black/10 bg-white/70 px-4 py-1.5 text-sm font-medium text-slate-600 backdrop-blur">
        M0 地基 · 脚手架已跑通
      </span>
      <h1 className="bg-gradient-to-r from-[#5B6CFF] to-[#22D3EE] bg-clip-text text-5xl font-bold tracking-tight text-transparent sm:text-6xl">
        模考Offer
      </h1>
      <p className="mt-4 max-w-md text-lg text-slate-600">
        面向求职者的全流程 AI 闭环平台 · 简历 · 人岗匹配 · 模拟面试
      </p>
      <p className="mt-10 text-sm text-slate-400">由灵计一动驱动开发 · mockoffer.com</p>
    </main>
  );
}
