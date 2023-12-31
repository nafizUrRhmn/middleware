package com.agent.middleware.controller;

import com.agent.middleware.config.JwtTokenProvider;
import com.agent.middleware.dto.JwtResponseDto;
import com.agent.middleware.dto.RefreshTokenRequestDto;
import com.agent.middleware.dto.UserLoginDto;
import com.agent.middleware.dto.UserRegisterDto;
import com.agent.middleware.entity.RefreshToken;
import com.agent.middleware.service.RefreshTokenService;
import com.agent.middleware.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class UserRestController {

    private final UserService userService;

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;


    public UserRestController(UserService userService, AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider, RefreshTokenService refreshTokenService) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
    }


    @PostMapping(value = "/public/register")
    public HttpStatus register(@RequestBody UserRegisterDto userRequestDto) {
        userService.register(userRequestDto);
        return HttpStatus.OK;
    }

    @PostMapping(value = "/public/login")
    public JwtResponseDto login(@RequestBody UserLoginDto userLoginDto) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(userLoginDto.getUsername(),
                userLoginDto.getPassword()));
        if (authentication.isAuthenticated()) {
            System.out.println("Authenticated");
            SecurityContextHolder.getContext().setAuthentication(authentication);
            final String jwtToken = jwtTokenProvider.generateToken(authentication);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(userLoginDto.getUsername());
            JwtResponseDto jwtResponseDto = new JwtResponseDto();
            jwtResponseDto.setJwtToken(jwtToken);
            jwtResponseDto.setRefreshToken(refreshToken.getToken());
            return jwtResponseDto;
        } else {
            throw new UsernameNotFoundException("invalid user request..!!");
        }
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/user/refresh-token")
    public JwtResponseDto refreshToken(@RequestBody RefreshTokenRequestDto refreshTokenRequestDTO) {
        RefreshToken refreshToken = refreshTokenService.findByToken(refreshTokenRequestDTO.getRefreshToken());
        refreshTokenService.verifyExpiration(refreshToken);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String jwtToken = jwtTokenProvider.generateToken(authentication);
        JwtResponseDto jwtResponseDto = new JwtResponseDto();
        jwtResponseDto.setJwtToken(jwtToken);
        jwtResponseDto.setRefreshToken(refreshToken.getToken());
        return jwtResponseDto;
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/user/revoke-token")
    public HttpStatus revokeToken(@RequestBody RefreshTokenRequestDto refreshTokenRequestDTO) {
        RefreshToken refreshToken = refreshTokenService.findByToken(refreshTokenRequestDTO.getRefreshToken());
        refreshTokenService.deleteRefreshToken(refreshToken);
        return HttpStatus.OK;
    }
}
