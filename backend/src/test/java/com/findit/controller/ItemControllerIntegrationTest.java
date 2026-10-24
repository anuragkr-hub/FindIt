package com.findit.controller;

import com.findit.model.Item;
import com.findit.model.User;
import com.findit.model.enums.ItemCategory;
import com.findit.model.enums.ItemStatus;
import com.findit.model.enums.ItemType;
import com.findit.model.enums.Role;
import com.findit.repository.ItemRepository;
import com.findit.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ItemControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    private User testUser;
    private Item testItem;

    @BeforeEach
    void setUp() {
        testUser = new User("tester@campus.edu", "$2a$12$DummyHashForTestingOnly...", "Tester Smith", "555-1234", Role.ROLE_USER);
        userRepository.save(testUser);

        testItem = new Item(testUser, ItemType.LOST, "Calculus 3 Textbook",
                "Hardcover Stewart Calculus 9th edition with blue post-it notes",
                ItemCategory.BOOKS_NOTEBOOKS, "Blue", "Math Building Room 101",
                LocalDate.now().minusDays(1), null);
        itemRepository.save(testItem);
    }

    @Test
    @DisplayName("Public browse page renders without authentication")
    void testBrowseItemsPublic() throws Exception {
        mockMvc.perform(get("/items"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/browse"))
                .andExpect(model().attributeExists("itemPage"));
    }

    @Test
    @DisplayName("Public item detail page renders for existing item")
    void testItemDetailPublic() throws Exception {
        mockMvc.perform(get("/items/" + testItem.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/item-detail"))
                .andExpect(model().attributeExists("item"));
    }

    @Test
    @DisplayName("Unauthenticated user attempting to report lost is redirected to login")
    void testUnauthenticatedReportRedirects() throws Exception {
        mockMvc.perform(get("/items/report/lost"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "tester@campus.edu", roles = {"USER"})
    @DisplayName("Authenticated user can view and submit report form")
    void testAuthenticatedReportSubmission() throws Exception {
        mockMvc.perform(get("/items/report/lost"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/report-lost"))
                .andExpect(model().attributeExists("itemDto"));

        mockMvc.perform(post("/items/report/lost")
                        .with(csrf())
                        .param("type", "LOST")
                        .param("title", "Car Keys with Lanyard")
                        .param("category", "KEYS_BADGES")
                        .param("color", "Red")
                        .param("location", "Student Union 1st Floor")
                        .param("eventDate", LocalDate.now().toString())
                        .param("description", "Two brass keys on a red campus athletics lanyard."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/items/*"));
    }
}
