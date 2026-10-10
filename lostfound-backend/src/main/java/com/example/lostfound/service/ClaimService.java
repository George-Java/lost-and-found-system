package com.example.lostfound.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.lostfound.domain.entity.ClaimRecord;

import java.time.LocalDateTime;
import java.util.List;

public interface ClaimService extends IService<ClaimRecord> {

    ClaimRecord findDetail(Long id);

    List<ClaimRecord> findByClaimantId(Long claimantId);

    List<ClaimRecord> findByItemId(Long itemId);

    ClaimRecord findPendingByItemAndClaimant(Long itemId, Long claimantId);

    List<ClaimRecord> findList(String status);

    long countAll();

    long countByStatus(String status);

    /** 只有仍处于 PENDING 的申请才能被审核，返回影响行数用于并发校验。 */
    int reviewPending(Long id, String status, Long reviewerId, String reviewNote, LocalDateTime reviewedAt);

    /** 某个申请通过后，同一物品的其他待审申请自动置为拒绝。 */
    int rejectOthersAfterApproved(Long itemId, Long approvedId, Long reviewerId,
                                  String reviewNote, LocalDateTime reviewedAt);
}
