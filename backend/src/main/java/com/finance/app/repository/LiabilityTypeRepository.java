package com.finance.app.repository;

import com.finance.app.model.LiabilityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LiabilityTypeRepository extends JpaRepository<LiabilityType, Long> {

    List<LiabilityType> findAllByOrderByDisplayOrderAsc();

    Optional<LiabilityType> findByType(String type);
}
