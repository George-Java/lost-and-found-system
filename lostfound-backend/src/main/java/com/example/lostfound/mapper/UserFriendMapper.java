package com.example.lostfound.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.lostfound.domain.entity.UserFriend;
import org.apache.ibatis.annotations.Mapper;

/** 好友关系表 Mapper：查询逻辑都在 UserFriendService（继承 ServiceImpl）。 */
@Mapper
public interface UserFriendMapper extends BaseMapper<UserFriend> {
}
