package com.Csports.Csports.repository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.Csports.Csports.model.UserLocation;
import com.Csports.Csports.model.User;


@Repository
public interface UserLocationRepository extends JpaRepository<UserLocation, Long> {

    Optional<UserLocation> findByUser(User user);
}
