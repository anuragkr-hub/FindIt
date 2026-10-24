package com.findit.service;

import com.findit.dto.DashboardStatsDto;
import com.findit.model.enums.ClaimStatus;
import com.findit.model.enums.ItemStatus;
import com.findit.model.enums.ItemType;
import com.findit.repository.ClaimRepository;
import com.findit.repository.ItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final ItemRepository itemRepository;
    private final ClaimRepository claimRepository;

    public DashboardService(ItemRepository itemRepository, ClaimRepository claimRepository) {
        this.itemRepository = itemRepository;
        this.claimRepository = claimRepository;
    }

    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats() {
        DashboardStatsDto stats = new DashboardStatsDto();

        long total = itemRepository.count();
        long lost = itemRepository.countByType(ItemType.LOST);
        long found = itemRepository.countByType(ItemType.FOUND);
        long openLost = itemRepository.countByTypeAndStatus(ItemType.LOST, ItemStatus.OPEN);
        long openFound = itemRepository.countByTypeAndStatus(ItemType.FOUND, ItemStatus.OPEN);
        long pendingClaims = claimRepository.countByStatus(ClaimStatus.PENDING);
        long resolved = itemRepository.countByStatus(ItemStatus.RESOLVED);

        int recoveryRate = (total > 0) ? (int) Math.round(((double) resolved / total) * 100) : 0;

        stats.setTotalReports(total);
        stats.setLostCount(lost);
        stats.setFoundCount(found);
        stats.setOpenLostCount(openLost);
        stats.setOpenFoundCount(openFound);
        stats.setPendingClaimsCount(pendingClaims);
        stats.setResolvedCount(resolved);
        stats.setRecoveryRate(recoveryRate);

        return stats;
    }
}
