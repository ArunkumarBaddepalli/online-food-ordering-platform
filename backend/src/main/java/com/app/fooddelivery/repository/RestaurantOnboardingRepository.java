package com.app.fooddelivery.repository;

import com.app.fooddelivery.model.OnboardingStatus;
import com.app.fooddelivery.model.RestaurantOnboarding;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RestaurantOnboardingRepository extends JpaRepository<RestaurantOnboarding, Long> {
    Optional<RestaurantOnboarding> findTopByUserIdOrderByCreatedAtDesc(Long userId);
    List<RestaurantOnboarding> findAllByOrderByCreatedAtDesc();
    List<RestaurantOnboarding> findByStatus(OnboardingStatus status);
}
