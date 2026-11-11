
function showPage(PAGEid){
    const pages = document.querySelectorAll(".PAGE");
        pages.forEach(PAGE => {
            PAGE.classList.remove("active");
        });
    document.getElementById(PAGEid).classList.add("active");
}

showPage("Dashboard");