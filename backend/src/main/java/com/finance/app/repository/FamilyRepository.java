package com.finance.app.repository;

import com.finance.app.model.Family;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FamilyRepository extends JpaRepository<Family, Long> {

    Optional<Family> findByFamilyName(String familyName);

    Optional<Family> findByIsDefaultTrue();
}
