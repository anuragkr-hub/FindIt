package com.findit.repository;

import com.findit.model.Item;
import com.findit.model.User;
import com.findit.model.enums.ItemCategory;
import com.findit.model.enums.ItemStatus;
import com.findit.model.enums.ItemType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long>, JpaSpecificationExecutor<Item> {

    List<Item> findByUserOrderByCreatedAtDesc(User user);

    List<Item> findTop6ByStatusOrderByCreatedAtDesc(ItemStatus status);

    List<Item> findByTypeAndStatusIn(ItemType type, List<ItemStatus> statuses);

    List<Item> findByTypeAndStatus(ItemType type, ItemStatus status);

    @Query("SELECT i FROM Item i WHERE i.type = :type AND i.status = :status AND i.id <> :excludeId")
    List<Item> findCandidateMatches(@Param("type") ItemType type,
                                    @Param("status") ItemStatus status,
                                    @Param("excludeId") Long excludeId);

    long countByType(ItemType type);

    long countByTypeAndStatus(ItemType type, ItemStatus status);

    long countByStatus(ItemStatus status);

    long countByUser(User user);

    Page<Item> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
