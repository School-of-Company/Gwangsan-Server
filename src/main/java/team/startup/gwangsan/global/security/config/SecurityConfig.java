package team.startup.gwangsan.global.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsUtils;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.global.auth.MemberDetailsService;
import team.startup.gwangsan.global.filter.ExceptionFilter;
import team.startup.gwangsan.global.filter.JwtAuthenticationFilter;
import team.startup.gwangsan.global.filter.JwtFilter;
import team.startup.gwangsan.global.filter.RequestLogFilter;
import team.startup.gwangsan.global.security.handler.JwtAccessDeniedHandler;
import team.startup.gwangsan.global.security.handler.JwtAuthenticationEntryPoint;
import team.startup.gwangsan.global.security.jwt.JwtProvider;
import team.startup.gwangsan.global.security.jwt.TokenParser;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final TokenParser tokenParser;
    private final ObjectMapper objectMapper;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final MemberDetailsService memberDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                .exceptionHandling(config ->
                        config.authenticationEntryPoint(jwtAuthenticationEntryPoint)
                                .accessDeniedHandler(jwtAccessDeniedHandler)
                )

                .sessionManagement(config ->
                        config.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth ->
                        auth
                                .requestMatchers(CorsUtils::isPreFlightRequest).permitAll()

                                // auth
                                .requestMatchers(HttpMethod.POST, "/api/auth/signup").permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/auth/signin").permitAll()
                                .requestMatchers(HttpMethod.PATCH, "/api/auth/reissue").permitAll()
                                .requestMatchers(HttpMethod.DELETE, "/api/auth/signout").authenticated()

                                // sms
                                .requestMatchers(HttpMethod.POST, "/api/sms").permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/sms/verify").permitAll()

                                // post
                                .requestMatchers(HttpMethod.POST, "api/post").authenticated()
                                .requestMatchers(HttpMethod.GET, "/api/post").authenticated()
                                .requestMatchers(HttpMethod.PATCH, "/api/post/{post_id}").authenticated()
                                .requestMatchers(HttpMethod.GET, "/api/post/{post_id}").authenticated()
                                .requestMatchers(HttpMethod.DELETE, "/api/post/{post_id}").authenticated()
                                .requestMatchers(HttpMethod.GET, "/api/post/current").authenticated()
                                .requestMatchers(HttpMethod.GET, "/api/post/member/{member_id}").authenticated()

                                // notice
                                .requestMatchers(HttpMethod.GET, "/api/notice").authenticated()
                                .requestMatchers(HttpMethod.GET, "/api/notice/{id}").authenticated()
                                .requestMatchers(HttpMethod.POST, "/api/notice")
                                .hasAnyAuthority(MemberRole.ROLE_PLACE_ADMIN.name(), MemberRole.ROLE_HEAD_ADMIN.name())
                                .requestMatchers(HttpMethod.PATCH, "/api/notice/{id}").authenticated()
                                .requestMatchers(HttpMethod.DELETE, "/api/notice/{id}").authenticated()

                                // related-keyword
                                .requestMatchers(HttpMethod.GET, "/api/related-keyword").authenticated()
                                .requestMatchers(HttpMethod.GET, "/api/related-keyword/current").authenticated()
                                .requestMatchers(HttpMethod.DELETE, "/api/related-keyword/{memberRelatedKeywordId}").authenticated()

                                // admin
                                .requestMatchers(HttpMethod.POST, "/api/admin/signin").permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/admin/**")
                                .hasAnyAuthority(MemberRole.ROLE_PLACE_ADMIN.name(), MemberRole.ROLE_HEAD_ADMIN.name())

                                // report
                                .requestMatchers(HttpMethod.POST, "/api/report").authenticated()

                                // health
                                .requestMatchers(HttpMethod.GET, "/api/health").permitAll()

                                .anyRequest().hasAnyAuthority(
                                        MemberRole.ROLE_USER.name(),
                                        MemberRole.ROLE_PLACE_ADMIN.name(),
                                        MemberRole.ROLE_HEAD_ADMIN.name()
                                )
                )

                .addFilterBefore(new RequestLogFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new ExceptionFilter(objectMapper), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new JwtFilter(jwtProvider, tokenParser), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider, memberDetailsService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
