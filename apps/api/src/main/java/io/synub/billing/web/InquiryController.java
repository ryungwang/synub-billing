package io.synub.billing.web;

import io.synub.billing.dto.Dtos.InquiryResultDto;
import io.synub.billing.service.InquiryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 문의(contact form) 접수 — 공개(비로그인) 엔드포인트.
 * IdentityFilter.PUBLIC_PREFIXES 에 "/inquiries" 등록으로 무토큰 통과.
 */
@RestController
@RequestMapping("/inquiries")
public class InquiryController {

    private final InquiryService inquiries;

    public InquiryController(InquiryService inquiries) {
        this.inquiries = inquiries;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public InquiryResultDto submit(@RequestParam String type,
                                   @RequestParam(required = false) String productCode,
                                   @RequestParam(required = false) String productLabel,
                                   @RequestParam(required = false) String name,
                                   @RequestParam String email,
                                   @RequestParam String message,
                                   @RequestParam(required = false) String website, // 허니팟
                                   @RequestPart(value = "attachment", required = false) MultipartFile attachment,
                                   HttpServletRequest request) throws IOException {
        byte[] bytes = (attachment != null && !attachment.isEmpty()) ? attachment.getBytes() : null;
        String filename = attachment != null ? attachment.getOriginalFilename() : null;
        return inquiries.submit(type, productCode, productLabel, name, email, message, website,
                bytes, filename, clientIp(request));
    }

    /** Caddy 프록시 뒤 실제 클라이언트 IP — X-Forwarded-For 첫 홉, 없으면 remoteAddr. */
    private static String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        return request.getRemoteAddr();
    }
}
