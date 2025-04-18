document.addEventListener("DOMContentLoaded", function () {
  const memoryGrid = document.getElementById("memory-grid");

  document.addEventListener("snapshot-update", function (event) {
    const snapshot = event.detail;
    const memory = snapshot.memoryManagement;
    const frameInfo = memory.frameInfo || [];
    const totalFrames = memory.totalFrames || 64;

    memoryGrid.innerHTML = ""; // 清空旧显示

    // 构建 pid 到颜色 class 的映射
    const colorClasses = [
      "color-0", "color-1", "color-2", "color-3",
      "color-4", "color-5", "color-6", "color-7", "color-9"
    ];
    const pidColorMap = {};
    let colorIndex = 0;

    // 构建 frameId 到 frame 的完整映射
    const frameMap = {};
    frameInfo.forEach(frame => {
      const pid = frame.pid;
      const frameId = frame.frameId;
      frameMap[frameId] = frame;
      if (!(pid in pidColorMap)) {
        pidColorMap[pid] = colorClasses[colorIndex % colorClasses.length];
        colorIndex++;
      }
    });

    for (let i = 0; i < totalFrames; i++) {
      const div = document.createElement("div");
      div.classList.add("block");
      div.style.pointerEvents = "auto"; // 保证 hover 时 tooltip 能出现

      if (frameMap.hasOwnProperty(i)) {
        const { pid, page } = frameMap[i];
        div.classList.add(pidColorMap[pid]);
        div.textContent = pid;
        div.title = `Frame: ${i}\nPID: ${pid}\nPage: ${page}`;
      } else {
        div.classList.add("color-empty");
        div.textContent = "";
        div.title = `Frame: ${i} (空闲)`;
      }

      memoryGrid.appendChild(div);
    }
  });
});
