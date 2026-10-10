package com.example.lostfound.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.lostfound.domain.entity.UserAccount;
import org.apache.ibatis.annotations.Mapper;

/** 用户表 Mapper：查询逻辑都在 UserService（继承 ServiceImpl）。 */
@Mapper
public interface UserMapper extends BaseMapper<UserAccount> {
}
