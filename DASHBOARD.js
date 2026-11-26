document.getElementById("scheduleForm").addEventListener("submit", async function(event) {
    event.preventDefault();

    const studentId = localStorage.getItem("studentId");
    const subjectName = document.getElementById("subjectName").value.trim();
    const instructor = document.getElementById("instructor").value.trim();
    const location = document.getElementById("location").value.trim();
    const day = document.getElementById("day").value;
    const scheduleType = document.getElementById("scheduleType").value;
    const startTime = document.getElementById("startTime").value;
    const endTime = document.getElementById("endTime").value;
    const notes = document.getElementById("notes").value.trim();

        if(!studentId){
            alert("Please log in first.");
            return;
        }

        if(timeToMinutes(startTime) >= timeToMinutes(endTime)){
            alert("End time must be later than start time.");
            return;
        }

        try{
            const response = await fetch("http://localhost:8080/api/student/schedules", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                }, body: JSON.stringify({
                    studentId: studentId,
                    subjectName: subjectName,
                    instructor: instructor,
                    location: location,
                    day: day,
                    scheduleType: scheduleType,
                    startTime: startTime,
                    endTime: endTime,
                    notes: notes
                })
            });

            const result = await response.json();
                if(response.ok){
                    alert(result.message);
                    document.getElementById("scheduleForm").reset();
                    checkScheduleConflicts();
                }else{
                    alert(result.message);
                }
        }catch(error){
            console.error("Schedule Error: ", error);
            alert("Cannot connect to the server.");
        }
});

async function checkScheduleConflicts(){
    const studentId = localStorage.getItem("studentId");

        try{
            const response = await fetch (`http://localhost:8080/api/student/schedules/conflicts?studentId=${studentId}`);
                if(!response.ok){
                    alert("Unable to check schedule conflicts");
                    return;
                }
            const data = await response.json();
            const conflicts = data.conflicts || [];
            const recommendedScheduleIds = data.recommendedScheduleIds || [];
            console.log("Conflict response:", conflicts);

                if(conflicts.length === 0){
                    alert("No schedule conflicts found!");
                    return;
                }

                if(!studentId){
                    alert("Student ID not found. Please log in again.");
                    return;
                }

                await loadStudentSchedules();
                const newest = allSchedules.reduce((a, b) => (a.id > b.id ? a : b));
                const recommendedSet = new Set(recommendedScheduleIds.map(Number));
                const recommendedConflict = conflicts.find(pair => recommendedSet.has(Number(pair.firstSchedule.id)) || recommendedSet.has(Number(pair.secondSchedule.id)));
                const selectedConflict = recommendedConflict || conflicts.find(pair => Number(pair.firstSchedule.id) === Number(newest.id) || Number(pair.secondSchedule.id) === Number(newest.id)) || conflicts[0];
                const recommendedId = recommendedConflict? (
                    recommendedSet.has(Number(recommendedConflict.firstSchedule.id))
                    ? recommendedConflict.firstSchedule.id
                    : recommendedConflict.secondSchedule.id
                ) : newest.id;
                showConflictCard(selectedConflict, recommendedId)
        }catch(error){
            console.error("Conflict Check Error: ", error);
            alert("Unable to connect to the conflict checker.");
        }
}

let allSchedules = [];

async function loadStudentSchedules() {
    const studentId = localStorage.getItem("studentId");

        if(!studentId){
            console.error("Student ID not found. ");
            return;
        }

        try{
            const response = await fetch ("http://localhost:8080/api/student/schedules?studentId=" + studentId);

                if(!response.ok){
                    throw new Error("Failed to retrieve schedules.");
                }

            allSchedules = await response.json();
            updateStats();
            displaySchedules();
        }catch(error){
            console.error("Schedule Loading Error.", error);
        }
}

function formatTime(t){
    
    if(!t) return "";
    let [h, m] = t.split(":");
    h = parseInt(h);

    const ampm = h >= 12 ? "PM" : "AM";
    h = h % 12 || 12;
    return `${h}:${m} ${ampm}`;
}

