import type { Metadata } from "next";
import Link from "next/link";
import { ChevronDown, MessageCircleQuestion } from "lucide-react";

export const metadata: Metadata = {
  title: "자주 묻는 질문 — Synub Billing",
  description:
    "구독·결제, 플랜 변경·해지, 계정·조직 등 Synub Billing 이용에 대한 자주 묻는 질문 모음.",
};

type QA = { q: string; a: string };
type Group = { title: string; items: QA[] };

// 실제 구현 동작 기준으로 작성(과장 없음): 컨텍스트 entitlement·플랜변경 다음주기·예약해지/철회·연간 2개월무료·VAT포함 등.
const GROUPS: Group[] = [
  {
    title: "구독·결제",
    items: [
      {
        q: "요금은 어떻게 청구되나요?",
        a: "선택한 요금제에 따라 매 결제주기(월간 또는 연간)마다 등록된 카드로 자동 결제·갱신됩니다. 화면에 표시된 모든 금액은 부가세(VAT)가 포함된 원화(KRW) 기준입니다.",
      },
      {
        q: "월간과 연간 중 무엇이 유리한가요?",
        a: "연간 결제에는 2개월 무료 혜택(약 17% 할인)이 적용됩니다. 월 요금의 10개월치로 12개월을 이용하는 셈이라, 오래 쓸수록 연간이 유리합니다.",
      },
      {
        q: "어떤 결제 수단을 지원하나요?",
        a: "국내 신용카드·체크카드를 지원합니다. 카드를 한 번 등록하면 매 결제주기에 자동으로 결제되며, 결제 수단은 언제든 변경할 수 있습니다.",
      },
      {
        q: "결제 내역과 영수증은 어디서 확인하나요?",
        a: "로그인 후 '결제 내역'에서 모든 결제 시도와 영수증을 확인하고 내려받을 수 있습니다.",
      },
      {
        q: "결제가 실패하면 어떻게 되나요?",
        a: "일정 기간 동안 자동으로 재시도되며, 그동안은 유예 상태로 안내드립니다. 재시도가 모두 실패하면 구독이 일시 정지될 수 있으니 결제 수단을 갱신해 주세요.",
      },
    ],
  },
  {
    title: "플랜 변경·해지",
    items: [
      {
        q: "플랜을 올리거나 내릴 수 있나요?",
        a: "네, 언제든 상위·하위 플랜으로 변경할 수 있습니다. 변경 사항은 현재 주기가 끝난 뒤 다음 결제주기부터 반영됩니다.",
      },
      {
        q: "구독을 해지하면 바로 중단되나요?",
        a: "아니요. 해지는 '예약 해지' 방식으로, 현재 결제주기가 끝나는 시점에 종료됩니다. 남은 기간에는 서비스를 계속 이용할 수 있고 자동 갱신만 멈춥니다.",
      },
      {
        q: "해지를 취소할 수 있나요?",
        a: "이용 기간이 끝나기 전이라면 '해지 철회'로 언제든 다시 되돌릴 수 있습니다. 별도 재결제 없이 구독이 그대로 유지됩니다.",
      },
      {
        q: "환불은 어떻게 되나요?",
        a: "결제 후 서비스를 이용하지 않았다면 7일 이내 청약철회로 전액 환불이 가능합니다. 연간 구독을 중도 해지하면 이용한 기간을 일할 계산해 잔액을 환불합니다. 자세한 기준은 '환불·청약철회 정책'을 참고하세요.",
      },
    ],
  },
  {
    title: "계정·조직",
    items: [
      {
        q: "계정 하나로 여러 제품을 이용할 수 있나요?",
        a: "네. 통합계정(SSO) 하나로 Synub의 모든 SaaS 제품을 구독하고 한곳에서 관리할 수 있습니다.",
      },
      {
        q: "개인 구독과 회사(조직) 구독을 함께 쓸 수 있나요?",
        a: "가능합니다. 개인·회사 컨텍스트를 선택해 각각 구독할 수 있고, 서비스를 이용할 때 어느 컨텍스트로 쓸지 고를 수 있습니다.",
      },
      {
        q: "'조직 전용' 제품은 무엇인가요?",
        a: "회사(조직) 계정에서만 구독할 수 있는 제품입니다. 개인 컨텍스트에서는 구독이 제한되며, 상단에서 회사로 전환하면 구독할 수 있습니다.",
      },
    ],
  },
  {
    title: "시작하기",
    items: [
      {
        q: "먼저 체험해볼 수 있나요?",
        a: "로그인 화면의 '데모 계정으로 둘러보기'로 가입 없이 서비스를 둘러볼 수 있고, 일부 제품은 무료(Free) 플랜을 제공합니다.",
      },
      {
        q: "더 궁금한 점이 있어요.",
        a: "'문의하기'에서 이메일 또는 온라인 문의 폼으로 언제든 문의해 주세요. 최대한 빠르게 답변드리겠습니다.",
      },
    ],
  },
];

const FAQ_JSONLD = {
  "@context": "https://schema.org",
  "@type": "FAQPage",
  mainEntity: GROUPS.flatMap((g) =>
    g.items.map((it) => ({
      "@type": "Question",
      name: it.q,
      acceptedAnswer: { "@type": "Answer", text: it.a },
    }))
  ),
};

export default function FaqPage() {
  return (
    <div className="mx-auto max-w-3xl">
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(FAQ_JSONLD) }}
      />

      <header>
        <h1 className="text-2xl font-extrabold tracking-tight">자주 묻는 질문</h1>
        <p className="mt-2 text-sm text-muted-foreground">
          구독·결제부터 플랜 변경·해지까지, 자주 묻는 내용을 모았어요. 찾는
          답이 없다면{" "}
          <Link href="/contact" className="font-semibold text-primary hover:underline">
            문의하기
          </Link>
          로 알려주세요.
        </p>
      </header>

      <div className="mt-8 space-y-10">
        {GROUPS.map((group) => (
          <section key={group.title}>
            <h2 className="mb-2 text-lg font-extrabold tracking-tight">
              {group.title}
            </h2>
            <div className="rounded-2xl border border-border bg-card px-5">
              {group.items.map((it, i) => (
                <details
                  key={it.q}
                  className={
                    "group py-1" +
                    (i > 0 ? " border-t border-border" : "")
                  }
                >
                  <summary className="flex cursor-pointer list-none items-center justify-between gap-4 py-4 text-[15px] font-semibold text-foreground [&::-webkit-details-marker]:hidden">
                    {it.q}
                    <ChevronDown className="size-5 shrink-0 text-muted-foreground transition-transform group-open:rotate-180" />
                  </summary>
                  <p className="pb-4 pr-8 text-sm leading-relaxed text-muted-foreground">
                    {it.a}
                  </p>
                </details>
              ))}
            </div>
          </section>
        ))}
      </div>

      {/* 문의 유도 */}
      <div className="mt-12 flex flex-col items-start gap-3 rounded-2xl border border-border bg-muted/30 px-6 py-5 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-3">
          <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-primary-subtle text-primary">
            <MessageCircleQuestion className="size-5" />
          </span>
          <div>
            <p className="text-sm font-bold">원하는 답을 못 찾으셨나요?</p>
            <p className="text-[13px] text-muted-foreground">
              담당자가 이메일·온라인 문의로 도와드릴게요.
            </p>
          </div>
        </div>
        <Link
          href="/contact"
          className="inline-flex shrink-0 items-center rounded-full bg-primary px-5 py-2.5 text-sm font-bold text-primary-foreground transition-opacity hover:opacity-90"
        >
          문의하기
        </Link>
      </div>
    </div>
  );
}
