const iconBox = document.querySelector('.Icon');
const menu = document.getElementById('DropdownMenu');
const logoutBtn = document.getElementById('LogoutBtn');
const card = iconBox.querySelector('.Settings-card');
const settingsBtn = document.getElementById('SettingsBtn');
    function closeAll() {
        menu.classList.remove('open');
        card.classList.remove('open');
        iconBox.classList.remove('open');
    }

    iconBox.addEventListener('click', (e) => {
        e.stopPropagation();
            if (card.classList.contains('open')) { closeAll(); return; }
                menu.classList.toggle('open');
                iconBox.classList.toggle('open');
            });

    settingsBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        menu.classList.remove('open');
        card.classList.add('open');
    });

    card.addEventListener('click', (e) => e.stopPropagation());

    logoutBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        localStorage.clear();
        window.location.href = 'LOGIN.html';
    });

    document.addEventListener('click', closeAll);

document.getElementById('BackBtn').addEventListener('click', () => {
    card.classList.remove('open');
    menu.classList.add('open');
});

const avatarBtn = document.getElementById('AvatarBtn');
const avatarInput = document.getElementById('AvatarInput');

avatarBtn.addEventListener('click', () => avatarInput.click());

function fixPhotoSrc(src) {
    const m = src.match(/^data:([^;]+);base64,(.+)$/);
    if (!m) return src;
    let text;
    try { text = atob(m[2]); } catch (e) { return src; }
    if (!text.startsWith("\\x")) return src;
    const hex = text.slice(2);
    const bytes = new Uint8Array(hex.length / 2);
    for (let i = 0; i < bytes.length; i++) {
        bytes[i] = parseInt(hex.substr(i * 2, 2), 16);
    }
    return URL.createObjectURL(new Blob([bytes], { type: m[1] }));
}

function showPhoto(src) {
    src = fixPhotoSrc(src);
    avatarBtn.querySelector('img')?.remove();
    const img = document.createElement('img');
    img.src = src;
    avatarBtn.prepend(img);

    const headerProfile = document.getElementById('ProfileBtn');
    headerProfile.querySelector('img')?.remove();
    const headerImg = document.createElement('img');
    headerImg.src = src;
    headerImg.style.cssText =
        'position:absolute;inset:0;width:100%;height:100%;object-fit:cover;';
    headerProfile.prepend(headerImg);
}

avatarInput.addEventListener('change', () => {
    const file = avatarInput.files[0];
    if (!file) return;
    if (!file.type.startsWith('image/')) return showMsg('Please choose an image file.');
    if (file.size > 2 * 1024 * 1024) return showMsg('Please choose an image under 2 MB.');

    const reader = new FileReader();
    reader.onload = async () => {
        try {
            const res = await fetch(`${API}/photo`, {
                method: 'PUT',
                credentials: 'include',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ photo: reader.result, photoType: file.type })
            });
            const result = await res.json();
            if (!res.ok) return showMsg(result.message || 'Unable to save picture.');
            showPhoto(reader.result);
            showMsg('Profile picture saved!', true);
        } catch (err) {
            showMsg('Unable to connect to the server.');
        }
    };
    reader.readAsDataURL(file);
});

const API = 'http://127.0.0.1:8080/api/admin';

const adminName = localStorage.getItem('fullName') || localStorage.getItem('full_name') || 'Admin';
const adminRole = localStorage.getItem('role') || 'Admin';

document.getElementById('Name').textContent = adminName;
document.getElementById('Role').textContent = adminRole;
document.getElementById('CardName').textContent = adminName;
document.getElementById('CardRole').textContent = adminRole;
document.getElementById('FullName').value = adminName;
document.getElementById('Username').value = localStorage.getItem('email') || '';

const msg = document.getElementById('SettingsMsg');

function showMsg(text, ok = false) {
    msg.textContent = text;
    msg.classList.toggle('ok', ok);
}

document.getElementById('CancelBtn').addEventListener('click', () => {
    card.classList.remove('open');
    menu.classList.add('open');
    showMsg('');
});

document.getElementById('SaveBtn').addEventListener('click', async () => {
    const fullName = document.getElementById('FullName').value.trim();
    const username = document.getElementById('Username').value.trim();
    const currentPassword = document.getElementById('CurrentPassword').value;
    const newPassword = document.getElementById('NewPassword').value;
    const confirmPassword = document.getElementById('ConfirmPassword').value;

    if (!fullName || !username) return showMsg('Full name and username are required.');

    if (newPassword || confirmPassword || currentPassword) {
        if (!currentPassword || !newPassword || !confirmPassword)
            return showMsg('Fill in all password fields to change it.');
        if (newPassword.length < 8 )
            return showMsg('New password must be at least 8 characters.');
        if (newPassword !== confirmPassword)
            return showMsg('New passwords do not match.');
        if (newPassword === currentPassword)
            return showMsg('New password must be different from the current one.');
    }

    try {
        let res = await fetch(`${API}/profile`, {
            method: 'PUT',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ fullName, username })
        });
        let result = await res.json();
        if (!res.ok) return showMsg(result.message || 'Unable to update profile.');

        if (newPassword) {
            res = await fetch(`${API}/password`, {
                method: 'PUT',
                credentials: 'include',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ currentPassword, newPassword })
            });
            result = await res.json();
            if (!res.ok) return showMsg(result.message || 'Unable to update password.');
        }

        localStorage.setItem('fullName', fullName);
        localStorage.setItem('username', username);
        document.getElementById('Name').textContent = fullName;
        document.getElementById('CardName').textContent = fullName;

        ['CurrentPassword', 'NewPassword', 'ConfirmPassword']
            .forEach(id => document.getElementById(id).value = '');
        showMsg('Saved!', true);
    } catch (err) {
        console.error('Profile update error:', err);
        showMsg('Unable to connect to the server.');
    }
});

