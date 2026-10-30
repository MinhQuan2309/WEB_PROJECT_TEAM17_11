package com.utetra.repository;

import com.utetra.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<User> findByRole(Role role);

    /** Danh sach ung vien shipper cho khu Manager: nguoi dung thuong va shipper hien co. */
    List<User> findByRoleInOrderByUsernameAsc(java.util.Collection<Role> roles);

    /** Dung cho trang quan ly user cua Admin/Manager. */
    @Query("""
            select u from User u
            where (:keyword is null or :keyword = ''
                   or lower(u.username) like lower(concat('%', :keyword, '%'))
                   or lower(u.fullName) like lower(concat('%', :keyword, '%'))
                   or lower(u.email) like lower(concat('%', :keyword, '%')))
              and (:role is null or u.role = :role)
            """)
    Page<User> search(@Param("keyword") String keyword, @Param("role") Role role, Pageable pageable);
}
