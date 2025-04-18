document.addEventListener("DOMContentLoaded", function () {
  const diskGrid = document.getElementById("disk-grid");

  document.addEventListener("snapshot-update", function (event) {
    const snapshot = event.detail;
    const disk = snapshot.diskManagement;
    const occupiedBlocks = disk.occupiedBlocks || [];
    const totalBlocks = disk.totalBlocks || 1024;

    diskGrid.innerHTML = ""; // 清空旧显示

    for (let i = 0; i < totalBlocks; i++) {
      const div = document.createElement("div");
      if (occupiedBlocks.includes(i)) {
        div.classList.add("block", "color-4");
        div.textContent = i; // 显示块号
      } else {
        div.classList.add("block", "color-empty");
        div.textContent = ""; // 空闲块不显示数字
      }
      diskGrid.appendChild(div);
    }
  });
});
