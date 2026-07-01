import type { SubStatus, PayStatus } from "@/components/status-badge";

export type BillingCycle = "monthly" | "yearly";

export interface Plan {
  code: string;
  name: string;
  amount: number;
  cycle: BillingCycle;
  tagline: string;
  features: string[];
  highlight?: boolean;
}

export interface Product {
  serviceCode: string;
  name: string;
  category: string;
  description: string;
  domain: string;
  emoji: string;
  accent: string; // tailwind text/bg accent override (decorative)
  subscribers: number;
  plans: Plan[];
}

export const products: Product[] = [
  {
    serviceCode: "doc-analysis",
    name: "문서분석 AI",
    category: "생산성",
    description:
      "계약서·보고서를 업로드하면 핵심 조항과 리스크를 자동 요약·추출합니다.",
    domain: "docs.synub.io",
    emoji: "📄",
    accent: "#3182f6",
    subscribers: 1284,
    plans: [
      {
        code: "basic",
        name: "Basic",
        amount: 19000,
        cycle: "monthly",
        tagline: "개인·소규모 팀",
        features: ["월 200건 분석", "기본 요약 템플릿", "이메일 지원"],
      },
      {
        code: "pro",
        name: "Pro",
        amount: 29000,
        cycle: "monthly",
        tagline: "성장하는 팀에 최적",
        features: [
          "무제한 문서 분석",
          "리스크 하이라이트",
          "API 액세스",
          "우선 지원",
        ],
        highlight: true,
      },
      {
        code: "enterprise",
        name: "Enterprise",
        amount: 99000,
        cycle: "monthly",
        tagline: "대규모 조직",
        features: ["전용 인스턴스", "SSO·감사 로그", "전담 매니저", "SLA 보장"],
      },
    ],
  },
  {
    serviceCode: "threads",
    name: "스레드 자동생성",
    category: "마케팅",
    description:
      "키워드 하나로 SNS 스레드·게시물 초안을 자동 생성하고 예약 발행합니다.",
    domain: "threads.synub.io",
    emoji: "🧵",
    accent: "#15b886",
    subscribers: 873,
    plans: [
      {
        code: "basic",
        name: "Basic",
        amount: 15000,
        cycle: "monthly",
        tagline: "크리에이터 입문",
        features: ["월 50개 스레드", "1개 채널 연동", "기본 템플릿"],
      },
      {
        code: "pro",
        name: "Pro",
        amount: 25000,
        cycle: "monthly",
        tagline: "전문 크리에이터",
        features: ["무제한 스레드", "5개 채널 연동", "예약 발행", "성과 분석"],
        highlight: true,
      },
    ],
  },
  {
    serviceCode: "office",
    name: "회계 자동화",
    category: "재무",
    description:
      "증빙 업로드부터 자동 분개·전표 생성, 부가세 신고 자료까지 한 번에.",
    domain: "office.synub.io",
    emoji: "🧾",
    accent: "#8b5cf6",
    subscribers: 512,
    plans: [
      {
        code: "pro",
        name: "Pro",
        amount: 39000,
        cycle: "monthly",
        tagline: "법인·개인사업자",
        features: ["자동 분개", "전표 관리", "부가세 신고자료", "회계사 공유"],
        highlight: true,
      },
      {
        code: "yearly",
        name: "Pro 연간",
        amount: 390000,
        cycle: "yearly",
        tagline: "2개월 무료 (17% 할인)",
        features: ["Pro 전체 기능", "연간 일괄 결제", "우선 지원"],
      },
    ],
  },
  {
    serviceCode: "desk",
    name: "고객지원 데스크",
    category: "CS",
    description:
      "문의를 AI가 1차 분류·답변 추천하고, 상담 이력을 자동 정리합니다.",
    domain: "desk.synub.io",
    emoji: "💬",
    accent: "#ff8a00",
    subscribers: 327,
    plans: [
      {
        code: "basic",
        name: "Basic",
        amount: 22000,
        cycle: "monthly",
        tagline: "1~3인 CS팀",
        features: ["월 1,000 문의", "AI 답변 추천", "기본 리포트"],
        highlight: true,
      },
    ],
  },
];

export interface Card {
  id: number;
  company: string;
  last4: string;
  type: string;
  expiry: string;
  isPrimary: boolean;
  brandColor: string;
}

export const cards: Card[] = [
  {
    id: 1,
    company: "신한카드",
    last4: "4921",
    type: "신용",
    expiry: "09/27",
    isPrimary: true,
    brandColor: "#1b64da",
  },
  {
    id: 2,
    company: "현대카드",
    last4: "1056",
    type: "체크",
    expiry: "03/26",
    isPrimary: false,
    brandColor: "#111827",
  },
];

export interface Usage {
  label: string;
  unit: string;
  used: number;
  limit: number | null; // null = 무제한
}

export interface Subscription {
  id: number;
  product: string;
  emoji: string;
  plan: string;
  amount: number;
  cycle: BillingCycle;
  status: SubStatus;
  startedAt: string;
  nextBillingDate: string;
  card: string;
  cancelAtPeriodEnd: boolean;
  usage: Usage;
  monthsActive: number;
}

