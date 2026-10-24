/**
 * FindIt - Matches View Script
 * Animates score progress bars and handles match modal/details
 */

document.addEventListener('DOMContentLoaded', () => {
    // Animate match score bars on scroll/load
    const progressFills = document.querySelectorAll('.score-progress-fill');
    progressFills.forEach(fill => {
        const targetWidth = fill.getAttribute('data-target-width') || '0%';
        fill.style.width = '0%';
        setTimeout(() => {
            fill.style.width = targetWidth;
        }, 150);
    });
});
