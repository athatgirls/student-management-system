package org.example;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.example.service.Impl.StudentServiceImpl;
import org.example.service.Impl.AdminServiceImpl;
import org.example.service.Impl.PartyMemberServiceImpl;

@SpringBootApplication(
    exclude = DataSourceAutoConfiguration.class,
    scanBasePackages = "org.example"  // 扫描整个 org.example 包
)
@EnableScheduling
public class MISApplication {
    public static void main(String[] args) {
        SpringApplication.run(MISApplication.class, args);
    }

    @Bean
    public ApplicationRunner runner(StudentServiceImpl studentService, AdminServiceImpl adminService, PartyMemberServiceImpl partyMemberService) {
        return args -> {
            studentService.createDefaultStudent();
            adminService.createDefaultAdmin();
            partyMemberService.createDefaultPartyMember();
        };
    }
}
