# FindIt - Frontend Architecture & Visual Guide

## Overview
The `frontend/` directory contains all presentation-layer assets, responsive CSS stylesheets, vanilla JavaScript client modules, and Thymeleaf 3 HTML templates for FindIt.

## Directory Structure
```
frontend/
├── static/
│   ├── css/
│   │   ├── main.css         # Navy/blue/white visual identity, typography, buttons, reset, nav
│   │   ├── components.css   # Item cards, badges, dashboard metric cards, score progress bars, tables
│   │   └── responsive.css   # Mobile off-canvas drawer, media queries, accessibility, print
│   ├── js/
│   │   ├── app.js          # Mobile drawer toggle, accessible keyboard trap, user dropdown, tabs
│   │   ├── items.js        # Real-time duplicate report detector (debounced AJAX) & image preview
│   │   └── matches.js      # Animated match score progress rings and comparison helpers
│   └── img/
│       ├── logo.svg
│       └── default-item.svg
└── templates/
    ├── layout/
    │   ├── base.html        # Master responsive layout with skip links
    │   ├── header.html      # Responsive header with notification counter and user menu
    │   ├── footer.html      # Campus safety links and footer
    │   └── alerts.html      # Flash message banners (success, error, warning, info)
    ├── pages/
    │   ├── index.html       # Landing page with hero, search, statistics, and recent items
    │   ├── browse.html      # Multi-facet filterable item catalog with pagination
    │   ├── item-detail.html # Item details, evidence review, and match preview
    │   ├── report-lost.html # Lost item reporting form with live duplicate detector
    │   ├── report-found.html# Found item reporting form
    │   ├── edit-item.html   # Report editing
    │   ├── dashboard.html   # User dashboard with metrics, tabs, and claim trackers
    │   ├── matches.html     # Deterministic weighted match visualizer & breakdown
    │   ├── notifications.html# In-app notifications center with unread counters
    │   ├── submit-claim.html# Ownership verification and evidence submission
    │   ├── review-claims.html# Private evidence review with approve/reject workflow
    │   ├── profile.html     # User profile and security settings
    │   ├── error.html       # Friendly 404, 403, and 500 error pages
    │   └── admin/
    │       └── dashboard.html# Moderator console with items, claims, users, and analytics
    └── auth/
        ├── login.html       # Authentication form with demo account helpers
        └── register.html    # User registration with Bean Validation
```

## Visual Identity
- **Primary Navy:** `#0a192f`, `#0f2942`, `#1e3a5f`
- **Primary Blue Accent:** `#2563eb`, `#3b82f6`, `#60a5fa`
- **Background & Neutrals:** `#ffffff`, `#f8fafc`, `#f1f5f9`, `#e2e8f0`, `#1e293b`
- **State Colors:** Emerald Green `#10b981` (Found / Resolved / Approved), Amber `#f59e0b` (Lost / Pending), Crimson `#ef4444` (Withdrawn / Rejected).
