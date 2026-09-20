package com.example.lostfound.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("claim_record")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("item_id")
    private Long itemId;

    @TableField("claimant_id")
    private Long claimantId;

    @TableField(exist = false)
    private String claimantName;

    @TableField(exist = false)
    private String claimantPhone;

    @TableField(exist = false)
    private String itemTitle;

    @TableField("claim_reason")
    private String claimReason;

    @TableField("proof_description")
    private String proofDescription;

    @TableField("proof_images")
    private String proofMaterials;
    private String status;

    @TableField("reviewer_id")
    private Long reviewerId;

    @TableField("review_note")
    private String reviewNote;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("reviewed_at")
    private LocalDateTime reviewedAt;
}
