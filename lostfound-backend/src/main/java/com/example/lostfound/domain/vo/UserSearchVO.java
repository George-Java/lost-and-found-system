package com.example.lostfound.domain.vo;

/** 用户搜索结果：在 UserVO 基础上多一个好友关系状态，字段与 JSON 结构保持不变。 */
public record UserSearchVO(Long id, String username, String realName, String phone,
                           String role, String friendshipStatus) {
}