export const subscriptions: Subscription[] = [
  {
    id: 101,
    product: "문서분석 AI",
    emoji: "📄",
    plan: "Pro",
    amount: 29000,
    cycle: "monthly",
    status: "active",
    startedAt: "2026-01-12",
    nextBillingDate: "2026-07-12",
    card: "신한카드 ····4921",
    cancelAtPeriodEnd: false,
    monthsActive: 6,
    usage: { label: "문서 분석", unit: "건", used: 642, limit: 2000 },
  },
  {
    id: 102,
    product: "회계 자동화",
    emoji: "🧾",
    plan: "Pro 연간",
    amount: 390000,
    cycle: "yearly",
    status: "active",
    startedAt: "2026-03-01",
    nextBillingDate: "2027-03-01",
    card: "신한카드 ····4921",
    cancelAtPeriodEnd: false,
    monthsActive: 4,
    usage: { label: "전표 처리", unit: "건", used: 1820, limit: 5000 },
  },
  {
    id: 103,
    product: "스레드 자동생성",
    emoji: "🧵",
    plan: "Pro",
    amount: 25000,
    cycle: "monthly",
    status: "past_due",
    startedAt: "2026-02-20",
    nextBillingDate: "2026-06-20",
    card: "현대카드 ····1056",
    cancelAtPeriodEnd: false,
    monthsActive: 4,
    usage: { label: "스레드 생성", unit: "개", used: 47, limit: 50 },
  },
  {
    id: 104,
    product: "고객지원 데스크",
    emoji: "💬",
    plan: "Basic",
    amount: 22000,
    cycle: "monthly",
    status: "canceled",
    startedAt: "2025-11-05",
    nextBillingDate: "2026-07-05",
    card: "현대카드 ····1056",
    cancelAtPeriodEnd: true,
    monthsActive: 8,
    usage: { label: "문의 처리", unit: "건", used: 312, limit: 1000 },
  },
];

/** 최근 6개월 결제 합계 (대시보드 지출 추이 차트) */
export const spendHistory: { month: string; amount: number }[] = [
  { month: "1월", amount: 29000 },
  { month: "2월", amount: 54000 },
  { month: "3월", amount: 444000 },
  { month: "4월", amount: 54000 },
  { month: "5월", amount: 54000 },
  { month: "6월", amount: 76000 },
];

export interface Payment {
  id: string;
  product: string;
  emoji: string;
  plan: string;
  amount: number;
  status: PayStatus;
  date: string;
  method: string;
  receiptNo: string;
}

export const payments: Payment[] = [
  {
    id: "PAY-2026-0612",
    product: "문서분석 AI",
    emoji: "📄",
    plan: "Pro",
    amount: 29000,
    status: "paid",
    date: "2026-06-12T09:30:00",
    method: "신한카드 ····4921",
    receiptNo: "20260612-0001",
  },
  {
    id: "PAY-2026-0620",
    product: "스레드 자동생성",
    emoji: "🧵",
    plan: "Pro",
    amount: 25000,
    status: "failed",
    date: "2026-06-20T03:10:00",
    method: "현대카드 ····1056",
    receiptNo: "—",
  },
  {
    id: "PAY-2026-0601",
    product: "고객지원 데스크",
    emoji: "💬",
    plan: "Basic",
    amount: 22000,
    status: "refunded",
    date: "2026-06-01T14:05:00",
    method: "현대카드 ····1056",
    receiptNo: "20260601-0007",
  },
  {
    id: "PAY-2026-0512",
    product: "문서분석 AI",
    emoji: "📄",
    plan: "Pro",
    amount: 29000,
    status: "paid",
    date: "2026-05-12T09:30:00",
    method: "신한카드 ····4921",
    receiptNo: "20260512-0001",
  },
  {
    id: "PAY-2026-0301",
    product: "회계 자동화",
    emoji: "🧾",
    plan: "Pro 연간",
    amount: 390000,
    status: "paid",
    date: "2026-03-01T00:05:00",
    method: "신한카드 ····4921",
    receiptNo: "20260301-0042",
  },
  {
    id: "PAY-2026-0420",
    product: "스레드 자동생성",
    emoji: "🧵",
    plan: "Pro",
    amount: 25000,
    status: "paid",
    date: "2026-04-20T03:10:00",
    method: "현대카드 ····1056",
    receiptNo: "20260420-0018",
  },
];

export const summary = {
  activeCount: subscriptions.filter((s) => s.status === "active").length,
  monthlyTotal: subscriptions
    .filter((s) => s.status === "active" && s.cycle === "monthly")
    .reduce((sum, s) => sum + s.amount, 0),
  nextBilling: subscriptions
    .filter((s) => s.status === "active")
    .sort(
      (a, b) =>
        new Date(a.nextBillingDate).getTime() -
        new Date(b.nextBillingDate).getTime()
    )[0],
  paidThisYear: payments
    .filter((p) => p.status === "paid")
    .reduce((sum, p) => sum + p.amount, 0),
  // 연간 결제로 절약한 금액 (월간 대비 2개월 무료)
  savedByYearly: subscriptions
    .filter((s) => s.status === "active" && s.cycle === "yearly")
    .reduce((sum, s) => sum + Math.round(s.amount / 10) * 2, 0),
};
