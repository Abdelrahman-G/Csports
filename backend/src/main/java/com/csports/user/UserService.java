package com.csports.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.csports.auth.exception.EmailAlreadyExistsException;
import com.csports.auth.exception.PhoneNumberAlreadyExistsException;
import com.csports.booking.dto.BookedSessionResponse;
import com.csports.common.pagination.PageResponse;
import com.csports.common.exception.ResourceNotFoundException;
import com.csports.booking.BookingMapper;
import com.csports.booking.Booking;
import com.csports.booking.BookingStatus;
import com.csports.booking.BookingRepository;
import com.csports.location.Region;
import com.csports.location.RegionRepository;
import com.csports.location.UserLocation;
import com.csports.user.dto.UpdateUserProfileRequest;
import com.csports.user.dto.UserProfileResponse;
import com.csports.user.exception.InvalidProfileUpdateException;

@Service
public class UserService {
    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;
    private final UserRepository userRepository;
    private final RegionRepository regionRepository;

    public UserService(
            BookingRepository bookingRepository,
            BookingMapper bookingMapper,
            UserRepository userRepository,
            RegionRepository regionRepository) {
        this.bookingRepository = bookingRepository;
        this.bookingMapper = bookingMapper;
        this.userRepository = userRepository;
        this.regionRepository = regionRepository;
    }

    public User getCurrentUser() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        return (User) authentication.getPrincipal();
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile() {
        return toProfileResponse(getManagedCurrentUser());
    }

    @Transactional
    public UserProfileResponse updateMyProfile(UpdateUserProfileRequest request) {
        User user = getManagedCurrentUser();

        if (!request.hasEditableFields()) {
            throw new InvalidProfileUpdateException(
                    "At least one editable account field must be provided.");
        }
        if (request.age() != null
                && user.getRole() == Role.TRAINER
                && request.age() < 18) {
            throw new InvalidProfileUpdateException(
                    "A trainer must be at least 18 years old.");
        }

        if (request.email() != null
                && !request.email().equals(user.getEmail())
                && userRepository.existsByEmailAndIdNot(request.email(), user.getId())) {
            throw new EmailAlreadyExistsException("Email already exists.");
        }
        if (request.phoneNumber() != null
                && !request.phoneNumber().equals(user.getPhoneNumber())
                && userRepository.existsByPhoneNumberAndIdNot(
                        request.phoneNumber(),
                        user.getId())) {
            throw new PhoneNumberAlreadyExistsException(
                    "This phone number is already registered.");
        }

        if (request.name() != null) {
            user.setName(request.name().trim());
        }
        if (request.email() != null) {
            user.setEmail(request.email().trim());
        }
        if (request.phoneNumber() != null) {
            user.setPhoneNumber(request.phoneNumber());
        }
        if (request.age() != null) {
            user.setAge(request.age());
        }
        updateLocation(user, request);

        return toProfileResponse(user);
    }

    @Transactional(readOnly = true)
    public PageResponse<BookedSessionResponse> getMySessions(int page, int size) {

        User currentUser = getCurrentUser();

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("bookedAt").descending());

        Page<Booking> bookings = bookingRepository.findByUserAndStatus(
                currentUser,
                BookingStatus.CONFIRMED,
                pageable);

        Page<BookedSessionResponse> response = bookings.map(bookingMapper::toResponse);

        return new PageResponse<>(
                response.getContent(),
                response.getNumber(),
                response.getSize(),
                response.getTotalElements(),
                response.getTotalPages(),
                response.isFirst(),
                response.isLast());
    }

    private User getManagedCurrentUser() {
        Long userId = getCurrentUser().getId();
        return userRepository.findProfileById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User account not found."));
    }

    private void updateLocation(User user, UpdateUserProfileRequest request) {
        if (!request.hasLocationFields()) {
            return;
        }

        UserLocation location = user.getLocation();
        if (location == null) {
            if (request.regionId() == null
                    || request.latitude() == null
                    || request.longitude() == null) {
                throw new InvalidProfileUpdateException(
                        "Region, latitude, and longitude are all required when adding a location.");
            }
            location = new UserLocation();
            user.setLocation(location);
        }

        if (request.regionId() != null) {
            Region region = regionRepository.findById(request.regionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Region not found."));
            location.setRegion(region);
        }
        if (request.latitude() != null) {
            location.setLatitude(request.latitude());
        }
        if (request.longitude() != null) {
            location.setLongitude(request.longitude());
        }
    }

    private UserProfileResponse toProfileResponse(User user) {
        UserLocation location = user.getLocation();
        Region region = location == null ? null : location.getRegion();
        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getAge(),
                user.getRole(),
                user.getPhotoUrl(),
                region == null ? null : region.getId(),
                region == null ? null : region.getName(),
                region == null ? null : region.getCity(),
                region == null ? null : region.getCountry(),
                location == null ? null : location.getLatitude(),
                location == null ? null : location.getLongitude());
    }
}