function priorityInfo(p){
    if (p >= 5) return {label: "High", cls: "p-high"};
    if (p === 4) return {label : "Medium", cls:"p-medium"};
    if (p === 3) return {label : "Low", cls:"p-low"};
    return {label: "Minimal", cls: "p-min"};
}

function displaySchedules(selectedDay = "all"){
    const scheduleDisplay = document.getElementById("scheduleDisplay");
    let filteredSchedules = allSchedules;

        if(selectedDay !== "all"){
            filteredSchedules = allSchedules.filter(schedule => schedule.day.toLowerCase() === selectedDay.toLowerCase());
        }

        if(filteredSchedules.length === 0){
            scheduleDisplay.innerHTML = `<div class = "EMPTY" id = "scheduleDisplay">
            <i class = "fa-regular fa-calendar-check"></i>
            <h2 class = "NOTES"> No Classes Schedule Yet </h2>
            <p class = "NOTES1"> Add a schedule to see your weekly timetable here. </p>
        </div>`;
            return;
        }

        const weekdayOrder = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"];

    const groupedSchedules = {};

    filteredSchedules.forEach(schedule => {
        const matchedDay = weekdayOrder.find(day =>
            day.toLowerCase() === schedule.day.toLowerCase()
        );

        if (!matchedDay) return;

        if (!groupedSchedules[matchedDay]) {
            groupedSchedules[matchedDay] = [];
        }

        groupedSchedules[matchedDay].push(schedule);
    });

    // GET THE MONDAY OF THIS WEEK
    const currentDate = new Date();
    const weekMonday = new Date(currentDate);

    weekMonday.setDate(currentDate.getDate() - ((currentDate.getDay() + 6) % 7));

    // GET DATE FOR EACH DAY
    function getDateForDay(day) {
        const index = weekdayOrder.indexOf(day);
        const date = new Date(weekMonday);

        date.setDate(weekMonday.getDate() + index);

        return date.toLocaleDateString("en-US", {
            month: "short",
            day: "numeric"
        });
    }

    // RENDER GROUPED SCHEDULES
    scheduleDisplay.innerHTML = weekdayOrder
        .filter(day => groupedSchedules[day])
        .map(day => {

            const dayClasses = groupedSchedules[day];

            return `
                <div class="schedule-day-row">

                    <div class="schedule-day-label">
                        <strong>${day.substring(0, 3)}</strong>
                        <span>${getDateForDay(day)}</span>
                    </div>

                    <div class="schedule-day-indicator"></div>

                    <div class="schedule-day-classes">

                        ${dayClasses.map(schedule => `
                            <div class="schedule-class ${schedule.mutedToday ? 'muted' : ''}">

                                ${schedule.mutedToday ? `
                                    <p class="muted-label">Muted Today</p>
                                ` : ''}

                                <h3>${schedule.subjectName ?? ""}</h3>

                                <p>
                                    ${formatTime(schedule.startTime)}
                                    - ${formatTime(schedule.endTime)},
                                    ${schedule.location ?? ""}
                                </p>

                                <p>${schedule.instructor ?? ""}</p>

                                <div class="schedule-class-actions">

                                    <span class="priority-badge ${priorityInfo(schedule.priority).cls}">
                                        ${priorityInfo(schedule.priority).label}
                                        (${schedule.priority ?? 1})
                                    </span>

                                    ${schedule.day.toLowerCase() === currentDate.toLocaleDateString("en-US", {
                                        weekday: "long"
                                    }).toLowerCase() && !schedule.mutedToday ? `
                                        <button class="mute-btn" type="button"
                                            onclick="muteSchedule(${schedule.id})">
                                            <i class="fa-solid fa-bell-slash"></i>
                                        </button>
                                    ` : ''}

                                    <button class="delete-btn" type="button"
                                        onclick="deleteSchedule(${schedule.id})">
                                        <i class="fa-solid fa-trash"></i>
                                        Delete
                                    </button>

                                </div>
                            </div>
                        `).join("")}

                    </div>
                </div>
            `;
        }).join("");
        
}

const days = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"];
    days.forEach(day => {
        const button = document.getElementById(day.toLowerCase() + "btn");

            if(button){
                button.addEventListener("click", () => {
                    displaySchedules(day);
                });
            }
    });

