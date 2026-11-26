const loginPopup = document.getElementById("loginPopup");
const loginAnimation = document.getElementById("loginAnimation");
const loginTitle = document.getElementById("loginTitle");
const loginMessage = document.getElementById("loginMessage");

function showLoginLoading() {
    loginPopup.classList.add("show");

    loginAnimation.innerHTML = `
        <div class="login-spinner"></div>
    `;

    loginTitle.textContent = "Checking Account";
    loginMessage.textContent = "Please wait...";
}

function showLoginSuccess() {
    loginAnimation.innerHTML = `
        <div class="login-icon login-success">✓</div>
    `;

    loginTitle.textContent = "Login Successful!";
    loginMessage.textContent = "Welcome to AyoSched.";
}

function showLoginError(message) {
    loginAnimation.innerHTML = `
        <div class="login-icon login-error">✕</div>
    `;

    loginTitle.textContent = "Login Failed";
    loginMessage.textContent = message;
}

document.getElementById("loginForm").addEventListener("submit", async function (event) {
    event.preventDefault();

    const email = document.getElementById("email").value;
    const password = document.getElementById("password").value;

    showLoginLoading();

    try {
        const response = await fetch("http://127.0.0.1:8080/api/admin/login", {
            method: "POST",
            credentials: "include",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                email: email,
                password: password
            })
        });

        const result = await response.json();

        if (response.ok) {

            localStorage.setItem("firstName", result.firstName);
            localStorage.setItem("role", result.role);

            if (result.role === "student") {
                localStorage.setItem("studentId", result.studentId);
                localStorage.setItem("fullName", result.fullName);
                localStorage.setItem("email", result.email);
            }

            showLoginSuccess();

            setTimeout(() => {
                window.location.href = "DASHBOARD.html";
            }, 1000);

        } else {
            showLoginError(result.message || "Incorrect email or password.");

            setTimeout(() => {
                loginPopup.classList.remove("show");
            }, 1800);
        }

    } catch (error) {
        console.error("Login Error:", error);

        showLoginError("Unable to connect to the server.");

        setTimeout(() => {
            loginPopup.classList.remove("show");
        }, 1800);
    }
});