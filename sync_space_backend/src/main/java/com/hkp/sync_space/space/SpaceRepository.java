package com.hkp.sync_space.space;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpaceRepository extends JpaRepository<Space, UUID> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select s from Space s where s.id = :id")
	Optional<Space> lockById(@Param("id") UUID id);

}
