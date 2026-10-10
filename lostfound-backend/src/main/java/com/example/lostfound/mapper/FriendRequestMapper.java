package com.example.lostfound.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.lostfound.domain.entity.FriendRequest;
import org.apache.ibatis.annotations.Mapper;

/** 好友申请表 Mapper：查询逻辑都在 FriendRequestService（继承 ServiceImpl）。 */
@Mapper
public interface FriendRequestMapper extends BaseMapper<FriendRequest> {
}
