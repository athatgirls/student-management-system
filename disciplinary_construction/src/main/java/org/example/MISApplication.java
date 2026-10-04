package org.example;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
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
        // Keep MongoDB LocalDateTime conversion stable across Windows and Linux deployments.
        // Event timestamps carry an explicit Z on the wire; user-entered deadlines remain local.
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));
        SpringApplication.run(MISApplication.class, args);
    }

    @Bean
    public ApplicationRunner runner(
            StudentServiceImpl studentService,
            AdminServiceImpl adminService,
            PartyMemberServiceImpl partyMemberService,
            @Value("${app.bootstrap.enabled:true}") boolean bootstrapEnabled,
            @Value("${app.demo-data.enabled:true}") boolean demoDataEnabled) {
        return args -> {
            if (bootstrapEnabled) {
                adminService.createDefaultAdmin();
            }
            if (demoDataEnabled) {
                studentService.createDefaultStudent();
                partyMemberService.createDefaultPartyMember();
            }
        };
    }
}
