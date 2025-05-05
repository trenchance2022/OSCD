document.addEventListener("DOMContentLoaded", function () {
    const deviceTableBody = document.querySelector("#devices .device-table tbody");

    document.addEventListener("snapshot-update", function (event) {
        const snapshot = event.detail;
        const deviceData = snapshot.deviceManagement;

        if (!deviceData || !deviceData.deviceList) return;

        // 清空旧表格
        deviceTableBody.innerHTML = "";

        // 动态生成设备行
        deviceData.deviceList.forEach(device => {
            const tr = document.createElement("tr");

            const deviceName = device.deviceName || "-";
            const runningProcess = device.runningProcess || "-";
            const waitingQueue = (device.waitingQueue && device.waitingQueue.length > 0)
                ? device.waitingQueue.join(", ") : "-";

            tr.innerHTML = `
                <td>${deviceName}</td>
                <td>${runningProcess}</td>
                <td>${waitingQueue}</td>
            `;

            deviceTableBody.appendChild(tr);
        });
    });
});