loadStudentSchedules();
setInterval(updateStats, 60000);

function updateStats() {
    const total = new Set(allSchedules.map(s => (s.subjectName || "").trim().toLowerCase()).filter(Boolean)).size;
    const el = document.getElementById("totalSubjects");

        if(el){
            el.textContent = total;
        }

    const dayNames = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"];
    const todayName = dayNames[new Date().getDay()];
    const todayCount = allSchedules.filter(s => (s.day || "").toLowerCase() === todayName.toLowerCase()).length;
    const todayEl = document.getElementById("classesToday");

        if(todayEl){
            todayEl.textContent = todayCount;
        }

    const now = new Date();
    const nowMinutes = now.getHours() * 60 + now.getMinutes();
    const upcoming = allSchedules
        .filter(s => (s.day || "").toLowerCase() === todayName.toLowerCase())
        .filter(s => toMinutes(s.startTime) > nowMinutes)
        .sort((a, b) => toMinutes(a.startTime) - toMinutes(b.startTime));
    const nextEl = document.getElementById("nextClass");
        if(nextEl){
            nextEl.textContent = upcoming.length ? formatTime(upcoming[0].startTime) : "None";
        }
    const roomEl = document.getElementById("nextRoom");
        if(roomEl){
            roomEl.textContent = upcoming.length ? (upcoming[0].location || "--") : "--";
        }
}

function toMinutes(t){
    const [h, m] = t.split(":");
    return parseInt(h) * 60 + parseInt(m);
}

function closeConflictCard(){
    document.getElementById("conflictOverlay")?.remove();
}

async function moveSchedule(id, slot){
    const res = await fetch(`http://localhost:8080/api/student/schedules/${id}/time`, {
        method: "PUT",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify({startTime: slot.start, endTime: slot.end})
    });

    closeConflictCard();
    if(res.ok) loadStudentSchedules();
    else alert ("Unable to move the schedule.");
}

async function showConflictCard(pair, recommendedId){
    const studentId = localStorage.getItem("studentId");
    const s = Number(pair.firstSchedule.id) === Number(recommendedId) ? pair.firstSchedule : pair.secondSchedule;
    const other = Number(pair.firstSchedule.id) === Number(recommendedId) ? pair.secondSchedule : pair.firstSchedule;
    const minutes = toMinutes(s.endTime) - toMinutes(s.startTime);
    const res = await fetch(`http://localhost:8080/api/student/schedules/free-slots?studentId=${studentId}&day=${s.day}&minutes=${minutes}&excludeId=${s.id}`);
    const slots = await res.json();
    closeConflictCard();
    const overlay = document.createElement("div");
    overlay.id = "conflictOverlay";
    overlay.innerHTML = `<div class = "conflict-card"> <h3> Schedule Conflict </h3> <p> Recommended to reconsider: <b>${s.subjectName}</b></p> <p><b>${s.subjectName}</b> overlaps <b>${other.subjectName}</b> (${formatTime(other.startTime)} - ${formatTime(other.endTime)}).</p> <p> Available time slots:</p> <div id="slotList"></div><button id="conflictKeep"> Keep as is</button> </div>`;
    document.body.appendChild(overlay);
    const list = overlay.querySelector("#slotList");
    if(!slots.length) list.textContent = "No free slot of that length on this day.";
    slots.forEach(slot => {
        const btn = document.createElement("button");
        btn.className = "slot-btn";
        btn.textContent = `${formatTime(slot.start)} - ${formatTime(slot.end)}`;
        btn.addEventListener("click", () => moveSchedule(s.id, slot));
        list.appendChild(btn);
    });
    overlay.querySelector("#conflictKeep").addEventListener("click", closeConflictCard);
}

function timeToMinutes(t){
    const m = t.trim().toUpperCase().match(/^(\d{1,2}):(\d{2})(?::\d{2})?\s*(AM|PM)?$/);
        if(m) return NaN;
        let h = parseInt(m[1]);
        if(m[3] === "PM" && h !== 12) h += 12;
        if(m[3] === "AM" && h === 12) h= 0;
        return h * 60 + parseInt(m[2]);
}

