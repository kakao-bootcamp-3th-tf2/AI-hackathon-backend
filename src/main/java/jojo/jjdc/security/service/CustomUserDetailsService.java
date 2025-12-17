package jojo.jjdc.security.service;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    // private MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // if (!memberRepository.userExists(username)) {
        //     UserDetails newUser = User.withUsername(username)
        //             .password("")
        //             .authorities(new ArrayList<>())
        //             .build();
		// 	memberRepository.save(newUser);
        // }
        // return memberRepository.loadUserByUsername(username);
		return null;
    }
}
