package com.example.lostfound.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.lostfound.domain.entity.ClaimRecord;
import com.example.lostfound.mapper.ClaimMapper;
import com.example.lostfound.service.ClaimService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ClaimServiceImpl extends ServiceImpl<ClaimMapper, ClaimRecord> implements ClaimService {

    @Override
    public ClaimRecord findDetail(Long id) {
        return baseMapper.selectDetailById(id);
    }

    @Override
    public List<ClaimRecord> findByClaimantId(Long claimantId) {
        return baseMapper.selectByClaimantId(claimantId);
    }

    @Override
    public List<ClaimRecord> findByItemId(Long itemId) {
        return baseMapper.selectByItemId(itemId);
    }

    @Override
    public ClaimRecord findPendingByItemAndClaimant(Long itemId, Long claimantId) {
        return baseMapper.selectPendingByItemAndClaimant(itemId, claimantId);
    }

    @Override
    public List<ClaimRecord> findList(String status) {
        return baseMapper.selectListByStatus(status);
    }

    @Override
    public long countAll() {
        return count();
    }

    @Override
    public long countByStatus(String status) {
        return count(Wrappers.<ClaimRecord>lambdaQuery()
                .eq(ClaimRecord::getStatus, status));
    }

    @Override
    public int reviewPending(Long id, String status, Long reviewerId, String reviewNote, LocalDateTime reviewedAt) {
        return baseMapper.reviewPending(id, status, reviewerId, reviewNote, reviewedAt);
    }

    @Override
    public int rejectOthersAfterApproved(Long itemId, Long approvedId, Long reviewerId,
                                         String reviewNote, LocalDateTime reviewedAt) {
        return baseMapper.rejectOthersAfterApproved(itemId, approvedId, reviewerId, reviewNote, reviewedAt);
    }
}