const calendarPopup = document.getElementById("calendarPopup");
    console.log("calendar popup");
const calendarMonth = document.getElementById("calendarMonth");
const calendarDates = document.getElementById("calendarDates");

let currentDate = new Date();

function displayCalendar(){
    const year = currentDate.getFullYear();
    const month = currentDate.getMonth();
    const today = new Date();
        calendarMonth.textContent = currentDate.toLocaleString("en-US", {month: "long", year: "numeric"});
    const firstDay = new Date(year, month, 1).getDay();
    const lastDate = new Date(year, month + 1, 0).getDate();
        calendarDates.innerHTML =  "";

        for(let i = 0; i < firstDay; i++){
            calendarDates.innerHTML += "<span></span>";
        }

        for(let day = 1; day <= lastDate; day++){
            const dateElement = document.createElement("span");
                dateElement.textContent = day;

                    if(day === today.getDate() && month === today.getMonth() && year === today.getFullYear()){
                        dateElement.classList.add("today");
                    }

                calendarDates.appendChild(dateElement);
        }
}

document.getElementById("viewCalendarBtn").addEventListener("click", () => {
    console.log("VIEW CALENDAR BUTTON CLICKED");
    if (calendarPopup.style.display === "none"){
        calendarPopup.style.display = "block";
        displayCalendar();
        console.log("Popup display:", calendarPopup.style.display);
        console.log("Calendar content: ", calendarDates.innerHTML);
    }else{
        calendarPopup.style.display = "none";
    }
});

document.getElementById("prevMonth").addEventListener("click", () => {
    currentDate.setMonth(currentDate.getMonth() - 1);
    displayCalendar();
});

document.getElementById("nextMonth").addEventListener("click", () => {
    currentDate.setMonth(currentDate.getMonth() + 1);
    displayCalendar();
});

const profileBtn = document.getElementById("profileBtn");
const profileDropdown = document.getElementById("profileDropdown");

    profileBtn?.addEventListener("click", (e) => {
        e.stopPropagation();
        profileDropdown.classList.toggle("show");
    });

    document.getElementById("settingsBtn").addEventListener("click", (e) => {
        e.stopPropagation();
        profileDropdown.classList.add("settings-open");
            document.getElementById("settingsStudentId").textContent = localStorage.getItem("studentId") || "--";
            document.getElementById("settingsName").textContent = localStorage.getItem("full_name") || "--";
        loadProfilePhoto();
    });

    document.getElementById("backBtn").addEventListener("click", () => {
        profileDropdown.classList.remove("settings-open");
    });

    document.addEventListener("click", () => {
        profileDropdown?.classList.remove("show");
    });

    document.getElementById("logoutBtn")?.addEventListener("click", () => {
        localStorage.clear();
        window.location.href = "LOGIN.html";
    });

async function muteSchedule(scheduleId){
    const studentId = localStorage.getItem("studentId");
        if(!studentId){
            alert("Student ID not found. Please log in again.");
            return;
        }

        try{
            const response = await fetch(`http://localhost:8080/api/student/schedules/${scheduleId}/mute?studentId=${studentId}`, {
                method: "POST"
            });
            const result = await response.json();
                if(!response.ok){
                    alert(result.message || "Unable to mute schedule.");
                    return;
                }
                alert("Schedule muted for today!");
            await loadStudentSchedules();
        }catch(error){
            console.error("Mute Schedule Error: ", error);
            alert("Unable to connect to the server.");
        }
}

async function deleteSchedule(scheduleId) {
    const studentId = localStorage.getItem("studentId");
        if(!confirm("Are you sure you want to delete this schedule?")){
            return;
        }

        try{
            const response = await fetch(`http://localhost:8080/api/student/schedules/${scheduleId}?studentId=${studentId}`,{
                method: "DELETE"
            });
            const result = await response.json();
                if(!response.ok){
                    alert(result.message || "Unable to delete schedule.");
                    return;
                }
            alert("Schedule deleted successfully!");
            await loadStudentSchedules();
        }catch(error){
            console.log("Delete Schedule Error: ", error);
            alert("Unable to connect to the server.");
        }
}

