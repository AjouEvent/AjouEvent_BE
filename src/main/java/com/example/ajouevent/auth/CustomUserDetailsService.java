package com.example.ajouevent.auth;

import com.example.ajouevent.domain.Member;
import com.example.ajouevent.dto.UserDTO;
import com.example.ajouevent.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String id) throws UsernameNotFoundException {
        Member member = userRepository.findById(Long.parseLong(id))
                .orElseThrow(() -> new UsernameNotFoundException("해당하는 유저가 없습니다."));

        UserDTO.UserInfoDto dto = UserDTO.UserInfoDto.builder()
                .memberId(member.getId())
                .email(member.getEmail())
                .role(member.getRole())
                .build();

        return new CustomUserDetails(dto);
    }
}
