package io.synub.billing.service;

import io.synub.billing.auth.Identity;
import io.synub.billing.auth.IdentityContext;
import io.synub.billing.domain.Inquiry;
import io.synub.billing.dto.Dtos.AdminInquiryDto;
import io.synub.billing.dto.Dtos.InquiryResultDto;
import io.synub.billing.mail.InquiryMailer;
import io.synub.billing.repo.InquiryRepository;
import io.synub.billing.storage.StorageService;
import io.synub.billing.web.ApiExceptions.BadRequestException;
import io.synub.billing.web.ApiExceptions.ForbiddenException;
import io.synub.billing.web.ApiExceptions.NotFoundException;
import io.synub.billing.web.ApiExceptions.TooManyRequestsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 문의(contact form) 접수·조회. 제출은 공개(비로그인)라 남용 방지가 핵심:
 * 허니팟 봇 차단 · IP 레이트리밋 · 입력 길이 제한 · 첨부 매직바이트 화이트리스트 · 용량 상한.
 * 조회/다운로드/처리는 관리자(admin claim) 전용.
 */
@Service
public class InquiryService {

    /** 첨부 상한 10MB — spring.servlet.multipart.max-file-size 와 정합. */
    private static final int MAX_ATTACHMENT = 10 * 1024 * 1024;
    private static final int MAX_MESSAGE = 5000;
    private static final int MAX_NAME = 100;
    private static final int MAX_EMAIL = 254;
    private static final int MAX_TYPE = 40;
    private static final int MAX_PRODUCT_CODE = 64;
    private static final int MAX_PRODUCT_LABEL = 200;

    /** IP당 레이트리밋: 10분 창에서 최대 5건. */
    private static final long RATE_WINDOW_MS = 10 * 60 * 1000L;
    private static final int RATE_MAX = 5;
    private final ConcurrentHashMap<String, Deque<Long>> rate = new ConcurrentHashMap<>();

    private final InquiryRepository inquiries;
    private final StorageService storage;
    private final InquiryMailer mailer;
    private final CurrentUser currentUser;

    public InquiryService(InquiryRepository inquiries, StorageService storage,
                          InquiryMailer mailer, CurrentUser currentUser) {
        this.inquiries = inquiries;
        this.storage = storage;
        this.mailer = mailer;
        this.currentUser = currentUser;
    }

    // ---- 공개 제출 ----

    @Transactional
    public InquiryResultDto submit(String type, String productCode, String productLabel,
                                   String name, String email, String message, String honeypot,
                                   byte[] attachment, String attachmentFilename, String clientIp) {
        // 봇 차단(허니팟) — 숨김 필드가 채워졌으면 저장하지 않고 성공한 척(공격자에게 힌트 주지 않음).
        if (honeypot != null && !honeypot.isBlank()) {
            return new InquiryResultDto(null, true);
        }

        rateLimit(clientIp);

        String t = require(type, "문의 유형", MAX_TYPE);
        String mail = require(email, "회신 이메일", MAX_EMAIL);
        if (!mail.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new BadRequestException("올바른 이메일 주소를 입력하세요.");
        }
        String msg = require(message, "문의 내용", MAX_MESSAGE);
        String nm = optional(name, "이름", MAX_NAME);
        String pcode = optional(productCode, "제품 코드", MAX_PRODUCT_CODE);
        String plabel = optional(productLabel, "제품명", MAX_PRODUCT_LABEL);

        Inquiry inquiry = new Inquiry(t, pcode, plabel, nm, mail, msg, currentExternalId(), clientIp);

        // 첨부(선택) — 있으면 용량·유형 검증 후 스토리지 저장.
        if (attachment != null && attachment.length > 0) {
            if (attachment.length > MAX_ATTACHMENT) {
                throw new BadRequestException("첨부파일은 10MB 이하만 업로드할 수 있습니다.");
            }
            if (!isImageOrPdf(attachment)) {
                throw new BadRequestException("첨부파일은 이미지(JPG/PNG) 또는 PDF만 업로드할 수 있습니다.");
            }
            String key = storage.store(attachment, safeName(attachmentFilename));
            inquiry.attach(key, safeName(attachmentFilename), attachment.length);
        }

        inquiries.save(inquiry);
        mailer.notifyReceived(inquiry);
        return new InquiryResultDto(inquiry.getId(), true);
    }

    // ---- 관리자 ----

    @Transactional(readOnly = true)
    public List<AdminInquiryDto> list() {
        requireAdmin();
        return inquiries.findAllByOrderByIdDesc().stream().map(this::toDto).toList();
    }

    @Transactional
    public void resolve(Long id) {
        requireAdmin();
        inquiry(id).resolve();
    }