(async function loadPhoto() {
    try{
        const res = await fetch(`${API}/photo`, {
            credentials: 'include'
        });
        const data = await res.json();
        if(res.ok && data.photo) showPhoto(data.photo);
    }catch (err){
        console.error('Loading profile photo failed:', err);
    }
})();

//Priority
const priorityAPI = 'http://127.0.0.1:8080/api/admin/priorities';

const scheduleTypeInput = document.getElementById('scheduleType');
const priorityLevelInput = document.getElementById('priorityLevel');
const priorityCategoryInput = document.getElementById('priorityCategory');
const savePriorityBtn = document.getElementById('prioritySaveBtn');
const cancelPriorityBtn = document.getElementById('priorityCancelBtn');
const priorityList = document.getElementById('priorityList');
let editingPriorityId = null;

async function loadPriorities() {
    try {
        const response = await fetch(priorityAPI);
        if (!response.ok) throw new Error('Unable to load priorities.');

        const priorities = await response.json();

        priorityList.innerHTML = '';

        if (priorities.length === 0) {
            priorityList.textContent = 'No priorities assigned yet.';
            return;
        }

        priorities.forEach(priority => {
            const item = document.createElement('div');
            item.className = 'priority-item';

            const name = document.createElement('span');
            name.textContent = `${priority.category === 'educational' ? 'Academic' : 'Personal'} - ${priority.schedule_type}`;

            const level = document.createElement('span');
            level.textContent = priority.priority_level;

            const editBtn = document.createElement('button');
editBtn.textContent = 'Edit';

editBtn.addEventListener('click', () => {
    editPriority(priority);
});

const deleteBtn = document.createElement('button');
deleteBtn.textContent = 'Delete';

deleteBtn.addEventListener('click', () => {
    deletePriority(priority.id);
});

item.append(name, level, editBtn, deleteBtn);

            item.append(name, level, deleteBtn);
            priorityList.appendChild(item);
        });

    } catch (error) {
        console.error('Priority loading error:', error);
    }
}

savePriorityBtn.addEventListener('click', async () => {
    const category = priorityCategoryInput.value;
    const scheduleType = scheduleTypeInput.value.trim();
    const priorityLevel = priorityLevelInput.value.trim();

    if (!scheduleType || !priorityLevel) {
        alert('Please complete all fields.');
        return;
    }

    try {
        const isEditing = editingPriorityId !== null;

        const response = await fetch(
            isEditing
                ? `${priorityAPI}/${editingPriorityId}`
                : priorityAPI,
            {
                method: isEditing ? 'PUT' : 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    category: category,
                    scheduleType: scheduleType,
                    priorityLevel: priorityLevel
                })
            }
        );

        const result = await response.json();

        if (!response.ok) {
            alert(result.message || 'Unable to save priority.');
            return;
        }

        alert(result.message);

        editingPriorityId = null;
        scheduleTypeInput.value = '';
        priorityLevelInput.value = '';
        priorityCategoryInput.value = 'educational';
        savePriorityBtn.textContent = 'Add Priority';

        loadPriorities();

    } catch (error) {
        console.error('Priority save error:', error);
        alert('Unable to connect to the server.');
    }
});

cancelPriorityBtn.addEventListener('click', () => {
    editingPriorityId = null;

    scheduleTypeInput.value = '';
    priorityLevelInput.value = '';
    priorityCategoryInput.value = 'educational';

    savePriorityBtn.textContent = 'Add Priority';
});

function editPriority(priority) {
    editingPriorityId = priority.id;

    priorityCategoryInput.value = priority.category;
    scheduleTypeInput.value = priority.schedule_type;
    priorityLevelInput.value = priority.priority_level;

    savePriorityBtn.textContent = 'Save Changes';
}

async function deletePriority(id) {
    if (!confirm('Are you sure you want to delete this priority?')) return;

    try {
        const response = await fetch(`${priorityAPI}/${id}`, {
            method: 'DELETE'
        });

        if (!response.ok) {
            alert('Unable to delete priority.');
            return;
        }

        loadPriorities();

    } catch (error) {
        console.error('Priority deletion error:', error);
        alert('Unable to connect to the server.');
    }
}

