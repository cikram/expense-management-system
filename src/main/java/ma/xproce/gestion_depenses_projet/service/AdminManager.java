package ma.xproce.gestion_depenses_projet.service;

import lombok.RequiredArgsConstructor;
import ma.xproce.gestion_depenses_projet.dao.entities.Admin;
import ma.xproce.gestion_depenses_projet.dao.repositories.AdminRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminManager implements AdminService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Admin createAdmin(Admin admin) {


        if (adminRepository.existsByUsername(admin.getUsername())) {
            throw new RuntimeException("Nom d'utilisateur admin déjà utilisé !");
        }


        if (adminRepository.existsByEmail(admin.getEmail())) {
            throw new RuntimeException("Email admin déjà utilisé !");
        }


        admin.setPassword(passwordEncoder.encode(admin.getPassword()));

        return adminRepository.save(admin);
    }

    @Override
    public Optional<Admin> findById(Long id) {
        return adminRepository.findById(id);
    }

    @Override
    public Optional<Admin> findByUsername(String username) {
        return adminRepository.findByUsername(username);
    }

    @Override
    public Optional<Admin> findByEmail(String email) {
        return adminRepository.findByEmail(email);
    }

    @Override
    public List<Admin> getAllAdmins() {
        return adminRepository.findAll();
    }

    @Override
    public Admin updateAdmin(Admin admin) {
        Admin existing = adminRepository.findById(admin.getId())
                .orElseThrow(() -> new RuntimeException("Admin non trouvé !"));


        existing.setUsername(admin.getUsername());
        existing.setEmail(admin.getEmail());

        return adminRepository.save(existing);
    }

    @Override
    public void deleteAdmin(Long id) {
        adminRepository.deleteById(id);
    }

    @Override
    public Admin changePassword(Long id, String newPassword) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Admin non trouvé !"));

        admin.setPassword(passwordEncoder.encode(newPassword));

        return adminRepository.save(admin);
    }

    @Override
    public Page<Admin> getAdminsPage(int page, int size) {
        return adminRepository.findAll(PageRequest.of(page, size));
    }
}
