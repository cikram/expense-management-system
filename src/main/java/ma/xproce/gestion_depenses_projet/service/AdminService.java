package ma.xproce.gestion_depenses_projet.service;

import ma.xproce.gestion_depenses_projet.dao.entities.Admin;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface AdminService {

    Admin createAdmin(Admin admin);

    Optional<Admin> findById(Long id);

    Optional<Admin> findByUsername(String username);

    Optional<Admin> findByEmail(String email);

    List<Admin> getAllAdmins();

    Admin updateAdmin(Admin admin);

    void deleteAdmin(Long id);

    Admin changePassword(Long id, String newPassword);

    Page<Admin> getAdminsPage(int page, int size);
}
