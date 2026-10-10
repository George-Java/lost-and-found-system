package com.example.lostfound.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.lostfound.domain.entity.UserAccount;
import com.example.lostfound.mapper.UserMapper;
import com.example.lostfound.service.UserService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, UserAccount> implements UserService {

    @Override
    public UserAccount findByUsername(String username) {
        return getOne(Wrappers.<UserAccount>lambdaQuery()
                .eq(UserAccount::getUsername, username)
                .last("limit 1"));
    }

    @Override
    public UserAccount findById(Long id) {
        return id == null ? null : getById(id);
    }

    @Override
    public List<UserAccount> findAll() {
        return list(Wrappers.<UserAccount>lambdaQuery()
                .orderByDesc(UserAccount::getCreatedAt)
                .orderByDesc(UserAccount::getId));
    }

    @Override
    public List<UserAccount> search(String keyword) {
        return list(Wrappers.<UserAccount>lambdaQuery()
                .like(UserAccount::getUsername, keyword)
                .or()
                .like(UserAccount::getRealName, keyword)
                .orderByAsc(UserAccount::getUsername)
                .last("limit 20"));
    }

    @Override
    public void updateRole(Long id, String role) {
        UserAccount user = new UserAccount();
        user.setId(id);
        user.setRole(role);
        updateById(user);
    }

    @Override
    public long countAll() {
        return count();
    }
}
