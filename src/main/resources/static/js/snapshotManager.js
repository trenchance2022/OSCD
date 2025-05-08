document.addEventListener("DOMContentLoaded", function () {
    const eventSource = new EventSource("/api/snapshot");

    eventSource.addEventListener("snapshot", function (event) {
        try {
            const snapshot = JSON.parse(event.data);
            const customEvent = new CustomEvent("snapshot-update", {detail: snapshot});
            document.dispatchEvent(customEvent); // 广播快照事件
        } catch (e) {
            console.error("Failed to parse snapshot:", e);
        }
    });

    eventSource.onerror = function (err) {
        console.error("SSE connection error (snapshot):", err);
    };
});
