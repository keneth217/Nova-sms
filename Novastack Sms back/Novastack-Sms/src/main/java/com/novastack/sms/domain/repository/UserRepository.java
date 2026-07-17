package com.novastack.sms.domain.repository;

import com.novastack.sms.domain.entity.User;
import com.novastack.sms.domain.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.organization WHERE u.email = :email")
    Optional<User> findByEmailWithOrganization(@Param("email") String email);

    @Query("""
            SELECT u FROM User u
            LEFT JOIN FETCH u.organization o
            WHERE o.phone = :phone
              AND u.role = 'ORGANIZATION_ADMIN'
            ORDER BY u.createdAt ASC
            """)
    java.util.List<User> findOrgAdminsByOrganizationPhone(@Param("phone") String phone);

    default Optional<User> findOrgAdminByOrganizationPhone(String phone) {
        var list = findOrgAdminsByOrganizationPhone(phone);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    boolean existsByEmail(String email);

    long countByOrganizationId(UUID organizationId);

    @Query("""
            SELECT u FROM User u
            WHERE (:role IS NULL OR u.role = :role)
              AND (:organizationId IS NULL OR u.organization.id = :organizationId)
              AND (
                    :search IS NULL OR :search = ''
                    OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
                  )
            ORDER BY u.createdAt DESC
            """)
    Page<User> search(
            @Param("role") UserRole role,
            @Param("organizationId") UUID organizationId,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.organization WHERE u.id = :id")
    Optional<User> findByIdWithOrganization(@Param("id") UUID id);

    long countByRole(UserRole role);
}
