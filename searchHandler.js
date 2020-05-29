const MAX_CHAR_COUNT = 220;
let searchQuery = "";
const resultList = document.getElementById('resultsList');
const loaderItem = document.querySelector('.loaderItem');

const doneLoadingCCEvent = new Event("doneLoadingCC");

document.getElementById("searchForm").addEventListener("submit", (e) => {
   e.preventDefault();


   [].forEach.call(resultList.querySelectorAll('li'), (li) => {
        if(li.classList.contains('loaderItem')) return;
       li.remove();
   });

   localStorage.removeItem("idx");
   localStorage.removeItem("isLast");
    localStorage.removeItem("imageIdx");
    localStorage.removeItem("isImageLast");

   if(window.isImageSearch){
        searchImage(true);
   }else{
        search(true);
   }


   document.onscroll = () => {
       if(window.innerHeight + window.scrollY >= document.documentElement.scrollHeight - 2){
           if(window.isImageSearch){
               if(localStorage.getItem("isImageLast") === "true"){
                   loaderItem.remove();
                   return;
               }
           }else{
               if(localStorage.getItem("isLast") === "true"){
                   loaderItem.remove();
                   return;
               }
           }
           if(!document.body.classList.contains("loading")){
               if(window.isImageSearch){
                   searchImage();
               }else{
                   search();
               }
           }
       }
   };

   return false;
});
async function searchImage(isNew = false){
    searchQuery = document.getElementById("textContainer").value;
    if(!searchQuery || !searchQuery.trim()) return;
    const cc = localStorage.getItem("countryCode");
    if(!cc){
        window.addEventListener("doneLoadingCC", () => search(isNew));
        return;
    }
    document.body.classList.add('results');
    document.body.classList.add('imageResults');
    document.body.classList.add("loading");

    if(resultList.style.display === "none"){
        resultList.style.display = "initial";
    }

    const idx = localStorage.getItem("imageIdx")
    const link = idx && !isNew ? `http://localhost:8080/search?img=on&q=${searchQuery}&cc=${cc}&idx=${Number(idx)}`
        :
        `http://localhost:8080/search?q=${searchQuery}&img=on&cc=${cc}`;

    const images = await fetch(link)
        .then(resp => resp.json())
        .then(resp => {
            localStorage.setItem("imageIdx", resp.idx.toString());
            localStorage.setItem("isImageLast", resp.isLast.toString());
            if(resp.isLast){
                document.querySelector('.loaderItem').style.display = "none";
            }else{
                document.querySelector('.loaderItem').style.display = "initial";
            }
            return resp.data;
        }).catch(e => {
            return [];
        });

    for(const image of images){
        addImageResult(image);
    }


    document.body.classList.remove("loading");
    document.querySelector("#textContainer").classList.remove("suggestionsVisible");
    initializeSuggestionsList();
}

async function search(isNew = false){

    searchQuery = document.getElementById("textContainer").value;
    if(!searchQuery || !searchQuery.trim()) return;

    document.body.classList.add('results');
    document.body.classList.remove('imageResults');
    document.body.classList.add("loading");

    const cc = localStorage.getItem("countryCode");
    if(!cc){
        window.addEventListener("doneLoadingCC", () => search(isNew));
        return;
    }
    if(resultList.style.display === "none"){
        resultList.style.display = "initial";
    }
    const idx = localStorage.getItem("idx")
    const link = idx && !isNew ? `http://localhost:8080/search?q=${searchQuery}&cc=${cc}&idx=${Number(idx)}`
        :
        `http://localhost:8080/search?q=${searchQuery}&cc=${cc}`;

    const pages = await fetch(link)
        .then(resp => resp.json())
        .then(resp => {
            localStorage.setItem("idx", resp.idx.toString());
            localStorage.setItem("isLast", resp.isLast.toString());
            if(resp.isLast){
                document.querySelector('.loaderItem').style.display = "none";
            }else{
                document.querySelector('.loaderItem').style.display = "initial";
            }
            return resp.data;
        }).catch(e => {
            console.log(e);
            return [];
        });

    for(const page of pages){
        addPageResult(page, searchQuery);
    }

    document.body.classList.remove("loading");
    document.querySelector("#textContainer").classList.remove("suggestionsVisible");
    initializeSuggestionsList();
}

function addImageResult(image) {
    let a = document.createElement('a');
    let li = document.createElement('li');
    let img = document.createElement('img');
    let description = document.createElement('p');
    description.innerText = image.description;

    li.classList.add("image");
    img.src = image.link;
    a.href = image.link;
    a.target = "_blank";
    a.appendChild(img);
    a.appendChild(description);
    li.appendChild(a);
    resultList.insertBefore(li, loaderItem);
}

function addPageResult(page, searchQuery){
    let a = document.createElement('a');
    let li = document.createElement('li');
    let header = document.createElement('h3');
    header.innerText = page.title;

    let description = document.createElement('p');
    description.innerText = getFirstOccurrenceOfAny(page.content,
        searchQuery.split(' ')
            .map(word => word.replace(/"/g, ''))
    );

    a.href = page.link;
    a.target = "_blank";
    a.appendChild(header);
    li.appendChild(a);
    li.appendChild(description);
    resultList.insertBefore(li, loaderItem);
}

function getFirstOccurrenceOfAny(content, words){
    for(const word of words){
        const i = content.indexOf(word);
        if(i !== -1){
            let fullStatement = content.substr(0, content.indexOf(".", content.indexOf('twitter')));
            if(fullStatement.length > MAX_CHAR_COUNT){
                return fullStatement.substr(0, MAX_CHAR_COUNT - 3) + "...";
            }
            return fullStatement;
        }
    }
    return "";
}

window.onbeforeunload = () => {
    localStorage.removeItem("idx");
    localStorage.removeItem("isLast");
};


if(!localStorage.getItem("countryCode")){
    fetch("http://api.ipstack.com/check?access_key=769b825f7812f99310f4d84a07694575")
        .then(resp => resp.json())
        .then(({country_code}) => {
            if(!country_code) throw new Error();
            localStorage.setItem("countryCode", country_code);
            window.dispatchEvent(doneLoadingCCEvent);
        }).catch(err => {
            window.dispatchEvent(doneLoadingCCEvent);
    });
}

document.getElementById("textContainer").addEventListener("keyup", getSuggestions);

async function getSuggestions(){
    const input = this.value;
    if(!input){
        initializeSuggestionsList();
        document.querySelector("#textContainer").classList.remove("suggestionsVisible");
        return;
    }
    const results = await fetch(`http://localhost:8080/suggestion?input=${input}`)
        .then(resp => resp.json())
        .catch(e => []);
    initializeSuggestionsList();
    if(results.length){
        document.querySelector("#textContainer").classList.add("suggestionsVisible");
    }
    for(const result of results){
        addToSuggestionList(result);
    }
}

function initializeSuggestionsList() {
    [].forEach.call(document.querySelectorAll("#suggestionsList *"), (el) => {
        el.remove();
    });
}
function addToSuggestionList(result = ""){
    let li = document.createElement('li');
    li.innerText = result;
    li.onclick = function () {
        initializeSuggestionsList();
        document.getElementById("textContainer").value = this.innerText;
        document.querySelector("#textContainer").classList.remove("suggestionsVisible");
        // document.getElementById("searchBtn").click();
    };
    document.querySelector("#suggestionsList").appendChild(li);
}

document.getElementById("imgSearchBtn").addEventListener("click", () => {
    window.isImageSearch = true;
});
document.getElementById("searchBtn").addEventListener("click", () => {
    window.isImageSearch = false;
});