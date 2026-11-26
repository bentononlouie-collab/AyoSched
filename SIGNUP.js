function showSignupLoading() {
    document.getElementById("loginPopup").classList.add("show");

    document.getElementById("loginAnimation").innerHTML =
        '<div class="login-spinner"></div>';

    document.getElementById("loginTitle").textContent = "Creating Account";
    document.getElementById("loginMessage").textContent = "Please wait...";
}

function showSignupSuccess(message) {
    document.getElementById("loginAnimation").innerHTML =
        '<div class="login-icon login-success">✓</div>';

    document.getElementById("loginTitle").textContent = "Account Created!";
    document.getElementById("loginMessage").textContent = message;
}

function showSignupError(message) {
    document.getElementById("loginPopup").classList.add("show");

    document.getElementById("loginAnimation").innerHTML =
        '<div class="login-icon login-error">✕</div>';

    document.getElementById("loginTitle").textContent = "Signup Failed";
    document.getElementById("loginMessage").textContent = message;
}

document.getElementById("signupForm").addEventListener("submit", async function(event) {
    event.preventDefault();

    const firstName = document.getElementById("firstName").value.trim();
    const lastName = document.getElementById("lastName").value.trim();
    const email = document.getElementById("email").value.trim();
    const password = document.getElementById("password").value;
    const confirmPassword = document.getElementById("confirmPassword").value;
    const terms = document.getElementById("terms").checked;

    if (!firstName || !lastName || !email || !password || !confirmPassword) {
        showSignupError("Please complete all fields.");
        return;
    }

    if (password !== confirmPassword) {
        showSignupError("Passwords do not match.");
        return;
    }

    if (password.length < 8) {
        showSignupError("Password must be at least 8 characters.");
        return;
    }

    if (!terms) {
        showSignupError("Please agree to the Terms and Conditions.");
        return;
    }

    const fullName = firstName + " " + lastName;

    try {
        showSignupLoading();

        const response = await fetch("http://localhost:8080/api/student/signup", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                fullName: fullName,
                email: email,
                password: password
            })
        });

        const result = await response.json();

        if (response.ok) {
            showSignupSuccess(result.message || "Your account was created successfully!");

            setTimeout(() => {
                window.location.href = "LOGIN.html";
            }, 1500);

        } else {
            showSignupError(result.message || "Unable to create account.");
        }

    } catch (error) {
        console.error("Signup Error: ", error);
        showSignupError("Cannot connect to the server.");
    }
});