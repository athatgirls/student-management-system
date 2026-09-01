package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.AdminModel;
import org.example.repository.AdminRepository;
import org.example.service.Impl.AdminServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock
    private AdminRepository adminRepository;

    @InjectMocks
    private AdminServiceImpl adminService;

    @Test
    void createAdminHashesPasswordAndNeverSerializesIt() throws Exception {
        AdminModel input = new AdminModel();
        input.setAdminId("ADMIN002");
        input.setUsername("teacher");
        input.setPassword("Secure123!");

        when(adminRepository.save(any(AdminModel.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdminModel saved = adminService.createAdmin(input);

        assertNotEquals("Secure123!", saved.getPassword());
        assertTrue(saved.getPassword().startsWith("$2"));
        assertFalse(new ObjectMapper().writeValueAsString(saved).contains("password"));
    }

    @Test
    void loginMigratesLegacyPlaintextPassword() {
        AdminModel legacyAdmin = new AdminModel();
        legacyAdmin.setUsername("admin");
        legacyAdmin.setPassword("admin123");
        legacyAdmin.setIsActive(true);

        when(adminRepository.findByUsername("admin")).thenReturn(Optional.of(legacyAdmin));
        when(adminRepository.save(any(AdminModel.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdminModel loggedIn = adminService.login("admin", "admin123");

        assertNotNull(loggedIn);
        assertTrue(loggedIn.getPassword().startsWith("$2"));
        verify(adminRepository).save(legacyAdmin);
    }

    @Test
    void loginRejectsWrongPassword() {
        AdminModel admin = new AdminModel();
        admin.setUsername("admin");
        admin.setPassword("admin123");
        admin.setIsActive(true);

        when(adminRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        assertNull(adminService.login("admin", "wrong-password"));
        verify(adminRepository, never()).save(any());
    }
}
