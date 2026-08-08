package com.app.fooddelivery.repository;

import com.app.fooddelivery.model.SavedAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedAddressRepository extends JpaRepository<SavedAddress, Long> {
    List<SavedAddress> findByUserId(Long userId);

    Optional<SavedAddress> findByUserIdAndIsDefaultTrue(Long userId);
}
