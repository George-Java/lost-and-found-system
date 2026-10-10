package com.example.lostfound.service.impl;

import com.example.lostfound.common.BusinessException;
import com.example.lostfound.domain.entity.ClaimRecord;
import com.example.lostfound.domain.entity.LostItem;
import com.example.lostfound.messaging.AuditEventPublisher;
import com.example.lostfound.service.ClaimReviewService;
import com.example.lostfound.service.ClaimService;
import com.example.lostfound.service.ItemService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClaimReviewServiceImpl implements ClaimReviewService {

    private final ClaimService claimService;
    private final ItemService itemService;
    private final RedissonClient redissonClient;
    private final AuditEventPublisher auditEventPublisher;
    private final TransactionTemplate transactionTemplate;

    @Override
    public void review(Long claimId, String status, Long reviewerId, String reviewNote) {
        ClaimRecord initial = claimService.findDetail(claimId);
        if (initial == null) throw new BusinessException(404, "Claim not found");
        RLock lock = redissonClient.getLock("lostfound:claim-review:item:" + initial.getItemId());
        boolean locked = false;
        try {
            locked = lock.tryLock(3, 15, TimeUnit.SECONDS);
            if (!locked) throw new BusinessException(409, "Claim is being reviewed, please retry");
            transactionTemplate.executeWithoutResult(tx -> reviewInTransaction(claimId, status, reviewerId, reviewNote));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException(503, "Review interrupted, please retry");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) lock.unlock();
        }
    }

    @Override
    public void reviewInTransaction(Long claimId, String status, Long reviewerId, String reviewNote) {
        ClaimRecord claim = claimService.findDetail(claimId);
        if (claim == null) throw new BusinessException(404, "Claim not found");
        if (!"PENDING".equals(claim.getStatus())) throw new BusinessException(409, "Claim has already been reviewed");
        LostItem item = itemService.getById(claim.getItemId());
        if (item == null) throw new BusinessException(404, "Item not found");
        if ("APPROVED".equals(status) && !"OPEN".equals(item.getStatus())) {
            throw new BusinessException(409, "Item is no longer open for approval");
        }
        LocalDateTime now = LocalDateTime.now();
        if (claimService.reviewPending(claimId, status, reviewerId, reviewNote, now) != 1) {
            throw new BusinessException(409, "Claim status changed, please refresh");
        }
        if ("APPROVED".equals(status)) {
            if (itemService.updateOpenStatus(claim.getItemId(), "DELETED") != 1) {
                throw new BusinessException(409, "Item status changed, please refresh");
            }
            claimService.rejectOthersAfterApproved(claim.getItemId(), claimId, reviewerId,
                    "Rejected because another claim was approved", now);
        }
        auditEventPublisher.publish("CLAIM_" + status, reviewerId, claimId);
    }
}
