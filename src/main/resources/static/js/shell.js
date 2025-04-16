document.addEventListener("DOMContentLoaded", function() {
    // 获取 DOM 元素
    const terminal = document.getElementById("terminal");
    const commandLine = document.getElementById("command-line"); // 固定区域（不变）
    const terminalOutput = document.getElementById("terminal-output");

    // 初始提示符（全局变量），固定区域内保持为初始提示符，不变化
    let currentPrompt = "/root>";

    // Modal dialog for vi editor
    const viModal = document.getElementById("viModal");
    const viFilenameSpan = document.getElementById("viFilename");
    const viTextarea = document.getElementById("viTextarea");
    const viOkButton = document.getElementById("viOk");
    const viCancelButton = document.getElementById("viCancel");

    // 获取固定区域中的输入框
    function getInputLine() {
        return document.getElementById("input-line");
    }

    // 辅助函数：在日志输出区域末尾追加一行提示符（带有 class "log-prompt"）
    function appendPrompt(promptText) {
        const promptElem = document.createElement("p");
        promptElem.className = "log-prompt";
        promptElem.textContent = promptText;
        terminalOutput.appendChild(promptElem);
        terminalOutput.scrollTop = terminalOutput.scrollHeight;
    }

    // 辅助函数：更新日志区域中最后一行的提示符文本
    function updateLastPrompt(promptText) {
        const prompts = terminalOutput.getElementsByClassName("log-prompt");
        if (prompts.length > 0) {
            const lastPrompt = prompts[prompts.length - 1];
            lastPrompt.textContent = promptText;
        }
    }

    // 初始状态：日志输出区域显示一行提示符
    appendPrompt(currentPrompt);

    // vi 编辑器函数
    function openViEditor(fileName) {
        viFilenameSpan.textContent = fileName;
        viTextarea.value = "";
        viModal.style.display = "block";
        viTextarea.focus();
    }
    function closeViEditor() {
        viModal.style.display = "none";
    }
    viOkButton.addEventListener("click", function() {
        const fileName = viFilenameSpan.textContent;
        const content = viTextarea.value;
        fetch("/api/shell/vi", {
            method: "POST",
            headers: {"Content-Type": "application/json;charset=UTF-8"},
            body: JSON.stringify({ fileName: fileName, content: content })
        })
            .then(response => response.text())
            .then(result => { console.log("File saved:", result); })
            .catch(error => { console.error("Error saving file:", error); });
        closeViEditor();
    });
    viCancelButton.addEventListener("click", function() {
        closeViEditor();
    });

    // 建立 SSE 连接以实时接收后端日志与提示符更新
    const eventSource = new EventSource("/api/shell/stream");
    eventSource.addEventListener("log", function(event) {
        const logMessage = event.data;
        // 特殊信号处理
        if (logMessage.startsWith("OPEN_VI_*:")) {
            const fileName = logMessage.split(":")[1].trim();
            alert("File not found. A new file has been created for editing.");
            openViEditor(fileName);
            return;
        }
        if (logMessage.startsWith("OPEN_VI:")) {
            const fileName = logMessage.split(":")[1].trim();
            openViEditor(fileName);
            return;
        }
        if (logMessage.startsWith("PROMPT:")) {
            // 更新当前提示符
            currentPrompt = logMessage.substring("PROMPT:".length).trim();
            updateLastPrompt(currentPrompt);
            return;
        }
        // 普通日志消息：直接追加到日志输出区域
        const pre = document.createElement("pre");
        pre.textContent = logMessage;
        terminalOutput.appendChild(pre);
        terminalOutput.scrollTop = terminalOutput.scrollHeight;
    });

    // 绑定输入框回车事件
    const inputLine = getInputLine();
    inputLine.addEventListener("keydown", function(e) {
        if (e.key === "Enter") {
            e.preventDefault();  // 阻止 contenteditable 内换行
            const command = inputLine.textContent.trim();
            if (command === "") return;
            // 在日志区域追加当前命令行记录：格式为“当前提示符 + 空格 + 命令”
            const cmdElem = document.createElement("p");
            cmdElem.textContent = currentPrompt + " " + command;
            terminalOutput.appendChild(cmdElem);
            terminalOutput.scrollTop = terminalOutput.scrollHeight;
            // 发送命令到后端
            fetch("/api/shell/command", {
                method: "POST",
                headers: {"Content-Type": "text/plain;charset=UTF-8"},
                body: command
            })
                .then(response => response.text())
                .then(result => { console.log("Command result:", result); })
                .catch(error => {
                    const errorElem = document.createElement("p");
                    errorElem.textContent = "Error: " + error;
                    terminalOutput.appendChild(errorElem);
                });
            // 如果命令是 "cd" 或 "cd.."，我们期望后端返回更新后的提示符
            // 此处如果命令为 cd 开头，则先删除旧提示符行和添加新的提示符行
            const tokens = command.split(" ");
            if (tokens[0] === "cd" || tokens[0] === "cd..") {
                // 删除当前最新提示符行
                const prompts = terminalOutput.getElementsByClassName("log-prompt");
                if (prompts.length > 0) {
                    prompts[prompts.length - 1].remove();
                }
                // 先在输出区域追加一行旧的提示符，等待后端通过 PROMPT 消息更新
                appendPrompt(currentPrompt);
            }
            // 清空输入区域
            inputLine.textContent = "";
        }
    });

    // 点击终端区域时聚焦输入框
    terminal.addEventListener("click", function() {
        const inputLine = getInputLine();
        if (inputLine) {
            inputLine.focus();
        }
    });
});
