package ru.mkilord.node.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.mkilord.node.model.ClubRating;

public interface ClubRatingRepository extends JpaRepository<ClubRating, ClubRating.Id> {

    @Query("select avg(r.rating) from ClubRating r where r.id.clubId = :clubId")
    Double averageForClub(@Param("clubId") Long clubId);

    @Query("select count(r) from ClubRating r where r.id.clubId = :clubId")
    long countForClub(@Param("clubId") Long clubId);
}
