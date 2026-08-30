package com.illbethere.repo;

import com.illbethere.domain.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    Optional<Favorite> findByUserIdAndLocationId(Long userId, Long locationId);

    @Query("""
            select f from Favorite f
            join fetch f.location
            where f.user.id = :userId
            order by f.lastActivityAt desc, f.createdAt desc
            """)
    List<Favorite> findByUserIdOrderByActivity(@Param("userId") Long userId);

    void deleteByUserIdAndLocationId(Long userId, Long locationId);

    boolean existsByUserIdAndLocationId(Long userId, Long locationId);
}
