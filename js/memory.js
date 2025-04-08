const memoryData = [
  45, 103, 104, 105, 106, 107, 46, 47,
  102, "", 10, 5, 7, "", "", "",
  "", "", 1, 0, 4, 1, 0, "",
  "", "", 1, 0, "", 1, "", "",
  "", "", 1, "", "", "", 1, "",
  "", "", "", "", "", "", "", "",
  "", "", "", "", "", "", "", "",
  "", "", "", "", "", "", "", ""
];

function getColorClass(value) {
  if (value === "") return "color-empty";
  if (value === 0) return "color-0";
  if (value === 1) return "color-1";
  if (value === 4) return "color-2";
  if (value === 5) return "color-3";
  if (value === 7) return "color-4";
  if (value === 10) return "color-5";
  if (value === 45 || value === 46 || value === 47) return "color-6";
  if (value >= 100) return "color-7";
  return "color-8"; 
}

const grid = document.getElementById('memory-grid');
memoryData.forEach(val => {
  const div = document.createElement('div');
  div.classList.add('block', getColorClass(val));
  div.textContent = val !== "" ? val : "";
  grid.appendChild(div);
});
