import {
  CheckCircle2,
  AlertTriangle,
  PauseCircle,
  XCircle,
  RotateCcw,
} from "lucide-react";
import { Badge } from "@/components/ui/badge";

export type SubStatus =
  | "active"
  | "past_due"
  | "suspended"
  | "canceled";

export type PayStatus = "paid" | "failed" | "refunded" | "pending";

const SUB_MAP: Record<
  SubStatus,
  { label: string; variant: "success" | "warning" | "destructive" | "default"; Icon: typeof CheckCircle2 }
> = {
  active: { label: "이용 중", variant: "success", Icon: CheckCircle2 },
  past_due: { label: "결제 지연", variant: "warning", Icon: AlertTriangle },
  suspended: { label: "일시중지", variant: "destructive", Icon: PauseCircle },
  canceled: { label: "해지 예정", variant: "default", Icon: XCircle },
};

const PAY_MAP: Record<
  PayStatus,
  { label: string; variant: "success" | "destructive" | "default" | "primary"; Icon: typeof CheckCircle2 }
> = {
  paid: { label: "결제완료", variant: "success", Icon: CheckCircle2 },
  failed: { label: "실패", variant: "destructive", Icon: XCircle },
  refunded: { label: "환불", variant: "default", Icon: RotateCcw },
  pending: { label: "대기", variant: "primary", Icon: RotateCcw },
};

export function SubscriptionStatusBadge({ status }: { status: SubStatus }) {
  const { label, variant, Icon } = SUB_MAP[status];
  return (
    <Badge variant={variant}>
      <Icon className="size-3.5" />
      {label}
    </Badge>
  );
}

export function PaymentStatusBadge({ status }: { status: PayStatus }) {
  const { label, variant, Icon } = PAY_MAP[status];
  return (
    <Badge variant={variant}>
      <Icon className="size-3.5" />
      {label}
    </Badge>
  );
}