const stName = document.getElementById("stFullName");
const stEmail = document.getElementById("stEmail");
const stAvatar = document.getElementById("stAvatar");
const stDefaultAvatar = stAvatar ? stAvatar.innerHTML : "";
let stSaved = {name: "", email: ""};
    function fillSettingContent(){
        const name = localStorage.getItem("fullName") || "Student";
        document.getElementById("stDisplayName").textContent = name;
        document.getElementById("stStudentId").textContent = localStorage.getItem("studentId") || "--";
        stName.value = name;
        stEmail.value = localStorage.getItem("email") || "";
    }
    fillSettingContent();

    function stSetEditing(on){
        stName.disabled = !on;
        stEmail.disabled = !on;
        document.getElementById("stInfoActions").hidden = !on;
        document.getElementById("stEditBtn").hidden = on;
        if(on) stName.focus();
    }

    document.getElementById("stEditBtn")?.addEventListener("click", () => {
        stSaved = {name: stName.value, email: stEmail.value};
        stSetEditing(true);
    })

    document.getElementById("stCancelInfo")?.addEventListener("click", () => {
        stName.value = stSaved.name;
        stEmail.value = stSaved.email;
        stSetEditing(false);
    });

    document.getElementById("stSaveInfo")?.addEventListener("click", async () => {
    const name = stName.value.trim();
    const email = stEmail.value.trim();

    if (!name) {
        alert("Please enter your full name.");
        return;
    }

    if (!email || !/^\S+@\S+\.\S+$/.test(email)) {
        alert("Please enter a valid email address.");
        return;
    }

    try {
        const response = await fetch("http://127.0.0.1:8080/api/student/profile", {
            method: "PUT",
            credentials: "include",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                fullName: name,
                email: email
            })
        });

        const result = await response.json();

        if (!response.ok) {
            alert(result.message || "Unable to update profile.");
            return;
        }

        // Update displayed information
        stName.value = name;
        stEmail.value = email;
        document.getElementById("stDisplayName").textContent = name;

        // Update local storage
        localStorage.setItem("fullName", name);
        localStorage.setItem("firstName", name.split(/\s+/)[0]);
        localStorage.setItem("email", email);

        // Update saved values for Cancel
        stSaved = {
            name: name,
            email: email
        };

        stSetEditing(false);

        alert("Profile updated successfully!");

    } catch (error) {
        console.error("Profile update error:", error);
        alert("Unable to connect to the server.");
    }
});

    document.getElementById("stPhotoInput")?.addEventListener("change", async (e) => {
        const file = e.target.files[0];
            if (!file) return;

            if (!file.type.startsWith("image/")) {
                alert("Please choose an image file.");
                return;
            }

            if (file.size > 2 * 1024 * 1024) {
                alert("Please choose an image under 2 MB.");
                return;
            }

        const reader = new FileReader();

            reader.onload = async () => {
                try {
                    const response = await fetch("http://127.0.0.1:8080/api/student/photo", {
                        method: "PUT",
                        credentials: "include",
                        headers: {
                            "Content-Type": "application/json"
                        },
                        body: JSON.stringify({
                        photo: reader.result,
                        photoType: file.type
                })
            });

            const result = await response.json();

            if (!response.ok) {
                alert(result.message || "Unable to save profile picture.");
                return;
            }

            stAvatar.innerHTML = `<img src="${reader.result}" alt="Profile picture">`;
            alert("Profile picture saved successfully!");

        } catch (error) {
            console.error("Photo upload error:", error);
            alert("Unable to connect to the server.");
        }
    };

    reader.readAsDataURL(file);
});

    document.getElementById("stRemovePhoto")?.addEventListener("click", () => {
        stAvatar.innerHTML = stDefaultAvatar;
        document.getElementById("stPhotoInput").value = "";
    });

    document.getElementById("stUpdatePw")?.addEventListener("click", async () => {
    const current = document.getElementById("stCurrentPw").value;
    const next = document.getElementById("stNewPw").value;
    const again = document.getElementById("stConfirmPw").value;
    const msg = document.getElementById("stPwMsg");

    const fail = (text) => {
        msg.textContent = text;
        msg.className = "st-msg error";
    };

    if (!current || !next || !again)
        return fail("Please fill in all password fields.");

    if (next.length < 8)
        return fail("New password must be at least 8 characters.");

    if (next !== again)
        return fail("New passwords do not match.");

    if (next === current)
        return fail("New password must be different from the current one.");

    try {
        const response = await fetch("http://127.0.0.1:8080/api/student/password", {
            method: "PUT",
            credentials: "include",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                currentPassword: current,
                newPassword: next
            })
        });

        const result = await response.json();

        if (!response.ok) {
            return fail(result.message || "Unable to update password.");
        }

        msg.textContent = result.message;
        msg.className = "st-msg ok";

        ["stCurrentPw", "stNewPw", "stConfirmPw"].forEach(id => {
            document.getElementById(id).value = "";
        });

    } catch (error) {
        console.error("Password update error:", error);
        fail("Unable to connect to the server.");
    }
});

