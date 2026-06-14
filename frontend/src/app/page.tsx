export default function Home() {
  return (
    <main className="flex flex-1 flex-col items-center justify-center bg-[#F4F6FB] px-6 text-center">
      <span className="mb-6 rounded-full border border-black/10 bg-white/70 px-4 py-1.5 text-sm font-medium text-slate-600 backdrop-blur">
        M0 地基 · 脚手架已跑通
      </span>
      <h1 className="bg-gradient-to-r from-[#5B6CFF] to-[#22D3EE] bg-clip-text text-5xl font-bold tracking-tight text-transparent sm:text-6xl">
        模考Offer
      </h1>
      <p className="mt-4 max-w-md text-lg text-slate-600">
        面向求职者的全流程 AI 闭环平台 · 简历 · 人岗匹配 · 模拟面试
      </p>
      <p className="mt-10 text-sm text-slate-400">
        由灵计一动驱动开发 · mockoffer.com
      </p>
    </main>
  );
}
