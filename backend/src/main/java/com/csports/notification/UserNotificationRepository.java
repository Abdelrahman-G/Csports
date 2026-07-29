package com.csports.notification;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.csports.user.User;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {

    Page<UserNotification> findByRecipient(User recipient, Pageable pageable);

    Optional<UserNotification> findByIdAndRecipient(Long id, User recipient);
}
