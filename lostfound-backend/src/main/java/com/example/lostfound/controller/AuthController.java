package com.example.lostfound.controller;

import com.example.lostfound.auth.AuthContext;
import com.example.lostfound.auth.LoginRequired;
import com.example.lostfound.auth.SessionStore;
import com.example.lostfound.auth.SessionUser;
import com.example.lostfound.common.ApiResponse;
import com.example.lostfound.common.BusinessException;
import com.example.lostfound.dto.LoginRequest;
import com.example.lostfound.dto.RegisterRequest;
import com.example.lostfound.entity.UserAccount;
import com.example.lostfound.mapper.UserMapper;
import com.example.lostfound.util.PasswordUtil;
import com.example.lostfound.vo.LoginVO;
import com.example.lostfound.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserMapper userMapper;
    private final SessionStore sessionStore;

    public AuthController(UserMapper userMapper, SessionStore sessionStore) {
        this.userMapper = userMapper;
        this.sessionStore = sessionStore;
    }

    @PostMapping("/register")
    public ApiResponse<UserVO> register(@RequestBody @Valid RegisterRequest request) {
        UserAccount exists = userMapper.findByUsername(request.getUsername());
        if (exists != null) {
            throw new BusinessException(409, "Username already exists");
        }

        UserAccount user = new UserAccount();
        user.setUsername(request.getUsername());
        user.setPasswordHash(PasswordUtil.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setRole("USER");
        userMapper.insert(user);

        return ApiResponse.success(new UserVO(user.getId(), user.getUsername(), user.getRealName(), user.getPhone(), user.getRole()));
    }

    @PostMapping("/login")
    public ApiResponse<LoginVO> login(@RequestBody @Valid LoginRequest request) {
        UserAccount user = userMapper.findByUsername(request.getUsername());
        if (user == null || !PasswordUtil.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(401, "Username or password is invalid");
        }

        String token = sessionStore.create(new SessionUser(user.getId(), user.getUsername(), user.getRole()));
        LoginVO vo = new LoginVO(token, user.getId(), user.getUsername(), user.getRole(), user.getRealName());
        return ApiResponse.success(vo);
    }

    @LoginRequired
    @GetMapping("/me")
    public ApiResponse<UserVO> me() {
        UserAccount user = userMapper.findById(AuthContext.userId());
        if (user == null) {
            throw new BusinessException(404, "User not found");
        }
        return ApiResponse.success(new UserVO(user.getId(), user.getUsername(), user.getRealName(), user.getPhone(), user.getRole()));
    }
}