    /** 첨부 원본 열람(관리자). */
    @Transactional(readOnly = true)
    public Attachment attachment(Long id) {
        requireAdmin();
        Inquiry inq = inquiry(id);
        if (inq.getAttachmentKey() == null) {
            throw new NotFoundException("첨부파일이 없습니다.");
        }
        return new Attachment(storage.load(inq.getAttachmentKey()),
                contentType(inq.getAttachmentKey()),
                inq.getAttachmentFilename() == null ? "attachment" : inq.getAttachmentFilename());
    }

    public record Attachment(byte[] content, String contentType, String filename) {}

    private Inquiry inquiry(Long id) {
        return inquiries.findById(id)
                .orElseThrow(() -> new NotFoundException("문의를 찾을 수 없습니다."));
    }

    private void requireAdmin() {
        if (!currentUser.isAdmin()) {
            throw new ForbiddenException("관리자만 접근할 수 있습니다.");
        }
    }

    private AdminInquiryDto toDto(Inquiry i) {
        String created = i.getCreatedAt() == null ? null
                : LocalDateTime.ofInstant(i.getCreatedAt(), DtoMapper.KST)
                        .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return new AdminInquiryDto(i.getId(), i.getType(), i.getProductLabel(), i.getName(),
                i.getEmail(), i.getMessage(), i.getAttachmentKey() != null,
                i.getAttachmentFilename(), i.getAttachmentSize(), i.getStatus(),
                i.getExternalId(), i.getClientIp(), created);
    }

    // ---- 헬퍼 ----

    /** 로그인 상태면 external_id, 익명이면 null(dev-fallback 신원은 제출자로 기록하지 않음). */
    private String currentExternalId() {
        Identity id = IdentityContext.current();
        return id != null && id.externalId() != null && !id.externalId().isBlank()
                ? id.externalId() : null;
    }

    private void rateLimit(String ip) {
        if (ip == null || ip.isBlank()) return;
        long now = System.currentTimeMillis();
        Deque<Long> hits = rate.computeIfAbsent(ip, k -> new ArrayDeque<>());
        synchronized (hits) {
            while (!hits.isEmpty() && now - hits.peekFirst() > RATE_WINDOW_MS) {
                hits.pollFirst();
            }
            if (hits.size() >= RATE_MAX) {
                throw new TooManyRequestsException("문의가 너무 많이 접수되었습니다. 잠시 후 다시 시도해 주세요.");
            }
            hits.addLast(now);
        }
        // 메모리 누수 방지 — 창을 벗어나 비워진 IP 엔트리 정리.
        if (rate.size() > 10_000) {
            rate.entrySet().removeIf(e -> {
                synchronized (e.getValue()) {
                    return e.getValue().isEmpty()
                            || now - e.getValue().peekLast() > RATE_WINDOW_MS;
                }
            });
        }
    }

    private static String require(String v, String label, int max) {
        String s = v == null ? "" : v.trim();
        if (s.isEmpty()) throw new BadRequestException(label + "을(를) 입력하세요.");
        if (s.length() > max) throw new BadRequestException(label + "이(가) 너무 깁니다.");
        return s;
    }

    private static String optional(String v, String label, int max) {
        String s = v == null ? "" : v.trim();
        if (s.isEmpty()) return null;
        if (s.length() > max) throw new BadRequestException(label + "이(가) 너무 깁니다.");
        return s;
    }

    /** 원본 파일명에서 경로·제어문자 제거(스토리지 키는 확장자만 사용하지만 표시·다운로드 안전). */
    private static String safeName(String name) {
        if (name == null || name.isBlank()) return "attachment";
        String base = name.replaceAll("[\\\\/]", "_").replaceAll("[\\x00-\\x1f]", "").trim();
        if (base.length() > 200) base = base.substring(base.length() - 200);
        return base.isBlank() ? "attachment" : base;
    }

    /** 매직바이트 화이트리스트 — Content-Type/확장자 신뢰하지 않음(사업자등록증 업로드와 동일 규약). */
    private static boolean isImageOrPdf(byte[] b) {
        if (b.length < 4) return false;
        boolean pdf = b[0] == 0x25 && b[1] == 0x50 && b[2] == 0x44 && b[3] == 0x46;       // %PDF
        boolean png = (b[0] & 0xFF) == 0x89 && b[1] == 0x50 && b[2] == 0x4E && b[3] == 0x47; // \x89PNG
        boolean jpg = (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF; // JPEG
        return pdf || png || jpg;
    }

    private static String contentType(String key) {
        String k = key.toLowerCase();
        if (k.endsWith(".pdf")) return "application/pdf";
        if (k.endsWith(".png")) return "image/png";
        if (k.endsWith(".jpg") || k.endsWith(".jpeg")) return "image/jpeg";
        return "application/octet-stream";
    }
}
