const diskData = [
  5, 45, 104, 10, "", 1, 7, 7,
  102, "", 10, 5, 7, "", 10, "",
  "", "", 1, 0, 4, 1, 0, "",
  "", "", 105, 0, 5, "", "", "",
  "", "", "", "", "", "", 102, "",
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

const diskGrid = document.getElementById('disk-grid');
diskData.forEach(val => {
  const div = document.createElement('div');
  div.classList.add('block', getColorClass(val));
  div.textContent = val !== "" ? val : "";
  diskGrid.appendChild(div);
});

const requiredBlocks = 64; 
while (diskData.length < requiredBlocks) {
  diskData.push(""); 
}