async function loadProfilePhoto() {
    try {
        const response = await fetch("http://127.0.0.1:8080/api/student/photo", {
            credentials: "include"
        });

        const result = await response.json();

        if (!response.ok) {
            console.error(result.message);
            return;
        }

        if (result.photo) {
            stAvatar.innerHTML = `<img src="${result.photo}" alt="Profile picture">`;
        } else {
            stAvatar.innerHTML = stDefaultAvatar;
        }

    } catch (error) {
        console.error("Loading profile photo failed:", error);
    }
}

const headerIcon = document.getElementById("profileBtn");
const headerIconDefault = headerIcon ? headerIcon.innerHTML : "";
    function fixPhotoSrc(src){
        const m = src.match(/^data:([^;]+);base64,(.+)$/);
        if(!m) return src;
        let text;
        try{text=atob(m[2]); }catch(e) {return src; }
        if(!text.startsWith("\\x")) return src;
        const hex = text.slice(2);
        const bytes = new Uint8Array(hex.length / 2);
        for(let i = 0; i < bytes.length; i++){
            bytes[i] = parseInt(hex.substr(i * 2, 2), 16);
        }
        return URL.createObjectURL(new Blob([bytes], {type: m[1]}));
    }
    function syncHeaderPhoto(){
        if(!headerIcon || !stAvatar) return;
        const img = stAvatar.querySelector("img");
        if(!img){
            headerIcon.innerHTML = headerIconDefault;
            return;
        }
        const apply = () => {headerIcon.innerHTML = `<img src = "${img.src}" alt = "">`;};
        const fail = () => {
            const bad = img.src;
            console.warn("Photo failed to load. src starts with:", img.src.slice(0, 80));
                if(!img.dataset.retried){
                    img.dataset.retried = "1";
                    const fixed = fixPhotoSrc(bad);
                    if(fixed !== bad){
                        img.src = fixed;
                        return;
                    }
                }
            headerIcon.innerHTML = headerIconDefault;
            stAvatar.innerHTML = stDefaultAvatar;
        };
        if(img.complete){
            img.naturalWidth > 0 ? apply() : fail();
        }else{
            img.addEventListener("load", apply, {once: true});
            img.addEventListener("error", fail, {once: true});
        }
    }
    if(stAvatar){
        new MutationObserver(syncHeaderPhoto).observe(stAvatar, {childList: true});
    }
    loadProfilePhoto();

document.addEventListener("load", (e) => {
    const img = e.target;
    if(!img || img.tagName !== "IMG" || !img.closest("#stAvatar")) return;
    if(img.naturalWidth === 0) return;
    const header = document.getElementById("profileBtn");
    if(header) header.innerHTML = `<img src="${img.src}" alt = "">`;
}, true);

document.getElementById("stRemovePhoto")?.addEventListener("click", () => {
    const header = document.getElementById("profileBtn");
    if(header) header.innerHTML = headerIconDefault;
});

const goodName = document.getElementById("goodName");
    if(goodName){
        const first = localStorage.getItem("firstName") || (localStorage.getItem("full_name") || localStorage.getItem("fullName") || "").split(" ")[0];
            goodName.textContent = first || "User";
    }