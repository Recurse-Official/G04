const dropzone = document.getElementById('dropzone');
const fileInput = document.getElementById('fileInput');
const uploadBtn = document.getElementById('uploadBtn');
const resultsDiv = document.getElementById('results');
const chartCanvas = document.getElementById('chart');
const notification = document.getElementById('notification');
const loading = document.getElementById('loading');

let selectedFile = null;

// Display Notification
function showNotification(message, type) {
  notification.textContent = message;
  notification.className = `notification ${type}`;
  notification.style.display = 'block';
  setTimeout(() => {
    notification.style.display = 'none';
  }, 3000);
}

// Handle Drag and Drop
dropzone.addEventListener('dragover', (e) => {
  e.preventDefault();
  dropzone.style.backgroundColor = '#e8f7e8';
});

dropzone.addEventListener('dragleave', () => {
  dropzone.style.backgroundColor = '#f9fff9';
});

dropzone.addEventListener('drop', (e) => {
  e.preventDefault();
  dropzone.style.backgroundColor = '#f9fff9';
  selectedFile = e.dataTransfer.files[0];
  showNotification(`File "${selectedFile.name}" selected`, 'success');
});

// Handle Click on Dropzone
dropzone.addEventListener('click', () => fileInput.click());

// Handle File Input Change
fileInput.addEventListener('change', (e) => {
  selectedFile = e.target.files[0];
  showNotification(`File "${selectedFile.name}" selected`, 'success');
});

// Upload File and Display Results
uploadBtn.addEventListener('click', async () => {
  if (!selectedFile) {
    showNotification('Please select an APK file!', 'error');
    return;
  }

  const formData = new FormData();
  formData.append('file', selectedFile);

  loading.style.display = 'block';

  try {
    const response = await fetch('http://localhost:5000/scan', {
      method: 'POST',
      body: formData,
    });

    const data = await response.json();
    loading.style.display = 'none';
    displayResults(data);
  } catch (error) {
    loading.style.display = 'none';
    showNotification('Error uploading file.', 'error');
    console.error(error);
  }
});

// Display Results in Chart
function displayResults(data) {
  resultsDiv.classList.remove('hidden');

  const ctx = chartCanvas.getContext('2d');
  new Chart(ctx, {
    type: 'bar',
    data: {
      labels: Object.keys(data),
      datasets: [{
        label: 'Scan Results',
        data: Object.values(data),
        backgroundColor: ['#4caf50', '#ff9800', '#f44336'],
      }],
    },
    options: {
      responsive: true,
      plugins: {
        legend: {
          display: false,
        },
      },
    },
  });
}
// JavaScript to randomize icon positions
window.onload = function() {
  const icons = document.querySelectorAll('.icon');  // Select all icons
  
  icons.forEach(icon => {
      // Generate random positions
      const randomTop = Math.random() * window.innerHeight;  // Random vertical position
      const randomLeft = Math.random() * window.innerWidth; // Random horizontal position

      // Apply random positions
      icon.style.top = `${randomTop}px`;
      icon.style.left = `${randomLeft}px`;
  });
};
