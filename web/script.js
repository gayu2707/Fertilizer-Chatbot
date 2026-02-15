const sendBtn = document.getElementById("sendBtn");
const userInput = document.getElementById("userInput");
const chatBox = document.getElementById("chatBox");
const newChatBtn = document.getElementById("newChatBtn");
const chatHistory = document.getElementById("chatHistory");
const reportBtn = document.getElementById("reportBtn");
const searchChat = document.getElementById("searchChat");

const issueModal = document.getElementById("issueModal");
const closeModal = document.getElementById("closeModal");
const submitIssue = document.getElementById("submitIssue");
const issueText = document.getElementById("issueText");

let chats = JSON.parse(localStorage.getItem("chats")) || [];
let currentChat = [];

function renderMessages(messages) {
    chatBox.innerHTML = "";
    messages.forEach(msg => {
        const div = document.createElement("div");
        div.classList.add("message", msg.sender === "user" ? "user-message" : "bot-message");
        div.textContent = msg.text;
        chatBox.appendChild(div);
    });
    chatBox.scrollTop = chatBox.scrollHeight;
}

function addMessage(sender, text) {
    currentChat.push({ sender, text });
    renderMessages(currentChat);
}

function saveChat() {
    if (currentChat.length > 0) {
        chats.push(currentChat);
        localStorage.setItem("chats", JSON.stringify(chats));
        loadChatHistory();
        currentChat = [];
        chatBox.innerHTML = "";
    }
}

function loadChatHistory() {
    chatHistory.innerHTML = "";
    chats.forEach((chat, index) => {
        const li = document.createElement("li");
        li.textContent = `Chat ${index + 1}`;
        li.style.cursor = "pointer";
        li.onclick = () => {
            currentChat = chat;
            renderMessages(currentChat);
        };
        chatHistory.appendChild(li);
    });
}

searchChat.addEventListener("input", () => {
    const query = searchChat.value.toLowerCase();
    const chatsLi = chatHistory.getElementsByTagName("li");
    Array.from(chatsLi).forEach(li => {
        li.style.display = li.textContent.toLowerCase().includes(query) ? "block" : "none";
    });
});

sendBtn.addEventListener("click", () => {
    const text = userInput.value.trim();
    if (text === "") return;

    addMessage("user", text);
    userInput.value = "";

    fetch("/Fertilizerchatbot/chat", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: "message=" + encodeURIComponent(text)
    })
        .then(res => res.json())  
        .then(data => {
            addMessage("bot", data.reply);
        })
        .catch(() => {
            addMessage("bot", "⚠️ Server not responding...");
        });
});

userInput.addEventListener("keypress", (e) => {
    if (e.key === "Enter") {
        sendBtn.click();
    }
});

newChatBtn.addEventListener("click", saveChat);

reportBtn.addEventListener("click", () => {
    issueModal.style.display = "block";
});

closeModal.addEventListener("click", () => {
    issueModal.style.display = "none";
});

submitIssue.addEventListener("click", () => {
    const issue = issueText.value.trim();
    if (issue === "") return;

    fetch("/Fertilizerchatbot/report", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: "issue=" + encodeURIComponent(issue)
    })
        .then(res => res.text())
        .then(reply => {
            alert(reply);
            issueModal.style.display = "none";
            issueText.value = "";
        })
        .catch(() => {
            alert("⚠️ Failed to submit issue");
        });
});

loadChatHistory();