loadPriorities();

// --- Category Management ---
const categoryAPI = 'http://127.0.0.1:8080/api/admin/categories';

const catNameInput = document.getElementById('catName');
const catTypeInput = document.getElementById('catType');
const catColorInput = document.getElementById('catColor');
const catSaveBtn = document.getElementById('catSaveBtn');
const catCancelBtn = document.getElementById('catCancelBtn');
const categoryList = document.getElementById('categoryList');

async function loadCategories(){
    try{
        const response = await fetch(categoryAPI, { credentials: 'include' });
        if(!response.ok) throw new Error('Unable to load categories.');
        const categories = await response.json();
        categoryList.innerHTML = '';
        if(categories.length === 0){
            categoryList.textContent = 'No categories yet.';
            return;
        }
        categories.forEach(cat => {
            const item = document.createElement('div');
            item.className = 'category-item';
            item.innerHTML = `
                <span><span class="cat-color" style="background:${cat.color}"></span>${cat.name} <small>(${cat.type})</small></span>
                <button onclick="deleteCategory(${cat.id})">Delete</button>
            `;
            categoryList.appendChild(item);
        });
    }catch(error){
        console.error('Category load error:', error);
    }
}

window.deleteCategory = async function(id){
    if(!confirm('Delete this category?')) return;
    try{
        const response = await fetch(`${categoryAPI}/${id}`, { method: 'DELETE', credentials: 'include' });
        if(response.ok) loadCategories();
        else alert('Unable to delete category.');
    }catch(e){ alert('Unable to connect to server.'); }
};

catSaveBtn.addEventListener('click', async () => {
    const name = catNameInput.value.trim();
    const type = catTypeInput.value;
    const color = catColorInput.value;
    if(!name){ alert('Please enter category name.'); return; }
    try{
        const response = await fetch(categoryAPI, {
            method: 'POST',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, type, color })
        });
        const result = await response.json();
        if(!response.ok){ alert(result.message || 'Unable to save category.'); return; }
        alert(result.message);
        catNameInput.value = '';
        loadCategories();
    }catch(e){ alert('Unable to connect to server.'); }
});

catCancelBtn.addEventListener('click', () => {
    catNameInput.value = '';
});

// --- Schedule Time Management ---
const scheduleTimeAPI = 'http://127.0.0.1:8080/api/admin/schedule-times';

const stCategoryInput = document.getElementById('stCategory');
const stLabelInput = document.getElementById('stLabel');
const stStartTimeInput = document.getElementById('stStartTime');
const stEndTimeInput = document.getElementById('stEndTime');
const stSaveBtn = document.getElementById('stSaveBtn');
const stCancelBtn = document.getElementById('stCancelBtn');
const scheduleTimeList = document.getElementById('scheduleTimeList');

async function loadScheduleTimes(){
    try{
        const response = await fetch(scheduleTimeAPI, { credentials: 'include' });
        if(!response.ok) throw new Error('Unable to load schedule times.');
        const times = await response.json();
        scheduleTimeList.innerHTML = '';
        if(times.length === 0){
            scheduleTimeList.textContent = 'No schedule times yet.';
            return;
        }
        times.forEach(st => {
            const item = document.createElement('div');
            item.className = 'schedule-time-item';
            item.innerHTML = `
                <span><strong>${st.label}</strong> (${st.category}) • ${st.start_time} - ${st.end_time}</span>
                <button onclick="deleteScheduleTime(${st.id})">Delete</button>
            `;
            scheduleTimeList.appendChild(item);
        });
    }catch(error){
        console.error('Schedule time load error:', error);
    }
}

window.deleteScheduleTime = async function(id){
    if(!confirm('Delete this schedule time?')) return;
    try{
        const response = await fetch(`${scheduleTimeAPI}/${id}`, { method: 'DELETE', credentials: 'include' });
        if(response.ok) loadScheduleTimes();
        else alert('Unable to delete schedule time.');
    }catch(e){ alert('Unable to connect to server.'); }
};

stSaveBtn.addEventListener('click', async () => {
    const category = stCategoryInput.value;
    const label = stLabelInput.value.trim();
    const startTime = stStartTimeInput.value;
    const endTime = stEndTimeInput.value;
    if(!label || !startTime || !endTime){ alert('Please complete all fields.'); return; }
    try{
        const response = await fetch(scheduleTimeAPI, {
            method: 'POST',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ category, label, startTime, endTime })
        });
        const result = await response.json();
        if(!response.ok){ alert(result.message || 'Unable to save schedule time.'); return; }
        alert(result.message);
        stLabelInput.value = '';
        stStartTimeInput.value = '';
        stEndTimeInput.value = '';
        loadScheduleTimes();
    }catch(e){ alert('Unable to connect to server.'); }
});

stCancelBtn.addEventListener('click', () => {
    stLabelInput.value = '';
    stStartTimeInput.value = '';
    stEndTimeInput.value = '';
});

loadScheduleTimes();