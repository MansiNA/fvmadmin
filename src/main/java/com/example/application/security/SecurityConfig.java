package com.example.application.security;

import com.example.application.views.LoginView;
import com.vaadin.flow.spring.security.VaadinWebSecurity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;

import java.util.Collections;

import static com.example.application.security.ApplicationUserRole.*;

@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)  //notwendig, damit an den Methoden @PreAuthorize verwendet werden kann!

@Configuration
public class SecurityConfig extends VaadinWebSecurity {



    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests().antMatchers("/images/**").permitAll();

        super.configure(http);

        setLoginView(http, LoginView.class);


    }

/*
    @Bean
    public InMemoryUserDetailsManager userDetailsService() {
        return new SimpleInMemoryUserDetailsManager();
    }
*/

    @Bean
    public UserDetailsManager userDetailsService() {
        UserDetails user =
                User.withUsername("user")
                        .password("{noop}user")
                      //  .roles("USER")
                      //  .roles(USER.name())
                        .authorities(USER.getGrantedAuthorities())
                        .build();
        UserDetails user2 =
                User.withUsername("user2")
                        .password("{noop}fb_user2!")
                       // .roles("USER")
                       // .roles(USER.name())
                        .authorities(USER.getGrantedAuthorities())
                        .build();
        UserDetails admin =
                User.withUsername("admin")
                        .password("{noop}admin!2023")
                      //  .roles("ADMIN")
                       // .roles(ADMIN.name())
                        .authorities(ADMIN.getGrantedAuthorities())
                        .build();
        UserDetails pf_admin =
                User.withUsername("pf_admin")
                        .password("{noop}pf_admin!")
                        //.roles("PF_ADMIN")
                        //.roles(PF_ADMIN.name())
                        .authorities(PF_ADMIN.getGrantedAuthorities())
                        .build();
        return new InMemoryUserDetailsManager(user,user2, admin,pf_admin);




    }


 /*   @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception
    {
        http
                .authorizeHttpRequests(
                        authorizeHttpRequest -> authorizeHttpRequest.regexMatchers("/tip").permitAll()
                                .anyRequest().authenticated()
                ).formLogin(Customizer.withDefaults())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }
*/
}