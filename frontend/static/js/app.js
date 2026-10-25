/**
 * FindIt - Main Application Client Script
 * Handles navigation, mobile drawer, dropdowns, accessible controls, and alerts
 */

document.addEventListener('DOMContentLoaded', () => {
    // 1. User Dropdown Menu
    const userMenuBtn = document.getElementById('userMenuBtn');
    const userDropdown = document.getElementById('userDropdown');

    if (userMenuBtn && userDropdown) {
        userMenuBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            const isOpen = userDropdown.classList.contains('show');
            userDropdown.classList.toggle('show');
            userMenuBtn.setAttribute('aria-expanded', !isOpen);
        });

        document.addEventListener('click', (e) => {
            if (!userDropdown.contains(e.target) && !userMenuBtn.contains(e.target)) {
                userDropdown.classList.remove('show');
                userMenuBtn.setAttribute('aria-expanded', 'false');
            }
        });

        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && userDropdown.classList.contains('show')) {
                userDropdown.classList.remove('show');
                userMenuBtn.setAttribute('aria-expanded', 'false');
                userMenuBtn.focus();
            }
        });
    }

    // 2. Mobile Off-canvas Navigation Drawer
    const mobileToggleBtn = document.getElementById('mobileNavToggle');
    const mobileDrawer = document.getElementById('mobileNavDrawer');
    const mobileDrawerClose = document.getElementById('mobileDrawerClose');
    const mobileBackdrop = document.getElementById('mobileNavBackdrop');

    function openMobileDrawer() {
        if (mobileDrawer && mobileBackdrop) {
            mobileDrawer.classList.add('open');
            mobileBackdrop.classList.add('show');
            document.body.style.overflow = 'hidden';
            if (mobileDrawerClose) mobileDrawerClose.focus();
        }
    }

    function closeMobileDrawer() {
        if (mobileDrawer && mobileBackdrop) {
            mobileDrawer.classList.remove('open');
            mobileBackdrop.classList.remove('show');
            document.body.style.overflow = '';
            if (mobileToggleBtn) mobileToggleBtn.focus();
        }
    }

    if (mobileToggleBtn) {
        mobileToggleBtn.addEventListener('click', openMobileDrawer);
    }
    if (mobileDrawerClose) {
        mobileDrawerClose.addEventListener('click', closeMobileDrawer);
    }
    if (mobileBackdrop) {
        mobileBackdrop.addEventListener('click', closeMobileDrawer);
    }

    // 3. Dismissible Alert Banners
    document.querySelectorAll('.alert-dismiss-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            const alert = this.closest('.alert');
            if (alert) {
                alert.style.transition = 'opacity 0.2s ease, transform 0.2s ease';
                alert.style.opacity = '0';
                alert.style.transform = 'translateY(-8px)';
                setTimeout(() => alert.remove(), 200);
            }
        });
    });

    // 4. Client-side Tabs
    document.querySelectorAll('[data-tab-target]').forEach(tabBtn => {
        tabBtn.addEventListener('click', function(e) {
            e.preventDefault();
            const targetSelector = this.getAttribute('data-tab-target');
            const targetPane = document.querySelector(targetSelector);

            if (!targetPane) return;

            // Deactivate all sibling tabs in container
            const tabsNav = this.closest('.tabs-nav, .modern-segmented-tabs');
            if (tabsNav) {
                tabsNav.querySelectorAll('.tab-link, .segmented-tab-btn').forEach(btn => btn.classList.remove('active'));
            }
            this.classList.add('active');

            // Hide sibling tab panes
            const panesContainer = targetPane.parentElement;
            panesContainer.querySelectorAll('.tab-pane').forEach(pane => {
                pane.style.display = 'none';
            });

            // Show targeted pane
            targetPane.style.display = 'block';

            // Optional: update URL hash without scrolling
            if (history.replaceState) {
                history.replaceState(null, null, targetSelector);
            }
        });
    });

    // Check URL hash on load for tab activation
    if (window.location.hash) {
        const matchingTab = document.querySelector(`[data-tab-target="${window.location.hash}"]`);
        if (matchingTab) {
            matchingTab.click();
        }
    }
});
