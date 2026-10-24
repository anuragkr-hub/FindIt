package com.findit.config;

import com.findit.model.Claim;
import com.findit.model.Item;
import com.findit.model.Notification;
import com.findit.model.User;
import com.findit.model.enums.*;
import com.findit.repository.ClaimRepository;
import com.findit.repository.ItemRepository;
import com.findit.repository.NotificationRepository;
import com.findit.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final ClaimRepository claimRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed-data:true}")
    private boolean seedDataEnabled;

    public DataInitializer(UserRepository userRepository,
                           ItemRepository itemRepository,
                           ClaimRepository claimRepository,
                           NotificationRepository notificationRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
        this.claimRepository = claimRepository;
        this.notificationRepository = notificationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedDataEnabled) {
            return;
        }

        if (userRepository.count() > 0) {
            return; // Data already exists
        }

        // 1. Seed Users
        User admin = new User(
                "admin@findit.campus.edu",
                passwordEncoder.encode("AdminPass123!"),
                "Campus Safety Admin",
                "555-0100",
                Role.ROLE_ADMIN
        );
        userRepository.save(admin);

        User alex = new User(
                "alex@campus.edu",
                passwordEncoder.encode("UserPass123!"),
                "Alex Rivera",
                "555-0101",
                Role.ROLE_USER
        );
        userRepository.save(alex);

        User sarah = new User(
                "sarah@campus.edu",
                passwordEncoder.encode("UserPass123!"),
                "Sarah Chen",
                "555-0102",
                Role.ROLE_USER
        );
        userRepository.save(sarah);

        User mike = new User(
                "mike@campus.edu",
                passwordEncoder.encode("UserPass123!"),
                "Michael Patel",
                "555-0103",
                Role.ROLE_USER
        );
        userRepository.save(mike);

        // 2. Seed Items
        // Pair 1: MacBook Air (Lost by Alex, Found by Sarah) -> High Match
        Item lostLaptop = new Item(
                alex,
                ItemType.LOST,
                "Space Gray MacBook Air M2 13-inch",
                "Left my 13-inch Apple MacBook Air laptop in a dark neoprene sleeve near cubicle 204. Has a NASA worm logo sticker and a Python code sticker on the top shell.",
                ItemCategory.ELECTRONICS,
                "Space Gray",
                "Main Science Library 2nd Floor Cubicles",
                LocalDate.now().minusDays(2),
                null
        );
        lostLaptop.setStatus(ItemStatus.OPEN);
        itemRepository.save(lostLaptop);

        Item foundLaptop = new Item(
                sarah,
                ItemType.FOUND,
                "Gray Apple MacBook Air Laptop at Science Library",
                "Found an Apple laptop inside a dark sleeve on study desk 204 on the second floor of the Science Library. Safe at library information desk.",
                ItemCategory.ELECTRONICS,
                "Gray",
                "Science Library 2nd Floor Study Desk",
                LocalDate.now().minusDays(2),
                null
        );
        foundLaptop.setStatus(ItemStatus.OPEN);
        itemRepository.save(foundLaptop);

        // Pair 2: Water Bottle (Lost by Sarah, Found by Mike)
        Item lostBottle = new Item(
                sarah,
                ItemType.LOST,
                "Navy Blue Hydro Flask 32oz Water Bottle",
                "32oz wide-mouth dark navy blue Hydro Flask bottle. Has slight dent on bottom rim and a national parks mountain summit sticker.",
                ItemCategory.SPORTS_WATERBOTTLES,
                "Navy Blue",
                "Campus Recreation Center Gym Locker Room",
                LocalDate.now().minusDays(3),
                null
        );
        lostBottle.setStatus(ItemStatus.OPEN);
        itemRepository.save(lostBottle);

        Item foundBottle = new Item(
                mike,
                ItemType.FOUND,
                "Blue Insulated Metal Water Flask",
                "Found a blue insulated drink bottle with outdoor stickers left on the bleachers near the recreational gym courts.",
                ItemCategory.SPORTS_WATERBOTTLES,
                "Blue",
                "Campus Recreation Center Basketball Courts",
                LocalDate.now().minusDays(3),
                null
        );
        foundBottle.setStatus(ItemStatus.OPEN);
        itemRepository.save(foundBottle);

        // Pair 3: Car Key with Lanyard (Lost by Mike, Found by Alex) -> with Claim Pending
        Item lostKeys = new Item(
                mike,
                ItemType.LOST,
                "Subaru Car Key with Red Campus Lanyard",
                "Subaru key fob with two silver house keys attached to a red woven university athletics lanyard.",
                ItemCategory.KEYS_BADGES,
                "Red and Black",
                "Student Union Dining Hall",
                LocalDate.now().minusDays(4),
                null
        );
        lostKeys.setStatus(ItemStatus.OPEN);
        itemRepository.save(lostKeys);

        Item foundKeys = new Item(
                alex,
                ItemType.FOUND,
                "Car Key Fob with Red Lanyard at Student Union",
                "Car electronic key fob with lanyard found left on lunch table near the campus coffee shop.",
                ItemCategory.KEYS_BADGES,
                "Red",
                "Student Union Cafeteria Table",
                LocalDate.now().minusDays(4),
                null
        );
        foundKeys.setStatus(ItemStatus.CLAIM_PENDING);
        itemRepository.save(foundKeys);

        // Create Claim on foundKeys by Mike
        Claim keyClaim = new Claim(
                foundKeys,
                mike,
                "The key fob is for a 2021 Subaru Outback. One of the silver house keys has 'Schlage 2841' stamped on it and the emergency key release pin has a tiny nick on the corner.",
                "555-0103"
        );
        claimRepository.save(keyClaim);

        // Item 4: Resolved Wallet (Found and returned to Alex)
        Item resolvedWallet = new Item(
                sarah,
                ItemType.FOUND,
                "Brown Leather Bi-fold Wallet",
                "Brown leather wallet found in the lecture hall auditorium. Returned safely to student after ID verification.",
                ItemCategory.WALLETS_BAGS,
                "Brown",
                "Engineering Hall Lecture Auditorium B",
                LocalDate.now().minusDays(6),
                null
        );
        resolvedWallet.setStatus(ItemStatus.RESOLVED);
        itemRepository.save(resolvedWallet);

        Claim approvedWalletClaim = new Claim(
                resolvedWallet,
                alex,
                "Contains my student ID card with name Alex Rivera and university bus pass.",
                "555-0101"
        );
        approvedWalletClaim.setStatus(ClaimStatus.APPROVED);
        approvedWalletClaim.setReviewerNotes("Verified student ID matches claimant name. Item returned at Campus Safety Office.");
        claimRepository.save(approvedWalletClaim);

        // Seed Notifications
        notificationRepository.save(new Notification(
                alex,
                "Potential Match Detected (86%)",
                "A found item 'Gray Apple MacBook Air Laptop at Science Library' matches your lost report with high confidence.",
                "/matches/" + lostLaptop.getId(),
                NotificationType.MATCH_FOUND
        ));

        notificationRepository.save(new Notification(
                alex,
                "New Claim Submitted",
                "Michael Patel submitted an ownership claim for your reported item 'Car Key Fob with Red Lanyard at Student Union'.",
                "/claims/review/" + keyClaim.getId(),
                NotificationType.CLAIM_RECEIVED
        ));

        notificationRepository.save(new Notification(
                mike,
                "Claim Under Review",
                "Your claim for 'Car Key Fob with Red Lanyard at Student Union' was received and is pending owner verification.",
                "/items/" + foundKeys.getId(),
                NotificationType.SYSTEM
        ));
    }
}
