package com.example.lostfound.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.lostfound.domain.entity.UserAccount;

import java.util.List;

public interface UserService extends IService<UserAccount> {

    UserAccount findByUsername(String username);

    UserAccount findById(Long id);

    List<UserAccount> findAll();

    List<UserAccount> search(String keyword);

    void updateRole(Long id, String role);

    long countAll();
}
