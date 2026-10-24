package com.findit.service;

import com.findit.dto.DuplicateWarningDto;
import com.findit.dto.ItemReportDto;
import com.findit.dto.ItemSearchFilterDto;
import com.findit.dto.MatchResultDto;
import com.findit.exception.InvalidStateTransitionException;
import com.findit.exception.ResourceNotFoundException;
import com.findit.exception.UnauthorizedException;
import com.findit.model.Item;
import com.findit.model.User;
import com.findit.model.enums.ItemStatus;
import com.findit.model.enums.ItemType;
import com.findit.model.enums.NotificationType;
import com.findit.repository.ItemRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ItemService {

    private final ItemRepository itemRepository;
    private final FileStorageService fileStorageService;
    private final MatchingService matchingService;
    private final NotificationService notificationService;

    public ItemService(ItemRepository itemRepository,
                       FileStorageService fileStorageService,
                       MatchingService matchingService,
                       NotificationService notificationService) {
        this.itemRepository = itemRepository;
        this.fileStorageService = fileStorageService;
        this.matchingService = matchingService;
        this.notificationService = notificationService;
    }

    @Transactional
    public Item createItem(ItemReportDto dto, User reporter) {
        String imageUrl = null;
        if (dto.getImageFile() != null && !dto.getImageFile().isEmpty()) {
            imageUrl = fileStorageService.storeFile(dto.getImageFile());
        }

        Item item = new Item(
                reporter,
                dto.getType(),
                dto.getTitle().trim(),
                dto.getDescription().trim(),
                dto.getCategory(),
                dto.getColor() != null ? dto.getColor().trim() : null,
                dto.getLocation().trim(),
                dto.getEventDate(),
                imageUrl
        );

        Item savedItem = itemRepository.save(item);

        // Run automated matching to notify users if a strong match is discovered
        try {
            List<MatchResultDto> matches = matchingService.findMatchesForItem(savedItem, 60);
            for (MatchResultDto match : matches) {
                Item matchedItem = match.getCandidateItem();
                String notificationTitle = "Potential Match Found (" + match.getTotalScore() + "%): " + matchedItem.getTitle();
                String message = String.format("A new %s item '%s' posted at %s has a %d%% match score with your report.",
                        savedItem.getType().getDisplayName().toLowerCase(), savedItem.getTitle(), savedItem.getLocation(), match.getTotalScore());

                // Notify both reporters
                notificationService.sendNotification(
                        matchedItem.getUser(),
                        notificationTitle,
                        message,
                        "/matches/" + matchedItem.getId(),
                        NotificationType.MATCH_FOUND
                );
            }
        } catch (Exception e) {
            // Log and do not fail item creation
        }

        return savedItem;
    }

    @Transactional
    public Item updateItem(Long itemId, ItemReportDto dto, User currentUser) {
        Item item = findById(itemId);

        // Ownership or admin check
        if (!item.isOwnedBy(currentUser.getId()) && !currentUser.isAdmin()) {
            throw new UnauthorizedException("You are not authorized to edit this report");
        }

        if (item.getStatus() == ItemStatus.RESOLVED && !currentUser.isAdmin()) {
            throw new InvalidStateTransitionException("Resolved reports cannot be edited");
        }

        item.setTitle(dto.getTitle().trim());
        item.setDescription(dto.getDescription().trim());
        item.setCategory(dto.getCategory());
        item.setColor(dto.getColor() != null ? dto.getColor().trim() : null);
        item.setLocation(dto.getLocation().trim());
        item.setEventDate(dto.getEventDate());

        if (dto.getImageFile() != null && !dto.getImageFile().isEmpty()) {
            String newImageUrl = fileStorageService.storeFile(dto.getImageFile());
            item.setImageUrl(newImageUrl);
        }

        return itemRepository.save(item);
    }

    @Transactional(readOnly = true)
    public Item findById(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item report not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public Page<Item> searchItems(ItemSearchFilterDto filter) {
        Specification<Item> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Exclude withdrawn items by default in public browse
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            } else {
                predicates.add(root.get("status").in(ItemStatus.OPEN, ItemStatus.CLAIM_PENDING, ItemStatus.RESOLVED));
            }

            // Item Type (LOST, FOUND)
            if (filter.getType() != null) {
                predicates.add(cb.equal(root.get("type"), filter.getType()));
            }

            // Category
            if (filter.getCategory() != null) {
                predicates.add(cb.equal(root.get("category"), filter.getCategory()));
            }

            // Keyword query: searches title, description, and location
            if (filter.getQ() != null && !filter.getQ().trim().isEmpty()) {
                String pattern = "%" + filter.getQ().trim().toLowerCase() + "%";
                Predicate titleLike = cb.like(cb.lower(root.get("title")), pattern);
                Predicate descLike = cb.like(cb.lower(root.get("description")), pattern);
                Predicate locLike = cb.like(cb.lower(root.get("location")), pattern);
                predicates.add(cb.or(titleLike, descLike, locLike));
            }

            // Location
            if (filter.getLocation() != null && !filter.getLocation().trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("location")), "%" + filter.getLocation().trim().toLowerCase() + "%"));
            }

            // Color
            if (filter.getColor() != null && !filter.getColor().trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("color")), "%" + filter.getColor().trim().toLowerCase() + "%"));
            }

            // Date Range
            if (filter.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("eventDate"), filter.getFromDate()));
            }
            if (filter.getToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("eventDate"), filter.getToDate()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        if ("oldest".equalsIgnoreCase(filter.getSort())) {
            sort = Sort.by(Sort.Direction.ASC, "createdAt");
        } else if ("eventDateDesc".equalsIgnoreCase(filter.getSort())) {
            sort = Sort.by(Sort.Direction.DESC, "eventDate");
        } else if ("eventDateAsc".equalsIgnoreCase(filter.getSort())) {
            sort = Sort.by(Sort.Direction.ASC, "eventDate");
        }

        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);
        return itemRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public List<Item> getRecentItems() {
        return itemRepository.findTop6ByStatusOrderByCreatedAtDesc(ItemStatus.OPEN);
    }

    @Transactional(readOnly = true)
    public List<Item> getUserItems(User user) {
        return itemRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional
    public void withdrawItem(Long itemId, User currentUser) {
        Item item = findById(itemId);

        if (!item.isOwnedBy(currentUser.getId()) && !currentUser.isAdmin()) {
            throw new UnauthorizedException("You are not authorized to withdraw this item");
        }

        if (item.getStatus() == ItemStatus.RESOLVED) {
            throw new InvalidStateTransitionException("Resolved items cannot be withdrawn");
        }

        item.setStatus(ItemStatus.WITHDRAWN);
        itemRepository.save(item);
    }

    @Transactional
    public void reopenItem(Long itemId, User currentUser) {
        Item item = findById(itemId);

        if (!item.isOwnedBy(currentUser.getId()) && !currentUser.isAdmin()) {
            throw new UnauthorizedException("You are not authorized to reopen this item");
        }

        if (item.getStatus() == ItemStatus.RESOLVED) {
            throw new InvalidStateTransitionException("Resolved items cannot be reopened directly");
        }

        item.setStatus(ItemStatus.OPEN);
        itemRepository.save(item);
    }

    @Transactional
    public void deleteItem(Long itemId, User currentUser) {
        if (!currentUser.isAdmin()) {
            throw new UnauthorizedException("Only administrators can delete items permanently");
        }
        itemRepository.deleteById(itemId);
    }

    @Transactional(readOnly = true)
    public Optional<DuplicateWarningDto> checkDuplicate(ItemReportDto dto) {
        Item dummy = new Item();
        dummy.setId(dto.getId());
        dummy.setType(dto.getType());
        dummy.setTitle(dto.getTitle());
        dummy.setDescription(dto.getDescription());
        dummy.setCategory(dto.getCategory());
        dummy.setLocation(dto.getLocation());
        dummy.setColor(dto.getColor());
        dummy.setEventDate(dto.getEventDate());

        return matchingService.checkForDuplicates(dummy);
    }
}
