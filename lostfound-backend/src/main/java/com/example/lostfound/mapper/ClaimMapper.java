package com.example.lostfound.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.lostfound.domain.entity.ClaimRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 认领表 Mapper：带关联查询与动态条件的语句全部写在 resources/mapper/ClaimMapper.xml。
 */
@Mapper
public interface ClaimMapper extends BaseMapper<ClaimRecord> {

    ClaimRecord selectDetailById(@Param("id") Long id);

    List<ClaimRecord> selectByClaimantId(@Param("claimantId") Long claimantId);

    List<ClaimRecord> selectByItemId(@Param("itemId") Long itemId);

    ClaimRecord selectPendingByItemAndClaimant(@Param("itemId") Long itemId, @Param("claimantId") Long claimantId);

    List<ClaimRecord> selectListByStatus(@Param("status") String status);

    /** 只有仍处于 PENDING 的申请才能被审核，返回影响行数用于并发校验。 */
    int reviewPending(@Param("id") Long id,
                      @Param("status") String status,
                      @Param("reviewerId") Long reviewerId,
                      @Param("reviewNote") String reviewNote,
                      @Param("reviewedAt") LocalDateTime reviewedAt);

    /** 某个申请通过后，同一物品的其他待审申请自动置为拒绝。 */
    int rejectOthersAfterApproved(@Param("itemId") Long itemId,
                                  @Param("approvedId") Long approvedId,
                                  @Param("reviewerId") Long reviewerId,
                                  @Param("reviewNote") String reviewNote,
                                  @Param("reviewedAt") LocalDateTime reviewedAt);
}
