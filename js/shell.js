// 获取终端元素
const terminal = document.getElementById('terminal');

// 清空终端，确保没有初始的输入行
terminal.innerHTML = '';

// 创建初始的输入行
const inputLine = document.createElement('p');
inputLine.innerHTML = 'root&gt; <span contenteditable="true"></span>'; // 创建可编辑的span
terminal.appendChild(inputLine);

// 聚焦到输入框
const initialInput = inputLine.querySelector('span');
initialInput.focus();

// 监听输入框的键盘事件
terminal.addEventListener('keydown', function(event) {
  // 获取当前输入框
  const currentInput = terminal.querySelector('span[contenteditable="true"]');
  
  if (event.key === 'Enter') {
    // 获取输入的命令
    const command = currentInput.textContent.trim();

   
    // 防止默认的回车行为（防止换行）
    event.preventDefault();

    // 创建新的输入行，只显示 root> 而没有模拟输出
    const newCommandLine = document.createElement('p');
    newCommandLine.innerHTML = 'root&gt; <span contenteditable="true"></span>';
    terminal.appendChild(newCommandLine);

    // 聚焦到新的输入框
    const newInputLine = newCommandLine.querySelector('span');
    newInputLine.focus();

    // 保证滚动到底部
    terminal.scrollTop = terminal.scrollHeight;
  }
});
