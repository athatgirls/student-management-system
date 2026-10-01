package org.example.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .cors().and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .antMatchers("/msi/student/login", "/msi/admin/login").permitAll()
                .antMatchers("/msi/student/change-initial-password").permitAll()
                .antMatchers("/static/**", "/msi/session/logout").permitAll()
                .antMatchers("/doc.html", "/swagger-ui/**", "/v3/api-docs/**", "/webjars/**").permitAll()
                .antMatchers("/actuator/health", "/actuator/info").permitAll()
                .antMatchers("/msi/admin/**").hasRole("ADMIN")
                .antMatchers("/msi/**/admin/**").hasRole("ADMIN")
                .antMatchers("/msi/student/list", "/msi/student/create", "/msi/student/import",
                        "/msi/student/batch-delete", "/msi/student/batch-update-status", "/msi/student/batch-update-major",
                        "/msi/student/reset-password", "/msi/student/statistics",
                        "/msi/student/update", "/msi/student/delete/**").hasRole("ADMIN")
                .antMatchers("/msi/grade/**", "/msi/activities/**").hasRole("ADMIN")
                .antMatchers("/msi/major/create", "/msi/major/delete-by-name", "/msi/major/delete/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.POST,
                        "/msi/academic-events/create", "/msi/academic-events/audit",
                        "/msi/daily-activities/create", "/msi/daily-activities/audit",
                        "/msi/honors/audit",
                        "/msi/daily-tasks/create").hasRole("ADMIN")
                .antMatchers("/msi/daily-tasks/all", "/msi/daily-tasks/stats/**",
                        "/msi/daily-tasks/export/**", "/msi/daily-tasks/delete/**",
                        "/msi/daily-tasks/completion-analysis/**",
                        "/msi/daily-tasks/student-incomplete-tasks/**").hasRole("ADMIN")
                .antMatchers("/msi/leave/pending", "/msi/leave/all", "/msi/leave/audit/**",
                        "/msi/leave/update-status/**").hasRole("ADMIN")
                .antMatchers("/msi/internship-employment/all", "/msi/internship-employment/approve/**",
                        "/msi/internship-employment/reject/**", "/msi/internship-employment/statistics/**",
                        "/msi/internship-employment/statistics",
                        "/msi/internship-employment/status/**", "/msi/internship-employment/approval-status/**",
                        "/msi/internship-employment/type/**", "/msi/internship-employment/company-type/**",
                        "/msi/internship-employment/industry/**", "/msi/internship-employment/work-type/**",
                        "/msi/internship-employment/date-range", "/msi/internship-employment/search/**",
                        "/msi/internship-employment/search", "/msi/internship-employment/export/**",
                        "/msi/internship-employment/import", "/msi/internship-employment/validate",
                        "/msi/internship-employment/student-status", "/msi/internship-employment/student-approval")
                        .hasRole("ADMIN")
                .antMatchers("/msi/competitions/search", "/msi/competitions/active",
                        "/msi/papers/search", "/msi/patents/search", "/msi/projects/search")
                        .hasRole("ADMIN")
                .antMatchers("/msi/party-member/list", "/msi/party-member/search", "/msi/party-member/pending",
                        "/msi/party-member/audit/**", "/msi/party-member/batch-import",
                        "/msi/party-member/branch/**", "/msi/party-member/status/**",
                        "/msi/party-application/list", "/msi/party-application/search",
                        "/msi/party-application/audit/**", "/msi/party-application/batch-import",
                        "/msi/party-application/stage/**",
                        "/msi/thought-report/list", "/msi/thought-report/search", "/msi/thought-report/pending",
                        "/msi/thought-report/audit/**", "/msi/thought-report/type/**",
                        "/msi/thought-report/status/**", "/msi/thought-report/reviewer/**",
                        "/msi/party-course/list", "/msi/party-course/search",
                        "/msi/party-course/pending", "/msi/party-course/audit/**",
                        "/msi/party-course/type/**", "/msi/party-course/status/**",
                        "/msi/party-course/instructor/**",
                        "/msi/volunteer-service/list", "/msi/volunteer-service/search",
                        "/msi/volunteer-service/pending", "/msi/volunteer-service/audit/**",
                        "/msi/volunteer-service/type/**", "/msi/volunteer-service/status/**",
                        "/msi/volunteer-service/organization/**",
                        "/msi/ideological-tweet/**")
                        .hasRole("ADMIN")
                .anyRequest().authenticated()
                .and()
                .exceptionHandling()
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":401,\"msg\":\"Unauthorized\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":403,\"msg\":\"Forbidden\"}");
                })
                .and()
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
