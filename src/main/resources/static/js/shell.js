document.addEventListener("DOMContentLoaded", function() {
    // 获取输入区域、固定输入行与滚动输出区域
    const inputLine = document.getElementById("input-line");
    const commandLine = document.getElementById("command-line"); // 固定在顶部的区域
    const terminalOutput = document.getElementById("terminal-output"); // 日志输出区域
    const terminal = document.getElementById("terminal"); // 整个终端容器

    // Modal dialog elements for vi editor
    const viModal = document.getElementById("viModal");
    const viFilenameSpan = document.getElementById("viFilename");
    const viTextarea = document.getElementById("viTextarea");
    const viOkButton = document.getElementById("viOk");
    const viCancelButton = document.getElementById("viCancel");

    // Function to open vi editor modal with given fileName
    function openViEditor(fileName) {
        viFilenameSpan.textContent = fileName;
        // 可选：可通过 AJAX 预先加载文件内容，此处直接清空
        viTextarea.value = "";
        viModal.style.display = "block";
        viTextarea.focus();
    }

    // Function to close vi editor modal
    function closeViEditor() {
        viModal.style.display = "none";
    }

    // vi editor OK button handler
    viOkButton.addEventListener("click", function() {
        const fileName = viFilenameSpan.textContent;
        const content = viTextarea.value;
        // 发送保存请求到后端保存文件内容
        fetch("/api/shell/vi", {
            method: "POST",
            headers: {
                "Content-Type": "application/json;charset=UTF-8"
            },
            body: JSON.stringify({ fileName: fileName, content: content })
        })
            .then(response => response.text())
            .then(result => {
                console.log(result);
            })
            .catch(error => {
                console.error("Error saving file:", error);
            });
        closeViEditor();
    });

    // vi editor Cancel button handler
    viCancelButton.addEventListener("click", function() {
        closeViEditor();
    });

    // 建立 SSE 连接，实时接收后端日志
    const eventSource = new EventSource("/api/shell/stream");
    eventSource.addEventListener("log", function(event) {
        const logMessage = event.data;
        // 检查是否为特殊的 PROMPT 消息

        // 如果收到特殊标识 OPEN_VI_* 则提示用户文件不存在自动新建，并打开编辑窗口
        if (logMessage.startsWith("OPEN_VI_*:")) {
            const fileName = logMessage.split(":")[1].trim();
            alert("File not found. New file has been created for editing.");
            openViEditor(fileName);
            return;
        }
        // 如果收到特殊标识 OPEN_VI 则直接打开编辑窗口
        if (logMessage.startsWith("OPEN_VI:")) {
            const fileName = logMessage.split(":")[1].trim();
            openViEditor(fileName);
            return; // 不显示特殊消息
        }
        // 显示普通日志到输出区域
        const logLine = document.createElement("pre");
        logLine.textContent = logMessage;
        terminalOutput.appendChild(logLine);
        terminalOutput.scrollTop = terminalOutput.scrollHeight;
    });

    // 监听命令输入框的回车事件
    inputLine.addEventListener("keydown", function(e) {
        if (e.key === "Enter") {
            e.preventDefault(); // 防止 contenteditable 中换行
            const command = inputLine.textContent.trim();
            if (command === "") return;

            // 显示用户输入的命令到输出区域
            const commandDisplay = document.createElement("p");
            commandDisplay.textContent = "root> " + command;
            terminalOutput.appendChild(commandDisplay);
            terminalOutput.scrollTop = terminalOutput.scrollHeight;

            // 将命令发送到后端 REST 接口
            fetch("/api/shell/command", {
                method: "POST",
                headers: { "Content-Type": "text/plain;charset=UTF-8" },
                body: command
            })
                .then(response => response.text())
                .then(result => {
                    console.log(result);
                })
                .catch(error => {
                    const errorDisplay = document.createElement("p");
                    errorDisplay.textContent = "Error: " + error;
                    terminalOutput.appendChild(errorDisplay);
                });

            // 清空输入区域
            inputLine.textContent = "";
        }
    });

    // 点击终端区域时聚焦输入框
    terminal.addEventListener("click", function() {
        inputLine.focus();
    });
});
