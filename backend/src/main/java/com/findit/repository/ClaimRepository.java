package com.findit.repository;

import com.findit.model.Claim;
import com.findit.model.Item;
import com.findit.model.User;
import com.findit.model.enums.ClaimStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {

    List<Claim> findByClaimantOrderByCreatedAtDesc(User claimant);

    List<Claim> findByItemOrderByCreatedAtDesc(Item item);

    @Query("SELECT c FROM Claim c WHERE c.item.user = :owner ORDER BY c.createdAt DESC")
    List<Claim> findByItemOwner(@Param("owner") User owner);

    List<Claim> findByStatusOrderByCreatedAtDesc(ClaimStatus status);

    boolean existsByItemAndClaimant(Item item, User claimant);

    long countByStatus(ClaimStatus status);

    long countByClaimant(User claimant);

    @Query("SELECT COUNT(c) FROM Claim c WHERE c.item.user = :owner AND c.status = 'PENDING'")
    long countPendingClaimsForOwner(@Param("owner") User owner);

    Page<Claim> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
