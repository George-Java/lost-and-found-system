package com.example.lostfound.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.lostfound.entity.UserAccount;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<UserAccount> {

    default UserAccount findByUsername(String username) {
        return selectOne(Wrappers.<UserAccount>lambdaQuery()
                .eq(UserAccount::getUsername, username)
                .last("limit 1"));
    }

    default UserAccount findById(Long id) {
        return selectById(id);
    }

    default List<UserAccount> findAll() {
        return selectList(Wrappers.<UserAccount>lambdaQuery()
                .orderByDesc(UserAccount::getCreatedAt)
                .orderByDesc(UserAccount::getId));
    }

    default List<UserAccount> search(String keyword) {
        return selectList(Wrappers.<UserAccount>lambdaQuery()
                .like(UserAccount::getUsername, keyword)
                .or()
                .like(UserAccount::getRealName, keyword)
                .orderByAsc(UserAccount::getUsername)
                .last("limit 20"));
    }

    default int updateRole(Long id, String role) {
        UserAccount user = new UserAccount();
        user.setId(id);
        user.setRole(role);
        return updateById(user);
    }

    default long countAll() {
        return selectCount(Wrappers.<UserAccount>lambdaQuery());
    }
}
