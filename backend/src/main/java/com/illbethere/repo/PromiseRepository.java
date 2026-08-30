package com.illbethere.repo;

import com.illbethere.domain.AttendancePromise;
import com.illbethere.domain.PromiseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PromiseRepository extends JpaRepository<AttendancePromise, Long> {

    @Query("""
            select p from AttendancePromise p
            join fetch p.user
            where p.location.id = :locationId
              and p.status = :status
              and p.slotStart >= :from
              and p.slotStart < :to
            """)
    List<AttendancePromise> findActiveInRange(
            @Param("locationId") Long locationId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("status") PromiseStatus status);

    Optional<AttendancePromise> findByUserIdAndLocationIdAndSlotStartAndStatus(
            Long userId, Long locationId, Instant slotStart, PromiseStatus status);

    @Query("""
            select p from AttendancePromise p
            join fetch p.location
            where p.user.id = :userId and p.status = :status
            order by p.slotStart
            """)
    List<AttendancePromise> findByUserIdAndStatusOrderBySlotStartAsc(
            @Param("userId") Long userId, @Param("status") PromiseStatus status);
}
