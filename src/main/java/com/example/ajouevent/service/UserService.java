package com.example.ajouevent.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ajouevent.domain.User;
import com.example.ajouevent.dto.LoginResponse;
import com.example.ajouevent.dto.RegisterRequest;
import com.example.ajouevent.dto.UserDTO;
import com.example.ajouevent.dto.UserResponse;
import com.example.ajouevent.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
	private final UserRepository userRepository;

	@Transactional
	public String register(RegisterRequest registerRequest) {
		String email = registerRequest.getEmail();
		String password = registerRequest.getPassword();
		String name = registerRequest.getName();

		User user = User.builder()
			.password(registerRequest.getPassword())
			.email(registerRequest.getEmail())
			.build();

		userRepository.save(user);

		return "가입 완료"; // -> 수정 필요
	}

	@Transactional
	public String login(UserDTO.LoginRequest loginRequest) {
		String email = loginRequest.getEmail();
		String password = loginRequest.getPassword();

		// 이메일로 사용자 조회
		Optional<User> user = userRepository.findByEmail(email);


		if (user == null) {
			// 사용자가 없는 경우
			throw new RuntimeException("이메일이나 비밀번호가 올바르지 않습니다.");
		}



		// 로그인 성공
		return "로그인 완료";
	}
}
