"use client";

import * as React from "react";
import { useSyncExternalStore } from "react";
import {
  Users,
  Building2,
  UserPlus,
  Mail,
  Loader2,
  AlertCircle,
  X,
  Crown,
  Wallet,
  User as UserIcon,
} from "lucide-react";
import { PageHeader } from "@/components/page-header";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { EmptyState } from "@/components/empty-state";
import {
  api,
  type ApiMember,
  type ApiInvitation,
  type OrgRole,
} from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { rawContext, subscribeContext, contextOrgId } from "@/lib/context";
import { cn } from "@/lib/utils";

const ROLE: Record<OrgRole, { label: string; icon: typeof Crown }> = {
  owner: { label: "소유자", icon: Crown },
  billing_manager: { label: "결제 관리자", icon: Wallet },
  member: { label: "멤버", icon: UserIcon },
};

export default function TeamPage() {
  const { user } = useAuth();
  const ctx = useSyncExternalStore(subscribeContext, rawContext, () => "personal");
  const orgId = contextOrgId(ctx);

  const [role, setRole] = React.useState<OrgRole | null>(null);
  const [members, setMembers] = React.useState<ApiMember[] | null>(null);
  const [invites, setInvites] = React.useState<ApiInvitation[]>([]);
  const [orgName, setOrgName] = React.useState<string>("");

  const isManager = role === "owner" || role === "billing_manager";
  const isOwner = role === "owner";

  const reload = React.useCallback(() => {
    if (orgId === null) return;
    api.organizations().then((orgs) => {
      const o = orgs.find((x) => x.id === orgId);
      setRole(o?.role ?? null);
      setOrgName(o?.name ?? "");
      if (o?.role === "owner" || o?.role === "billing_manager") {
        api.orgInvitations(orgId).then(setInvites).catch(() => setInvites([]));
      }
    });
    api.members(orgId).then(setMembers).catch(() => setMembers([]));
  }, [orgId]);

  React.useEffect(() => reload(), [reload]);

  if (orgId === null) {
    return (
      <>
        <PageHeader title="멤버" description="회사 단위로 팀원을 초대하고 역할을 관리하세요." />
        <EmptyState
          icon={Building2}
          title="회사 컨텍스트에서 멤버를 관리할 수 있어요"
          description="상단에서 회사를 선택하거나 새로 만든 뒤 팀원을 초대하세요."
        />
      </>
    );
  }

  return (
    <>
      <PageHeader
        title={`${orgName} 멤버`}
        description="팀원을 초대하고 역할(소유자·결제 관리자·멤버)을 관리하세요."
      />

      {isManager && (
        <InviteForm orgId={orgId} onInvited={reload} />
      )}

      {isManager && invites.length > 0 && (
        <Card className="mt-6 p-0">
          <div className="border-b border-border px-5 py-3.5 text-sm font-bold">
            대기 중인 초대 <span className="text-muted-foreground">{invites.length}</span>
          </div>
          <div className="divide-y divide-border">
            {invites.map((inv) => (
              <div key={inv.id} className="flex items-center gap-3 px-5 py-3.5">
                <span className="flex size-9 items-center justify-center rounded-full bg-muted text-muted-foreground">
                  <Mail className="size-[18px]" />
                </span>
                <div className="min-w-0 flex-1">
                  <div className="truncate text-sm font-semibold">{inv.email}</div>
                  <div className="text-xs text-muted-foreground">
                    {ROLE[inv.role].label}로 초대됨 · 수락 대기
                  </div>
                </div>
                <Button
                  variant="ghost"
                  size="sm"
                  className="text-muted-foreground hover:text-destructive"
                  onClick={() =>
                    api.cancelInvitation(orgId, inv.id).then(reload)
                  }
                >
                  <X className="size-4" />
                  취소
                </Button>
              </div>
            ))}
          </div>
        </Card>
      )}

      <Card className="mt-6 p-0">
        <div className="border-b border-border px-5 py-3.5 text-sm font-bold">
          멤버 {members ? <span className="text-muted-foreground">{members.length}</span> : null}
        </div>
        {members === null ? (
          <div className="flex items-center justify-center gap-2 py-12 text-muted-foreground">
            <Loader2 className="size-5 animate-spin" /> 불러오는 중…
          </div>
        ) : (
          <div className="divide-y divide-border">
            {members.map((m) => {
              const RoleIcon = ROLE[m.role].icon;
              const isMe = user?.sub === m.externalId;
              return (
                <div key={m.customerId} className="flex items-center gap-3 px-5 py-3.5">
                  <span className="flex size-9 items-center justify-center rounded-full bg-primary-subtle text-xs font-bold text-primary-subtle-foreground">
                    {(m.email[0] ?? "?").toUpperCase()}
                  </span>
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-1.5 truncate text-sm font-semibold">
                      {m.email}
                      {isMe && <span className="text-xs font-medium text-muted-foreground">(나)</span>}
                    </div>
                    <div className="flex items-center gap-1 text-xs text-muted-foreground">
                      <RoleIcon className="size-3.5" />
                      {ROLE[m.role].label}
                    </div>
                  </div>
                  {isOwner && !isMe && (
                    <div className="flex items-center gap-2">
                      <select
                        value={m.role}
                        onChange={(e) =>
                          api
                            .changeMemberRole(orgId, m.customerId, e.target.value as OrgRole)
                            .then(reload)
                        }
                        className="rounded-lg border border-border bg-background px-2 py-1.5 text-xs font-medium outline-none focus:border-primary"
                      >
                        <option value="member">멤버</option>
                        <option value="billing_manager">결제 관리자</option>
                        <option value="owner">소유자</option>
                      </select>
                      <Button
                        variant="ghost"
                        size="icon-sm"
                        className="text-muted-foreground hover:text-destructive"
                        onClick={() => api.removeMember(orgId, m.customerId).then(reload)}
                        aria-label="멤버 제거"
                      >
                        <X className="size-4" />
                      </Button>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </Card>
    </>
  );
}

function InviteForm({ orgId, onInvited }: { orgId: number; onInvited: () => void }) {
  const [email, setEmail] = React.useState("");
  const [role, setRole] = React.useState<OrgRole>("member");
  const [busy, setBusy] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const [ok, setOk] = React.useState(false);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    setOk(false);
    try {
      await api.invite(orgId, email.trim(), role);
      setEmail("");
      setOk(true);
      onInvited();
    } catch (err) {
      setError(err instanceof Error ? err.message : "초대에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <Card className="p-5">
      <div className="mb-3 flex items-center gap-2 text-sm font-bold">
        <UserPlus className="size-4 text-primary" />
        팀원 초대
      </div>
      <form onSubmit={submit} className="flex flex-col gap-2 sm:flex-row">
        <input
          type="email"
          value={email}
          onChange={(e) => {
            setEmail(e.target.value);
            setOk(false);
          }}
          placeholder="초대할 이메일 (통합계정 이메일)"
          required
          className="flex-1 rounded-xl border border-border bg-background px-3.5 py-2.5 text-sm outline-none transition-colors placeholder:text-muted-foreground focus:border-primary focus:ring-2 focus:ring-primary/20"
        />
        <select
          value={role}
          onChange={(e) => setRole(e.target.value as OrgRole)}
          className="rounded-xl border border-border bg-background px-3 py-2.5 text-sm font-medium outline-none focus:border-primary"
        >
          <option value="member">멤버</option>
          <option value="billing_manager">결제 관리자</option>
        </select>
        <Button type="submit" disabled={busy || !email.trim()}>
          {busy && <Loader2 className="animate-spin" />}
          초대
        </Button>
      </form>
      {error && (
        <div className="mt-2 flex items-center gap-2 text-sm font-medium text-destructive">
          <AlertCircle className="size-4 shrink-0" />
          {error}
        </div>
      )}
      {ok && (
        <p className="mt-2 text-sm font-medium text-success-foreground">
          초대를 보냈어요. 상대가 통합계정으로 로그인하면 수락할 수 있어요.
        </p>
      )}
      <p className={cn("mt-2 text-xs text-muted-foreground", (error || ok) && "hidden")}>
        초대한 이메일로 상대가 로그인하면 &ldquo;받은 초대&rdquo;에서 수락하고 팀에 합류합니다.
      </p>
    </Card>
  );
}
