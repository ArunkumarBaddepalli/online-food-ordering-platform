package com.app.fooddelivery.service;

import com.app.fooddelivery.dto.*;
import com.app.fooddelivery.model.*;
import com.app.fooddelivery.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.ArrayList;
import java.util.List;

@Service
public class RestaurantOnboardingService {

    @Autowired
    private RestaurantOnboardingRepository onboardingRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GeocodingService geocodingService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private RestaurantOperatingHoursRepository restaurantOperatingHoursRepository;

    /**
     * Start or resume onboarding for a user.
     * - Returns existing DRAFT/SUBMITTED record (idempotent).
     * - Creates new record if REJECTED.
     * - Throws if already APPROVED.
     */
    @Transactional
    public RestaurantOnboarding startOnboarding(Long userId) {
        userRepository.findById(Objects.requireNonNull(userId, "userId required"))
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        onboardingRepository.findTopByUserIdOrderByCreatedAtDesc(userId).ifPresent(existing -> {
            if (existing.getStatus() == OnboardingStatus.APPROVED) {
                throw new RuntimeException("Restaurant already approved. Restaurant ID: " + existing.getCreatedRestaurantId());
            }
            if (existing.getStatus() == OnboardingStatus.DRAFT
                    || existing.getStatus() == OnboardingStatus.SUBMITTED
                    || existing.getStatus() == OnboardingStatus.PENDING_REVIEW
                    || existing.getStatus() == OnboardingStatus.DOCUMENTS_REQUIRED) {
                // Return handled by caller via getOnboardingByUserId
                throw new RuntimeException("EXISTING:" + existing.getOnboardingId());
            }
        });

        // Set user role to RESTAURANT_OWNER
        User user = userRepository.findById(Objects.requireNonNull(userId)).orElseThrow();
        if (!"RESTAURANT_OWNER".equals(user.getRole())) {
            user.setRole("RESTAURANT_OWNER");
            userRepository.save(user);
        }

        RestaurantOnboarding onboarding = new RestaurantOnboarding();
        onboarding.setUserId(userId);
        onboarding.setStatus(OnboardingStatus.DRAFT);
        onboarding.setCurrentStep(1);
        return onboardingRepository.save(onboarding);
    }

    public RestaurantOnboarding getOrStartOnboarding(Long userId) {
        try {
            return startOnboarding(userId);
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            if (msg != null && msg.startsWith("EXISTING:")) {
                Long id = Long.parseLong(msg.substring("EXISTING:".length()));
                return onboardingRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Onboarding not found"));
            }
            throw e;
        }
    }

    public RestaurantOnboarding getOnboarding(Long onboardingId) {
        return onboardingRepository.findById(Objects.requireNonNull(onboardingId, "onboardingId required"))
                .orElseThrow(() -> new RuntimeException("Onboarding not found: " + onboardingId));
    }

