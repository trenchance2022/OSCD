document.addEventListener("DOMContentLoaded", function () {
    const cpuTableBody = document.getElementById("cpu-table-body");
    const cpuRunningEl = document.getElementById("cpu-running");
    const readyQueueEl = document.getElementById("ready-queue");
    const waitingQueueEl = document.getElementById("waiting-queue");
    const schedulingPolicyEl = document.getElementById("scheduling-policy");

    document.addEventListener("snapshot-update", function (event) {
        const snapshot = event.detail;
        const process = snapshot.processManagement;

        // 清空现有 CPU 表格行
        cpuTableBody.innerHTML = "";

        // 渲染 CPU 核心信息
        (process.cpuDetails || []).forEach(cpu => {
            const tr = document.createElement("tr");
            tr.innerHTML = `
        <td>核${cpu.cpuId}</td>
        <td>${cpu.pid ?? '--'}</td>
        <td>${cpu.name ?? '--'}</td>
        <td>${cpu.instruction ?? '--'}</td>
        <td>${cpu.remainingTime ?? '--'}</td>
        <td>${cpu.priority ?? '--'}</td>
      `;
            cpuTableBody.appendChild(tr);
        });

        // 渲染运行/就绪/等待队列和调度策略
        cpuRunningEl.textContent = process.cpuRunning || "--";
        readyQueueEl.textContent = (process.readyQueue || []).join(", ") || "--";
        waitingQueueEl.textContent = (process.waitingQueue || []).join(", ") || "--";
        schedulingPolicyEl.textContent = process.schedulingPolicy || "--";
    });
});
