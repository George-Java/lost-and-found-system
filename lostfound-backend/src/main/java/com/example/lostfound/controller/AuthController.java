package com.example.lostfound.controller;

import com.example.lostfound.auth.AuthContext;
import com.example.lostfound.auth.SessionStore;
import com.example.lostfound.auth.SessionUser;
import com.example.lostfound.common.ApiResponse;
import com.example.lostfound.common.BusinessException;
import com.example.lostfound.domain.dto.LoginRequest;
import com.example.lostfound.domain.dto.RegisterRequest;
import com.example.lostfound.domain.entity.UserAccount;
import com.example.lostfound.service.UserService;
import com.example.lostfound.util.PasswordUtil;
import com.example.lostfound.domain.vo.LoginVO;
import com.example.lostfound.domain.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final SessionStore sessionStore;

    @PostMapping("/register")
    public ApiResponse<UserVO> register(@RequestBody @Valid RegisterRequest request) {
        UserAccount exists = userService.findByUsername(request.username());
        if (exists != null) {
            throw new BusinessException(409, "Username already exists");
        }

        UserAccount user = new UserAccount();
        user.setUsername(request.username());
        user.setPasswordHash(PasswordUtil.encode(request.password()));
        user.setRealName(request.realName());
        user.setPhone(request.phone());
        user.setRole("USER");
        userService.save(user);

        return ApiResponse.success(new UserVO(user.getId(), user.getUsername(), user.getRealName(), user.getPhone(), user.getRole()));
    }

    @PostMapping("/login")
    public ApiResponse<LoginVO> login(@RequestBody @Valid LoginRequest request) {
        UserAccount user = userService.findByUsername(request.username());
        if (user == null || !PasswordUtil.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(401, "Username or password is invalid");
        }
        if (PasswordUtil.needsRehash(user.getPasswordHash())) {
            user.setPasswordHash(PasswordUtil.encode(request.password()));
            userService.updateById(user);
        }

        String token = sessionStore.create(new SessionUser(user.getId(), user.getUsername(), user.getRole()));
        LoginVO vo = new LoginVO(token, user.getId(), user.getUsername(), user.getRole(), user.getRealName());
        return ApiResponse.success(vo);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        sessionStore.remove(extractToken(request.getHeader("Authorization")));
        return ApiResponse.success();
    }

    @GetMapping("/me")
    public ApiResponse<UserVO> me() {
        UserAccount user = userService.findById(AuthContext.userId());
        if (user == null) {
            throw new BusinessException(404, "User not found");
        }
        return ApiResponse.success(new UserVO(user.getId(), user.getUsername(), user.getRealName(), user.getPhone(), user.getRole()));
    }

    private String extractToken(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            return null;
        }
        if (authHeader.startsWith("Bearer ") && authHeader.length() > 7) {
            return authHeader.substring(7);
        }
        return null;
    }
}