    public RestaurantOnboarding getOnboardingByUserId(Long userId) {
        return onboardingRepository.findTopByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new RuntimeException("No onboarding found for user: " + userId));
    }

    @Transactional
    public RestaurantOnboarding saveBasicInfo(Long onboardingId, BasicInfoRequest req) {
        RestaurantOnboarding ob = getOnboarding(onboardingId);
        validateEditable(ob);

        ob.setRestaurantName(req.getRestaurantName());
        ob.setDescription(req.getDescription());
        ob.setCuisineTypes(req.getCuisineTypes());
        if (req.getRestaurantType() != null) {
            ob.setRestaurantType(RestaurantType.valueOf(req.getRestaurantType()));
        }
        ob.setPhone(req.getPhone());
        ob.setEmail(req.getEmail());
        ob.setCurrentStep(Math.max(ob.getCurrentStep(), 2));
        return onboardingRepository.save(ob);
    }

    @Transactional
    public RestaurantOnboarding saveLocation(Long onboardingId, LocationRequest req) {
        RestaurantOnboarding ob = getOnboarding(onboardingId);
        validateEditable(ob);

        ob.setStreet(req.getStreet());
        ob.setCity(req.getCity());
        ob.setState(req.getState());
        ob.setZipCode(req.getZipCode());
        ob.setDeliveryRadiusKm(req.getDeliveryRadiusKm());

        if (req.getLatitude() != null && req.getLongitude() != null) {
            ob.setLatitude(req.getLatitude());
            ob.setLongitude(req.getLongitude());
        } else {
            // Geocode from address
            String fullAddress = req.getStreet() + ", " + req.getCity() + ", " + req.getState() + " " + req.getZipCode();
            GeocodingService.GeocodingResult result = geocodingService.geocodeAddress(fullAddress);
            if (result != null) {
                ob.setLatitude(result.getLatitude());
                ob.setLongitude(result.getLongitude());
            }
        }

        ob.setCurrentStep(Math.max(ob.getCurrentStep(), 3));
        return onboardingRepository.save(ob);
    }

    @Transactional
    public RestaurantOnboarding saveOperatingHours(Long onboardingId, HoursWrapperRequest req) {
        RestaurantOnboarding ob = getOnboarding(onboardingId);
        validateEditable(ob);

        List<OperatingHoursRequest> hoursList = req.getHours();
        if (hoursList == null || hoursList.size() != 7) {
            throw new RuntimeException("Exactly 7 days of operating hours required");
        }

        // Replace all hours (orphanRemoval handles deletion)
        ob.getOperatingHours().clear();
        for (OperatingHoursRequest hr : hoursList) {
            OperatingHours hours = new OperatingHours();
            hours.setDayOfWeek(hr.getDayOfWeek());
            hours.setIsOpen(hr.getIsOpen());
            hours.setOpenTime(hr.getOpenTime());
            hours.setCloseTime(hr.getCloseTime());
            ob.getOperatingHours().add(hours);
        }

        if (req.getAcceptsScheduledOrders() != null) {
            ob.setAcceptsScheduledOrders(req.getAcceptsScheduledOrders());
        }
        if (req.getSlotDurationMinutes() != null) {
            ob.setSlotDurationMinutes(req.getSlotDurationMinutes());
        }

        ob.setCurrentStep(Math.max(ob.getCurrentStep(), 4));
        return onboardingRepository.save(ob);
    }

    @Transactional
    public RestaurantOnboarding saveDocuments(Long onboardingId, String fssaiLicenseNumber,
            String panNumber, String gstin, MultipartFile fssaiDocument) {
        RestaurantOnboarding ob = getOnboarding(onboardingId);
        validateEditable(ob);

        ob.setFssaiLicenseNumber(fssaiLicenseNumber);
        ob.setPanNumber(panNumber);
        ob.setGstin(gstin);

        if (fssaiDocument != null && !fssaiDocument.isEmpty()) {
            String path = fileStorageService.storeFile(onboardingId, fssaiDocument, "fssai");
            ob.setFssaiDocumentPath(path);
        }

        ob.setCurrentStep(Math.max(ob.getCurrentStep(), 5));
        return onboardingRepository.save(ob);
    }

    @Transactional
    public RestaurantOnboarding saveBankDetails(Long onboardingId, BankDetailsRequest req) {
        RestaurantOnboarding ob = getOnboarding(onboardingId);
        validateEditable(ob);

        ob.setBankAccountHolderName(req.getBankAccountHolderName());
        ob.setBankAccountNumber(req.getBankAccountNumber());
        ob.setBankIfscCode(req.getBankIfscCode());
        ob.setBankName(req.getBankName());
        ob.setCurrentStep(Math.max(ob.getCurrentStep(), 6));
        return onboardingRepository.save(ob);
    }

    @Transactional
    public RestaurantOnboarding submitOnboarding(Long onboardingId) {
        RestaurantOnboarding ob = getOnboarding(onboardingId);
        validateEditable(ob);

        List<String> missing = new ArrayList<>();
        if (isBlank(ob.getRestaurantName())) missing.add("Restaurant Name");
        if (isBlank(ob.getPhone())) missing.add("Phone");
        if (isBlank(ob.getStreet())) missing.add("Street");
        if (isBlank(ob.getCity())) missing.add("City");
        if (isBlank(ob.getFssaiLicenseNumber())) missing.add("FSSAI License Number");
        if (isBlank(ob.getPanNumber())) missing.add("PAN Number");
        if (isBlank(ob.getBankAccountNumber())) missing.add("Bank Account Number");
        if (isBlank(ob.getBankIfscCode())) missing.add("Bank IFSC Code");

        if (!missing.isEmpty()) {
            throw new RuntimeException("Please complete required fields: " + String.join(", ", missing));
        }

        ob.setStatus(OnboardingStatus.PENDING_REVIEW);
        ob.setSubmittedAt(LocalDateTime.now());
        return onboardingRepository.save(ob);
    }

    /**
     * Admin: approve onboarding and create Restaurant record.
     */
    @Transactional
    public RestaurantOnboarding adminApprove(Long onboardingId) {
        RestaurantOnboarding ob = getOnboarding(onboardingId);
        if (ob.getStatus() != OnboardingStatus.PENDING_REVIEW
                && ob.getStatus() != OnboardingStatus.DOCUMENTS_REQUIRED) {
            throw new RuntimeException("Can only approve PENDING_REVIEW or DOCUMENTS_REQUIRED applications");
        }

        Restaurant restaurant = new Restaurant();
        restaurant.setName(ob.getRestaurantName());
        restaurant.setDescription(ob.getDescription());
        restaurant.setAddress(buildAddress(ob));
        restaurant.setPhone(ob.getPhone());
        restaurant.setEmail(ob.getEmail());
        restaurant.setLatitude(ob.getLatitude());
        restaurant.setLongitude(ob.getLongitude());
        restaurant.setDeliveryRadiusKm(ob.getDeliveryRadiusKm() != null ? ob.getDeliveryRadiusKm() : 10.0);
        restaurant.setAcceptsScheduledOrders(ob.getAcceptsScheduledOrders());
        restaurant.setSlotDurationMinutes(ob.getSlotDurationMinutes());
        restaurant.setCuisineTypes(ob.getCuisineTypes());
        restaurant.setIsOpen(false); // owner activates manually

        // Record who runs this restaurant. Without it the owner dashboard has
        // no way to work out which orders belong to them.
        userRepository.findById(ob.getUserId()).ifPresent(restaurant::setOwner);

        // Map first open day's hours to legacy fields for backward compat
        ob.getOperatingHours().stream()
                .filter(h -> Boolean.TRUE.equals(h.getIsOpen()))
                .findFirst()
                .ifPresent(h -> {
                    restaurant.setOpeningTime(h.getOpenTime());
                    restaurant.setClosingTime(h.getCloseTime());
                });

        Restaurant saved = restaurantRepository.save(restaurant);

        // Copy per-day hours to restaurant_operating_hours table
        for (OperatingHours oh : ob.getOperatingHours()) {
            RestaurantOperatingHours roh = new RestaurantOperatingHours();
            roh.setRestaurantId(saved.getId());
            roh.setDayOfWeek(oh.getDayOfWeek());
            roh.setIsOpen(oh.getIsOpen());
            roh.setOpenTime(oh.getOpenTime());
            roh.setCloseTime(oh.getCloseTime());
            restaurantOperatingHoursRepository.save(roh);
        }

        ob.setStatus(OnboardingStatus.APPROVED);
        ob.setReviewedAt(LocalDateTime.now());
        ob.setCreatedRestaurantId(saved.getId());
        return onboardingRepository.save(ob);
    }

    /**
     * Admin: reject onboarding with reason.
     */
    @Transactional
    public RestaurantOnboarding adminReject(Long onboardingId, String reason) {
        RestaurantOnboarding ob = getOnboarding(onboardingId);
        ob.setStatus(OnboardingStatus.REJECTED);
        ob.setRejectionReason(reason);
        ob.setReviewedAt(LocalDateTime.now());
        return onboardingRepository.save(ob);
    }

    /**
     * Admin: request additional documents.
     */
    @Transactional
    public RestaurantOnboarding adminRequestDocuments(Long onboardingId, String reason) {
        RestaurantOnboarding ob = getOnboarding(onboardingId);
        ob.setStatus(OnboardingStatus.DOCUMENTS_REQUIRED);
        ob.setRejectionReason(reason);
        ob.setReviewedAt(LocalDateTime.now());
        return onboardingRepository.save(ob);
    }

    public List<RestaurantOnboarding> adminListAll() {
        return onboardingRepository.findAllByOrderByCreatedAtDesc();
    }

    private void validateEditable(RestaurantOnboarding ob) {
        if (ob.getStatus() == OnboardingStatus.APPROVED) {
            throw new RuntimeException("Onboarding already approved");
        }
        if (ob.getStatus() == OnboardingStatus.REJECTED) {
            throw new RuntimeException("Onboarding was rejected. Please start a new application");
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String buildAddress(RestaurantOnboarding ob) {
        StringBuilder sb = new StringBuilder();
        if (!isBlank(ob.getStreet())) sb.append(ob.getStreet());
        if (!isBlank(ob.getCity())) sb.append(", ").append(ob.getCity());
        if (!isBlank(ob.getState())) sb.append(", ").append(ob.getState());
        if (!isBlank(ob.getZipCode())) sb.append(" ").append(ob.getZipCode());
        return sb.toString();
    }
}
