package com.example.lostfound.domain.entity;

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

    private Long itemId;

    private Long claimantId;

    @TableField(exist = false)
    private String claimantName;

    @TableField(exist = false)
    private String claimantPhone;

    @TableField(exist = false)
    private String itemTitle;

    private String claimReason;

    private String proofDescription;

    @TableField("proof_images")
    private String proofMaterials;
    private String status;

    private Long reviewerId;

    private String reviewNote;

    private LocalDateTime createdAt;

    private LocalDateTime reviewedAt;
}
