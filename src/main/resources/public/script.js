const fileInput = document.getElementById('csvFile');
const topSongsBtn = document.getElementById('topSongsBtn');
const topArtistsBtn = document.getElementById('topArtistsBtn');
const topAlbumsBtn = document.getElementById('topAlbumsBtn');
const topSongsYearBtn = document.getElementById('topSongsYearBtn');
const topSongsYearMonthBtn = document.getElementById('topSongsYearMonthBtn');
const resultsDiv = document.getElementById('results');
const resultsContent = document.getElementById('resultsContent');

const playedSongsBtn = document.getElementById('playedSongsBtn');
const dateInput = document.getElementById('dateInput');

async function fetchData(endpoint, year, month, date) {
    const file = fileInput.files[0];
    if (!file) {
        alert('Please select a CSV file.');
        return;
    }

    const formData = new FormData();
    formData.append('file', file);

    let url = endpoint;
    if (year) {
        url += `/year/${year}`;
    }
    if (month) {
        url += `/month/${month}`;
    }
    if (date) {
        url += `/date/${date}`;
    }

    try {
        const response = await fetch(url, {
            method: 'POST',
            body: formData
        });

        if (response.ok) {
            const data = await response.json();
            console.log(data);
            displayResults(data);
        } else {
            const errorText = await response.text();
            alert(`Error: ${response.status} - ${errorText}`);
        }
    } catch (error) {
        console.error('Error:', error);
        alert('An error occurred while processing the request.');
    }
}

async function fetchPlayedSongs(date) {
    const file = fileInput.files[0];
    if (!file) {
        alert('Please select a CSV file.');
        return;
    }

    const formData = new FormData();
    formData.append('file', file);

    try {
        const response = await fetch(`/analyze/played-songs/date/${date}`, {
            method: 'POST',
            body: formData
        });

        if (response.ok) {
            const data = await response.json();
            console.log(data);
            displayResults(data);
        } else {
            const errorText = await response.text();
            alert(`Error: ${response.status} - ${errorText}`);
        }
    } catch (error) {
        console.error('Error:', error);
        alert('An error occurred while processing the request.');
    }
}

function displayResults(data) {
    resultsContent.innerHTML = ''; // Clear previous results

    if (Array.isArray(data)) {
        data.forEach(trackUri => {
            const trackId = trackUri.split(':')[2]; // Extract track ID
            const iframe = document.createElement('iframe');
            iframe.src = `https://open.spotify.com/embed/track/${trackId}?utm_source=generator&theme=0`;
            iframe.frameBorder = '0';
            iframe.allow = "autoplay; clipboard-write; encrypted-media; fullscreen; picture-in-picture";
            iframe.loading = "lazy"
            iframe.style.borderRadius = '12px'; // Add border-radius
            iframe.style.width = '100%';
            iframe.style.height = '100px';
            iframe.classList.add('iframe-container');
            resultsContent.appendChild(iframe);
        });
    } else {
        // Display other results as JSON
        const pre = document.createElement('pre');
        pre.textContent = JSON.stringify(data, null, 2);
        resultsContent.appendChild(pre);
    }

    resultsDiv.style.display = 'block';
}

function displayPlayedSongs(data) {
    resultsContent.innerHTML = ''; // Clear previous results for the main results container

    if (data.length === 0) {
        const noDataMessage = document.createElement('p');
        noDataMessage.textContent = 'No songs played on this date.';
        resultsContent.appendChild(noDataMessage);
    } else {
        const table = document.createElement('table');
        const thead = table.createTHead();
        const headerRow = thead.insertRow();
        const headers = ["Timestamp", "Duration (Minutes)", "Song Title", "Artist"];

        headers.forEach(headerText => {
            const th = document.createElement("th");
            th.textContent = headerText;
            headerRow.appendChild(th);
        });

        const tbody = table.createTBody();
        data.forEach(entry => {
            const row = tbody.insertRow();
            const tsCell = row.insertCell();
            const msPlayedCell = row.insertCell();
            const titleCell = row.insertCell();
            const artistCell = row.insertCell();

            tsCell.textContent = entry.ts;
            msPlayedCell.textContent = entry.msPlayed;
            titleCell.textContent = entry.trackName;
            artistCell.textContent = entry.artistName;
        });

        resultsContent.appendChild(table);
    }

    resultsDiv.style.display = 'block';
}

topSongsBtn.addEventListener('click', () => fetchData('/analyze/top-songs'));
topArtistsBtn.addEventListener('click', () => fetchData('/analyze/top-artists'));
topAlbumsBtn.addEventListener('click', () => fetchData('/analyze/top-albums'));
topSongsYearBtn.addEventListener('click', () => {
    const year = document.getElementById('yearInput').value;
    fetchData('/analyze/top-songs', year);
});
topSongsYearMonthBtn.addEventListener('click', () => {
    const year = document.getElementById('yearInput').value;
    const month = document.getElementById('monthInput').value;
    fetchData('/analyze/top-songs', year, month);
});
playedSongsBtn.addEventListener('click', () => {
    const date = dateInput.value;
    if (!date) {
        alert('Please select a date.');
        return;
    }
    fetchPlayedSongs(date);
});