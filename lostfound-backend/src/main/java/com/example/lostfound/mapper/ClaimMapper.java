package com.example.lostfound.mapper;

import com.example.lostfound.entity.ClaimRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ClaimMapper {

    String BASE_SELECT = """
            select
                c.id,
                c.item_id,
                c.claimant_id,
                c.claim_reason,
                c.proof_description,
                c.proof_images as proof_materials,
                c.status,
                c.reviewer_id,
                c.review_note,
                c.created_at,
                c.reviewed_at,
                u.real_name as claimant_name,
                u.phone as claimant_phone,
                i.title as item_title
            from claim_record c
            left join user_account u on c.claimant_id = u.id
            left join lost_item i on c.item_id = i.id
            """;

    @Insert("""
            insert into claim_record(item_id, claimant_id, claim_reason, proof_description, proof_images, status)
            values(#{itemId}, #{claimantId}, #{claimReason}, #{proofDescription}, #{proofMaterials}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ClaimRecord claimRecord);

    @Select(BASE_SELECT + " where c.id = #{id} limit 1")
    ClaimRecord findById(@Param("id") Long id);

    @Select(BASE_SELECT + " where c.claimant_id = #{claimantId} order by c.created_at desc")
    List<ClaimRecord> findByClaimantId(@Param("claimantId") Long claimantId);

    @Select(BASE_SELECT + " where c.item_id = #{itemId} order by c.created_at desc")
    List<ClaimRecord> findByItemId(@Param("itemId") Long itemId);

    @Select(BASE_SELECT + " where c.item_id = #{itemId} and c.claimant_id = #{claimantId} and c.status = 'PENDING' limit 1")
    ClaimRecord findPendingByItemAndClaimant(@Param("itemId") Long itemId, @Param("claimantId") Long claimantId);

    @Select("""
            <script>
            """ + BASE_SELECT + """
            where 1=1
            <if test="status != null and status != ''">
                and c.status = #{status}
            </if>
            order by c.created_at desc
            </script>
            """)
    List<ClaimRecord> findList(@Param("status") String status);

    @Select("select count(1) from claim_record")
    long countAll();

    @Select("select count(1) from claim_record where status = #{status}")
    long countByStatus(@Param("status") String status);

    @Update("""
            update claim_record
            set status = #{status}, reviewer_id = #{reviewerId}, review_note = #{reviewNote}, reviewed_at = #{reviewedAt}
            where id = #{id}
            """)
    int review(@Param("id") Long id,
               @Param("status") String status,
               @Param("reviewerId") Long reviewerId,
               @Param("reviewNote") String reviewNote,
               @Param("reviewedAt") LocalDateTime reviewedAt);

    @Update("""
            update claim_record
            set status = 'REJECTED', reviewer_id = #{reviewerId}, review_note = #{reviewNote}, reviewed_at = #{reviewedAt}
            where item_id = #{itemId} and status = 'PENDING' and id != #{approvedId}
            """)
    int rejectOthersAfterApproved(@Param("itemId") Long itemId,
                                  @Param("approvedId") Long approvedId,
                                  @Param("reviewerId") Long reviewerId,
                                  @Param("reviewNote") String reviewNote,
                                  @Param("reviewedAt") LocalDateTime reviewedAt);
}
