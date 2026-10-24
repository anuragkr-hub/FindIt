/**
 * FindIt - Item Reporting, Image Preview, and Duplicate Warning Client Script
 */

document.addEventListener('DOMContentLoaded', () => {
    // 1. Image Preview & Validation
    const imageInput = document.getElementById('imageFile');
    const previewContainer = document.getElementById('imagePreviewContainer');
    const previewImg = document.getElementById('imagePreview');
    const removeBtn = document.getElementById('removeImageBtn');

    if (imageInput && previewContainer && previewImg) {
        imageInput.addEventListener('change', function() {
            const file = this.files[0];
            if (file) {
                // Check max size: 5MB
                if (file.size > 5 * 1024 * 1024) {
                    alert('File size exceeds the 5MB limit. Please choose a smaller image.');
                    this.value = '';
                    previewContainer.style.display = 'none';
                    return;
                }

                const reader = new FileReader();
                reader.onload = function(e) {
                    previewImg.src = e.target.result;
                    previewContainer.style.display = 'flex';
                };
                reader.readAsDataURL(file);
            } else {
                previewContainer.style.display = 'none';
            }
        });

        if (removeBtn) {
            removeBtn.addEventListener('click', () => {
                imageInput.value = '';
                previewContainer.style.display = 'none';
                previewImg.src = '';
            });
        }
    }

    // 2. Real-time Duplicate Report Warning Check
    const titleInput = document.getElementById('title');
    const categorySelect = document.getElementById('category');
    const locationInput = document.getElementById('location');
    const duplicateBox = document.getElementById('duplicateWarningBox');

    let debounceTimer = null;

    function triggerDuplicateCheck() {
        if (!titleInput || !categorySelect || !locationInput || !duplicateBox) return;

        const title = titleInput.value.trim();
        const category = categorySelect.value;
        const location = locationInput.value.trim();
        const typeElement = document.querySelector('input[name="type"]') || document.getElementById('type');
        const type = typeElement ? typeElement.value : 'LOST';

        if (title.length < 4 || !category || location.length < 3) {
            duplicateBox.style.display = 'none';
            return;
        }

        // Get CSRF Token from meta tags or input
        const csrfTokenEl = document.querySelector('input[name="_csrf"]');
        const csrfToken = csrfTokenEl ? csrfTokenEl.value : '';

        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => {
            fetch('/api/items/check-duplicate', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-CSRF-TOKEN': csrfToken
                },
                body: JSON.stringify({
                    title: title,
                    category: category,
                    location: location,
                    type: type,
                    eventDate: (document.getElementById('eventDate') ? document.getElementById('eventDate').value : null),
                    description: (document.getElementById('description') ? document.getElementById('description').value : '')
                })
            })
            .then(res => res.json())
            .then(data => {
                if (data && data.similarityScore && data.similarityScore >= 65) {
                    const existing = data.existingItem;
                    duplicateBox.innerHTML = `
                        <div class="alert alert-warning" role="alert" style="margin-bottom: 0;">
                            <svg class="item-meta-icon" style="color: #b45309; width: 22px; height: 22px;" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path>
                            </svg>
                            <div>
                                <strong style="display: block; font-size: 0.95rem;">Possible Duplicate Report Detected (${data.similarityScore}% similarity)</strong>
                                <p style="margin: 0.25rem 0 0.5rem; font-size: 0.875rem;">${data.reason}</p>
                                <a href="/items/${existing.id}" target="_blank" class="btn btn-sm btn-outline-navy" style="display: inline-flex;">
                                    View Existing Report #${existing.id} &rarr;
                                </a>
                            </div>
                        </div>
                    `;
                    duplicateBox.style.display = 'block';
                } else {
                    duplicateBox.style.display = 'none';
                }
            })
            .catch(() => {
                duplicateBox.style.display = 'none';
            });
        }, 600);
    }

    if (titleInput) titleInput.addEventListener('input', triggerDuplicateCheck);
    if (categorySelect) categorySelect.addEventListener('change', triggerDuplicateCheck);
    if (locationInput) locationInput.addEventListener('input', triggerDuplicateCheck);
});
