package com.Csports.Csports.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.Csports.Csports.model.Region;

@Repository
public interface RegionRepository extends JpaRepository<Region, Long> {
    Region findByName(String name);
        
}
