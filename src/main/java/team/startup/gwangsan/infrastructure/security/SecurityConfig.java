package team.startup.gwangsan.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusScrapeEndpoint;
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
import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.adapter.security.auth.MemberDetailsService;
import team.startup.gwangsan.infrastructure.web.ExceptionFilter;
import team.startup.gwangsan.adapter.security.auth.JwtAuthenticationFilter;
import team.startup.gwangsan.adapter.security.auth.JwtFilter;
import team.startup.gwangsan.infrastructure.web.RequestLogFilter;
import team.startup.gwangsan.infrastructure.security.JwtAccessDeniedHandler;
import team.startup.gwangsan.infrastructure.security.JwtAuthenticationEntryPoint;
import team.startup.gwangsan.adapter.security.auth.JwtProvider;
import team.startup.gwangsan.adapter.security.auth.TokenParser;

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
                                .requestMatchers(HttpMethod.PATCH, "/api/auth/password").permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/auth/nickname").permitAll()

                                // sms
                                .requestMatchers(HttpMethod.POST, "/api/sms").permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/sms/verify").permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/sms/password").permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/sms/password/verify").permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/sms/nickname").permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/sms/nickname/verify").permitAll()

                                // post
                                .requestMatchers(HttpMethod.POST, "/api/post").authenticated()
                                .requestMatchers(HttpMethod.GET, "/api/post").authenticated()
                                .requestMatchers(HttpMethod.PATCH, "/api/post/{post_id}").authenticated()
                                .requestMatchers(HttpMethod.GET, "/api/post/{post_id}").authenticated()
                                .requestMatchers(HttpMethod.DELETE, "/api/post/{post_id}").authenticated()
                                .requestMatchers(HttpMethod.GET, "/api/post/current").authenticated()
                                .requestMatchers(HttpMethod.GET, "/api/post/member/{member_id}").authenticated()
                                .requestMatchers(HttpMethod.POST, "/api/post/trade").authenticated()
                                .requestMatchers(HttpMethod.PATCH, "/api/post/reservation/{post_id}").authenticated()
                                .requestMatchers(HttpMethod.DELETE, "/api/post/reservation/{post_id}").authenticated()

                                // notice
                                .requestMatchers(HttpMethod.GET, "/api/notice").authenticated()
                                .requestMatchers(HttpMethod.GET, "/api/notice/{id}").authenticated()
                                .requestMatchers(HttpMethod.POST, "/api/notice")
                                .hasAnyAuthority(MemberRole.ROLE_PLACE_ADMIN.name(), MemberRole.ROLE_HEAD_ADMIN.name())
                                .requestMatchers(HttpMethod.PATCH, "/api/notice/{id}").authenticated()
                                .requestMatchers(HttpMethod.DELETE, "/api/notice/{id}").authenticated()

                                // related-keyword
                                .requestMatchers(HttpMethod.GET, "/api/related-keyword").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/related-keyword/current").authenticated()
                                .requestMatchers(HttpMethod.DELETE, "/api/related-keyword/{memberRelatedKeywordId}").authenticated()

                                // admin
                                .requestMatchers(HttpMethod.POST, "/api/admin/signin").permitAll()
                                .requestMatchers("/api/admin/**")
                                .hasAnyAuthority(MemberRole.ROLE_PLACE_ADMIN.name(), MemberRole.ROLE_HEAD_ADMIN.name())

                                // review
                                .requestMatchers(HttpMethod.POST, "/api/review").authenticated()
                                .requestMatchers(HttpMethod.GET, "/api/review").authenticated()
                                .requestMatchers(HttpMethod.GET, "/api/review/current").authenticated()
                                .requestMatchers(HttpMethod.GET, "/api/review/{memberId}").authenticated()
                                .requestMatchers(HttpMethod.POST, "/api/review/detail/{reviewId}").authenticated()

                                // member
                                .requestMatchers(HttpMethod.GET, "/api/member").authenticated()
                                .requestMatchers(HttpMethod.PATCH, "/api/member").authenticated()
                                .requestMatchers(HttpMethod.GET, "/api/member/{memberId}").authenticated()
                                .requestMatchers(HttpMethod.GET, "/api/member/all")
                                .hasAnyAuthority(MemberRole.ROLE_PLACE_ADMIN.name(), MemberRole.ROLE_HEAD_ADMIN.name())

                                // report
                                .requestMatchers("/api/feedback", "/api/feedback/**").authenticated()
                                .requestMatchers(HttpMethod.POST, "/api/report").authenticated()

                                // health
                                .requestMatchers(HttpMethod.GET, "/api/health").permitAll()
                                // actuator: management 포트(9091, 비공개)에서 온 요청만 매칭된다. API 포트에는 actuator 가 없다.
                                .requestMatchers(EndpointRequest.to(HealthEndpoint.class, PrometheusScrapeEndpoint.class)).permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/app/version").permitAll()

                                // suspend
                                .requestMatchers(HttpMethod.PATCH, "/api/suspend")
                                .hasAnyAuthority(MemberRole.ROLE_PLACE_ADMIN.name(), MemberRole.ROLE_HEAD_ADMIN.name())

                                // trade
                                .requestMatchers(HttpMethod.GET, "/api/trade/graph/head").hasAnyAuthority(MemberRole.ROLE_HEAD_ADMIN.name())
                                .requestMatchers(HttpMethod.GET, "/api/trade/graph/place").hasAnyAuthority(MemberRole.ROLE_HEAD_ADMIN.name(), MemberRole.ROLE_PLACE_ADMIN.name())
                                .requestMatchers(HttpMethod.GET, "/api/trade/statistics/head").hasAnyAuthority(MemberRole.ROLE_HEAD_ADMIN.name())
                                .requestMatchers(HttpMethod.GET, "/api/trade/statistics/place").hasAnyAuthority(MemberRole.ROLE_HEAD_ADMIN.name(), MemberRole.ROLE_PLACE_ADMIN.name())

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
