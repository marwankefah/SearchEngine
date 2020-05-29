const selectList = document.getElementById("countriesSelectList");
const savedCC = localStorage.getItem("countryCode");
for(const cc in COUNTRY_CODES) {
    const option = document.createElement("option");
    option.value = cc;
    if(cc === savedCC){
        option.selected = true;
    }
    option.innerText = COUNTRY_CODES[cc];
    selectList.appendChild(option);
}

window.addEventListener("doneLoadingCC", () => {
    debugger;
    const savedCC = localStorage.getItem("countryCode");
    (document.querySelector(`option[value="${savedCC}"]`) || {})
        .selected = true;
});

selectList.addEventListener("change", function () {
    const savedCC = localStorage.getItem("countryCode");
    if(savedCC == this.value) return;
    localStorage.setItem("countryCode", this.value);
    updateTrends().then(r => r);
});
updateTrends().then(r => r);


async function updateTrends() {
    if(!window.location.pathname.endsWith("trends.html")) return;
    (document.getElementById('container') || {remove: () => {}}).remove();
    document.body.classList.add("loading");
    const resultList = document.getElementById("resultsList");
    resultList.style.display = "initial";
    const cc = localStorage.getItem("countryCode");
    debugger;
    const trends = await fetch(`http://localhost:8080/trends?cc=${cc}`)
        .then(resp => resp.json())
        .catch(() => []);
    document.body.classList.remove("loading");
    const container = document.createElement("div");
    container.setAttribute("id", 'container');
    document.body.appendChild(container);
    const x = trends.map(trend => trend.value);
    const y = trends.map(trend => trend.count);
    const data = [
        {
            x,
            y,
            type: 'bar'
        }
    ];
    Plotly.newPlot(container, data);
    resultList.style.display = "none";
}
