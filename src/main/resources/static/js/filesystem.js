document.addEventListener("DOMContentLoaded", function() {
    const filesystemElement = document.querySelector("#filesystem .tree");

    // 建立 SSE 连接以接收系统快照数据
    const snapshotSource = new EventSource("/api/snapshot");
    snapshotSource.addEventListener("snapshot", function(event) {
        try {
            const snapshotData = event.data;
            const snapshot = JSON.parse(snapshotData);
            // 仅更新文件目录区域的内容，这里直接用 innerText 更新
            filesystemElement.innerText = snapshot.fileDirectory;
            // 如有其它模块数据，可以更新其它页面区域
        } catch (e) {
            console.error("Error parsing snapshot JSON:", e);
        }
    });
});
