package com.example.lostfound.service;

/** 认领审核：加分布式锁 + 事务，保证同一物品的并发审核只成功一次。 */
public interface ClaimReviewService {

    void review(Long claimId, String status, Long reviewerId, String reviewNote);

    void reviewInTransaction(Long claimId, String status, Long reviewerId, String reviewNote);
}
