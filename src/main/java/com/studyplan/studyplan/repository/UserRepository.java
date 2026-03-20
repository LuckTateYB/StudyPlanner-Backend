package com.studyplan.studyplan.repository;
import com.studyplan.studyplan.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Necesario para login cuando se implemente JWT
    Optional<User> findByEmail(String email);
}