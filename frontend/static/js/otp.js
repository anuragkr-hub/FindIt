/**
 * FindIt - OTP Security Gateway & Identity Verification System
 * Handles client-side OTP dispatch, countdown timers, verification, and instant OTP login.
 */
(function() {
    'use strict';

    window.FindItOTP = {
        timers: {},

        /**
         * Dispatches an OTP to the given target (email or mobile)
         */
        sendOtp: function(options) {
            const { target, purpose, buttonEl, statusEl, codeContainerEl, autoFillInputEl, onDispatched } = options;
            if (!target || !target.trim()) {
                this.showStatus(statusEl, 'Please enter a valid campus email or phone number first.', 'error');
                return;
            }

            const sendBtn = typeof buttonEl === 'string' ? document.getElementById(buttonEl) : buttonEl;
            const originalText = sendBtn ? sendBtn.innerHTML : 'Send OTP';

            if (sendBtn) {
                sendBtn.disabled = true;
                sendBtn.innerHTML = '<span class="otp-spinner"></span> Sending...';
            }
            this.showStatus(statusEl, 'Connecting to security gateway...', 'info');

            fetch('/api/otp/send', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                },
                body: JSON.stringify({
                    target: target.trim(),
                    purpose: purpose || 'REGISTRATION'
                })
            })
            .then(res => res.json().then(data => ({ status: res.status, ok: res.ok, body: data })))
            .then(result => {
                if (!result.ok) {
                    throw new Error(result.body.message || 'Unable to dispatch verification code.');
                }

                const data = result.body;
                this.showStatus(statusEl, data.message || 'Verification code sent successfully!', 'success');

                // Reveal OTP input box if hidden
                if (codeContainerEl) {
                    const cEl = typeof codeContainerEl === 'string' ? document.getElementById(codeContainerEl) : codeContainerEl;
                    if (cEl) {
                        cEl.style.display = 'block';
                        cEl.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
                    }
                }

                // Show local demo assistant toast / banner with auto-fill option
                if (data.demoCode) {
                    this.showDemoHelper(data.demoCode, autoFillInputEl);
                }

                // Start countdown timer on send button
                this.startCountdown(sendBtn, data.expirySeconds || 300, originalText);

                if (typeof onDispatched === 'function') {
                    onDispatched(data);
                }
            })
            .catch(err => {
                this.showStatus(statusEl, err.message, 'error');
                if (sendBtn) {
                    sendBtn.disabled = false;
                    sendBtn.innerHTML = originalText;
                }
            });
        },

        /**
         * Verifies an entered OTP code
         */
        verifyOtp: function(options) {
            const { target, code, purpose, verifyBtnEl, statusEl, onSuccess, onError } = options;
            const btn = typeof verifyBtnEl === 'string' ? document.getElementById(verifyBtnEl) : verifyBtnEl;
            const originalText = btn ? btn.innerHTML : 'Verify';

            if (!code || code.trim().length !== 6) {
                this.showStatus(statusEl, 'Please enter the complete 6-digit verification code.', 'error');
                return;
            }

            if (btn) {
                btn.disabled = true;
                btn.innerHTML = '<span class="otp-spinner"></span> Verifying...';
            }

            fetch('/api/otp/verify', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                },
                body: JSON.stringify({
                    target: target.trim(),
                    code: code.trim(),
                    purpose: purpose || 'REGISTRATION'
                })
            })
            .then(res => res.json().then(data => ({ status: res.status, ok: res.ok, body: data })))
            .then(result => {
                if (!result.ok) {
                    throw new Error(result.body.message || 'Invalid or expired OTP code.');
                }

                if (btn) {
                    btn.innerHTML = '&#10003; Verified';
                    btn.className = 'btn btn-success';
                }

                this.showStatus(statusEl, '&#10003; Identity verified successfully!', 'success');

                if (typeof onSuccess === 'function') {
                    onSuccess(result.body);
                }
            })
            .catch(err => {
                this.showStatus(statusEl, err.message, 'error');
                if (btn) {
                    btn.disabled = false;
                    btn.innerHTML = originalText;
                }
                if (typeof onError === 'function') {
                    onError(err);
                }
            });
        },

        /**
         * Helper display for demo environment with quick auto-fill
         */
        showDemoHelper: function(code, autoFillInputEl) {
            let helper = document.getElementById('findit-otp-demo-helper');
            if (!helper) {
                helper = document.createElement('div');
                helper.id = 'findit-otp-demo-helper';
                helper.className = 'otp-demo-toast';
                document.body.appendChild(helper);
            }

            helper.innerHTML = `
                <div class="otp-toast-content">
                    <div class="otp-toast-title">
                        <span class="otp-shield-icon">&#128272;</span>
                        <strong>Campus Security OTP Dispatched</strong>
                    </div>
                    <div class="otp-toast-body">
                        Demo Verification Code: <span class="otp-code-highlight">${code}</span>
                    </div>
                    <div class="otp-toast-actions">
                        <button type="button" class="btn btn-sm btn-primary otp-autofill-btn" id="otpToastAutoFillBtn">
                            Auto-Fill Code
                        </button>
                        <button type="button" class="btn btn-sm btn-outline-secondary" onclick="this.closest('#findit-otp-demo-helper').style.display='none'">
                            Dismiss
                        </button>
                    </div>
                </div>
            `;

            helper.style.display = 'block';

            const autoFillBtn = document.getElementById('otpToastAutoFillBtn');
            if (autoFillBtn) {
                autoFillBtn.onclick = () => {
                    const targetInput = typeof autoFillInputEl === 'string' ? document.getElementById(autoFillInputEl) : autoFillInputEl;
                    if (targetInput) {
                        targetInput.value = code;
                        targetInput.dispatchEvent(new Event('input', { bubbles: true }));
                        targetInput.focus();
                    }
                    helper.style.display = 'none';
                };
            }
        },

        /**
         * Countdown timer on button
         */
        startCountdown: function(buttonEl, seconds, originalText) {
            if (!buttonEl) return;
            const timerId = buttonEl.id || 'otp-btn';
            if (this.timers[timerId]) {
                clearInterval(this.timers[timerId]);
            }

            let remaining = seconds;
            buttonEl.disabled = true;

            const updateText = () => {
                const mins = Math.floor(remaining / 60);
                const secs = remaining % 60;
                const formatted = `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
                buttonEl.innerHTML = `Resend in ${formatted}`;
            };

            updateText();

            this.timers[timerId] = setInterval(() => {
                remaining--;
                if (remaining <= 0) {
                    clearInterval(this.timers[timerId]);
                    buttonEl.disabled = false;
                    buttonEl.innerHTML = originalText || 'Resend OTP';
                } else {
                    updateText();
                }
            }, 1000);
        },

        /**
         * Displays notification status in dedicated status container
         */
        showStatus: function(el, message, type) {
            const targetEl = typeof el === 'string' ? document.getElementById(el) : el;
            if (!targetEl) return;

            targetEl.innerHTML = message;
            targetEl.className = `otp-status-message otp-status-${type}`;
            targetEl.style.display = 'block';
        }
    };

    // Auto-initialize login tab switcher if present on page
    document.addEventListener('DOMContentLoaded', function() {
        const passwordTabBtn = document.getElementById('tab-password-login');
        const otpTabBtn = document.getElementById('tab-otp-login');
        const passwordSection = document.getElementById('password-login-section');
        const otpSection = document.getElementById('otp-login-section');

        if (passwordTabBtn && otpTabBtn && passwordSection && otpSection) {
            passwordTabBtn.addEventListener('click', function() {
                passwordTabBtn.classList.add('active');
                otpTabBtn.classList.remove('active');
                passwordSection.style.display = 'block';
                otpSection.style.display = 'none';
            });

            otpTabBtn.addEventListener('click', function() {
                otpTabBtn.classList.add('active');
                passwordTabBtn.classList.remove('active');
                passwordSection.style.display = 'none';
                otpSection.style.display = 'block';
            });
        }
    });
})();
