package com.csports.sport;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.csports.sport.Sport;

public interface SportRepository extends JpaRepository<Sport, Long> {
        Optional<Sport> findByName(String name);

}
