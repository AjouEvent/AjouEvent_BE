package com.example.ajouevent.auth;

import com.example.ajouevent.domain.Member;
import com.example.ajouevent.dto.MemberDto;
import com.example.ajouevent.repository.MemberRepository;
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
    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String id) throws UsernameNotFoundException {
        Member member = memberRepository.findById(Long.parseLong(id))
                .orElseThrow(() -> new UsernameNotFoundException("해당하는 유저가 없습니다."));

        MemberDto.MemberInfoDto dto = MemberDto.MemberInfoDto.builder()
                .memberId(member.getId())
                .email(member.getEmail())
                .role(member.getRole())
                .build();

        return new CustomUserDetails(dto);
    }
}
