package com.example.application.security;

import com.example.application.data.entity.User;
import com.example.application.data.service.UserService;
import com.example.application.views.LoginView;
import com.vaadin.flow.spring.security.VaadinWebSecurity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Hashtable;

@EnableWebSecurity
@Configuration
public class SecurityConfiguration extends VaadinWebSecurity {

    private final UserDetailsService userDetailsService;
    private final UserService userService;
    private final String ldapUrl;
    private final String p_ldapUserPrefix;
    private final String p_ldapUserPostfix;
   // @Value("${ad.check.program}")
    private final String adCheckProgram;

    public SecurityConfiguration(@Value("${ad.check.program}") String adCheckProgram, @Value("${ldap.url}") String p_ldapUrl, @Value("${ldap.user.prefix}") String p_ldapUserPrefix, @Value("${ldap.user.postfix}") String p_ldapUserPostfix, UserDetailsService userDetailsService, UserService userService) {
        this.userDetailsService = userDetailsService;
        this.userService = userService;

        this.ldapUrl = p_ldapUrl;
        this.p_ldapUserPrefix=p_ldapUserPrefix;
        this.p_ldapUserPostfix=p_ldapUserPostfix;
        this.adCheckProgram = adCheckProgram;
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

//    @Bean
//    public String adCheckProgram(@Value("${ad.check.program}") String adCheckProgram) {
//        return adCheckProgram;
//    }

    @Component
    public class CustomAuthenticationProvider implements AuthenticationProvider {

        @Override
        public Authentication authenticate(Authentication authentication) throws AuthenticationException {

            System.out.println("Angemeldet bei Spring Security wird: " + authentication.getName());

            String username = authentication.getName();
            String password = authentication.getCredentials().toString();

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            //System.out.println("Authentifiziere User: " + username + " / " + password);
            User user = userService.getUserByUsername(username);

            if (user == null)
            {
                System.out.println("User " + username + " not found in table application_user!!!");
                return null;
            }

            if( user.getIs_ad() == 1) {

                System.out.println(user.getName() + " ist Active Directory User...");

//                boolean isLoginSuccessful = false;
//                isLoginSuccessful = connectToLdap(username, password);

            //     Use the script to check the AD user
                boolean isLoginSuccessful = checkUserWithScript(username, password);

                if (isLoginSuccessful) {
                    System.out.println("AD says successfully login...");

                    return new UsernamePasswordAuthenticationToken(userDetails, password, userDetails.getAuthorities());
                }

            }
            else {

                if(!passwordEncoder().matches(password,userDetails.getPassword()))
            //    if(!password.equals(userDetails.getPassword()))
                {
                    System.out.println("Falsches Passwort!");


                    return null;
                }
                return new UsernamePasswordAuthenticationToken(userDetails, password, userDetails.getAuthorities());
            }

            return null;



        }

        @Override
        public boolean supports(Class<?> authentication) {
            return authentication.equals(UsernamePasswordAuthenticationToken.class);
        }

    }


    private boolean connectToLdap(String username, String password) {
        //String ldapUrl = "ldap://viaginterkom.de:389";
        //String ldapUrl = "ldap://fhhnet.stadt.hamburg.de:389";
        //String ldapUrl = "ldap://91.107.232.133:10389";


        //String ldapUser= username + "@viaginterkom.de";
        //String ldapUser= username + "@fhhnet.stadt.hamburg.de";
//        String ldapUser= username + "@wimpi.net";

        //    String ldapUser = "uid=" + username + ",ou=users,dc=wimpi,dc=net"; // Adjust the DN pattern

        String ldapUser = p_ldapUserPrefix + username + p_ldapUserPostfix;

        String ldapPassword = password;

        System.out.println("Anmelden User: " + ldapUser);
        //  System.out.println("Password: " + ldapPassword);
        System.out.println("URL: " + ldapUrl);


        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, ldapUrl);
        //env.put(Context.SECURITY_PRINCIPAL, ldapUser);
        env.put(Context.SECURITY_PRINCIPAL, ldapUser);
        env.put("com.sun.jndi.ldap.connect.timeout", "500000");
        env.put("com.sun.jndi.ldap.read.timeout", "500000");

        env.put(Context.SECURITY_CREDENTIALS, ldapPassword);

        long start = 0;
        try {
            // Attempt to create an initial context with the provided credentials

            start = System.currentTimeMillis();
            System.out.println("Aufruf InitialDirContext Start");
            DirContext context = new InitialDirContext(env);

            System.out.println("Time for Login: " + ((System.currentTimeMillis() - start)) + " ms.");
            // Close the context after use
            context.close();
            System.out.println("Aufruf InitialDirContext Ende");

            System.out.println("Check User against AD is successfully...");


            return true;
        } catch (NamingException e) {
            // Handle exceptions (e.g., authentication failure)
            System.out.println("Check User against AD failed!!!");

            System.out.println("Failed because " + e.getRootCause()
                    .getMessage() + ". Timeout: " + ((System.currentTimeMillis() - start)) + " ms.");
            //System.out.println("Still act like it was successful");
            //return true;

            e.printStackTrace();
            return false;
        }

    }

    //@Override
    private boolean checkUserWithScript(String username, String password) {
        try {
            ProcessBuilder pb = new ProcessBuilder(adCheckProgram, username, password);
            Process process = pb.start();
            int exitCode = process.waitFor();  // Wait for the script to finish

            // Read the script's output (if needed)
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);  // Print the script's output
            }

            if (exitCode == 0) {
                System.out.println("Login successful via script.");
                return true;  // Script returned 0, login is successful
            } else {
                System.out.println("Login failed via script.");
                return false;  // Script returned non-zero, login failed
            }
        } catch (Exception e) {
            System.out.println("Error running AD check script: " + e.getMessage());
            return false;
        }
    }


    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http.authorizeHttpRequests().requestMatchers(new AntPathRequestMatcher("/images/*.png")).permitAll();
        http.authorizeHttpRequests().requestMatchers(new AntPathRequestMatcher("/line-awesome/**/*.svg")).permitAll();
        super.configure(http);
        setLoginView(http, LoginView.class);
        http.formLogin().defaultSuccessUrl("/",true);

    }

}
