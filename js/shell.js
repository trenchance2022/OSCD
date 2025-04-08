const terminal = document.getElementById('terminal');

terminal.innerHTML = '';


const inputLine = document.createElement('p');
inputLine.innerHTML = 'root&gt; <span contenteditable="true"></span>'; 
terminal.appendChild(inputLine);


const initialInput = inputLine.querySelector('span');
initialInput.focus();

terminal.addEventListener('keydown', function(event) {

  const currentInput = terminal.querySelector('span[contenteditable="true"]');
  
  if (event.key === 'Enter') {
    const command = currentInput.textContent.trim();


    event.preventDefault();

    const newCommandLine = document.createElement('p');
    newCommandLine.innerHTML = 'root&gt; <span contenteditable="true"></span>';
    terminal.appendChild(newCommandLine);

    const newInputLine = newCommandLine.querySelector('span');
    newInputLine.focus();

    terminal.scrollTop = terminal.scrollHeight;
  }
});
