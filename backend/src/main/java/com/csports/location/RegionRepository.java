package com.csports.location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.csports.location.Region;

@Repository
public interface RegionRepository extends JpaRepository<Region, Long> {
    Region findByName(String name);
        
}
