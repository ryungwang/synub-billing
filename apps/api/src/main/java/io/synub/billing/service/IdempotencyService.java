package io.synub.billing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.synub.billing.domain.IdempotencyKey;
import io.synub.billing.repo.IdempotencyKeyRepository;
import io.synub.billing.web.ApiExceptions.ConflictException;
import io.synub.billing.web.ApiExceptions.NotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * 멱등 실행. 동일 (scope, Idempotency-Key) 재요청은 재실행하지 않고 저장된 응답을 반환한다.
 * 더블클릭·클라이언트 재시도로 인한 이중 청구를 막는다. 키가 없으면 그냥 실행(멱등 미적용).
 *
 * <p>트랜잭션 주의: execute 자체는 비트랜잭션이라 각 리포 호출이 독립 커밋된다.
 * (in_progress 레코드를 먼저 커밋 → 동시 요청이 이를 보고 409) 실제 작업(operation)은 자체 @Transactional.
 */
@Service
public class IdempotencyService {

    private final IdempotencyKeyRepository repo;
    private final ObjectMapper json;
    private final CurrentUser currentUser;

    public IdempotencyService(IdempotencyKeyRepository repo, ObjectMapper json, CurrentUser currentUser) {
        this.repo = repo;
        this.json = json;
        this.currentUser = currentUser;
    }

    public <T> T execute(String scope, String key, Class<T> type, Supplier<T> operation) {
        if (key == null || key.isBlank()) {
            return operation.get(); // 멱등키 없으면 그냥 실행
        }
        String customer = currentUser.externalId();

        Optional<IdempotencyKey> existing = repo.findByScopeAndIdemKey(scope, key);
        if (existing.isPresent()) {
            return replay(existing.get(), customer, type);
        }

        IdempotencyKey claim;
        try {
            claim = repo.saveAndFlush(new IdempotencyKey(scope, key, customer)); // in_progress 선점
        } catch (DataIntegrityViolationException race) {
            // 동시 요청이 먼저 선점함 — 그 레코드로 재생
            IdempotencyKey rec = repo.findByScopeAndIdemKey(scope, key)
                    .orElseThrow(() -> new ConflictException("이미 처리 중인 요청입니다."));
            return replay(rec, customer, type);
        }

        T result;
        try {
            result = operation.get();
        } catch (RuntimeException e) {
            // 실패 시 레코드 제거 → 동일 키로 재시도 허용
            repo.deleteById(claim.getId());
            throw e;
        }

        claim.complete(serialize(result));
        repo.save(claim);
        return result;
    }

    private <T> T replay(IdempotencyKey rec, String customer, Class<T> type) {
        if (!customer.equals(rec.getCustomer())) {
            // 다른 고객의 키 — 존재 자체를 노출하지 않는다
            throw new NotFoundException("요청을 찾을 수 없습니다.");
        }
        if (!rec.isCompleted()) {
            throw new ConflictException("동일 요청이 처리 중입니다. 잠시 후 다시 시도하세요.");
        }
        return deserialize(rec.getResponseBody(), type);
    }

    private String serialize(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("멱등 응답 직렬화 실패", e);
        }
    }

    private <T> T deserialize(String body, Class<T> type) {
        try {
            return json.readValue(body, type);
        } catch (Exception e) {
            throw new IllegalStateException("멱등 응답 역직렬화 실패", e);
        }
    }
}
