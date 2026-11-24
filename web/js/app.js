
document.addEventListener('DOMContentLoaded', () => {

    // Password visibility controls.
    document.querySelectorAll('[data-password-toggle]').forEach(button => {
        button.addEventListener('click', () => {
            const input = document.getElementById(
                button.dataset.passwordToggle
            );

            if (!input) {
                return;
            }

            const show = input.type === 'password';

            input.type = show ? 'text' : 'password';
            button.textContent = show ? 'Hide' : 'Show';
            button.setAttribute(
                'aria-label',
                show ? 'Hide password' : 'Show password'
            );
        });
    });

    // Confirmation prompts.
    document.querySelectorAll('[data-confirm]').forEach(button => {
        button.addEventListener('click', event => {
            if (!confirm(button.dataset.confirm)) {
                event.preventDefault();
            }
        });
    });

    // Expand/collapse sections.
    document.querySelectorAll('[data-toggle-target]').forEach(button => {
        button.addEventListener('click', () => {
            const target = document.getElementById(
                button.dataset.toggleTarget
            );

            if (target) {
                target.hidden = !target.hidden;
            }
        });
    });

    // Unlock authentication inputs only when the user focuses them.
    document.querySelectorAll('[data-auth-field]').forEach(input => {
        input.addEventListener('focus', () => {
            input.removeAttribute('readonly');
        }, { once: true });
    });

    // Clear values restored by the browser.
    clearAuthenticationForms();
});

// Browsers can restore form values when a page is reopened from history.
window.addEventListener('pageshow', () => {
    clearAuthenticationForms();
});

function clearAuthenticationForms() {
    document.querySelectorAll('form[data-auth-form]').forEach(form => {
        form.querySelectorAll('[data-auth-field]').forEach(input => {
            input.value = '';
            input.setAttribute('readonly', 'readonly');
        });
    });
}
