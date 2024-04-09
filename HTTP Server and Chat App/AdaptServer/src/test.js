"use strict";

function handleWSOpenCB(){
    console.log("In web socket open");
    ws.send("Hello!");
    ws.send("Another test");
    ws.send("Here we go again");
}

function handleConnectCB(){
    console.log ("WS connected");
}

// function handleMessageCB(){
//     console.log("Hello there");
//     // ws.send("Sending this.");
// }

function handleMessageCB(event){
    console.log ("In message handle event");
    console.log("Message from server: " + event.data);
}

function handleCloseCB(){
    console.log ("WS closed");
}

function handleWSErrorCB(){
    console.log ("WS error");
}

let chatBox = document.getElementById('chatDiv');
chatBox.style.backgroundColor = "#1982FC";
chatBox.style.width = '400px';
chatBox.style.height = '400px';
chatBox.style.position = "relative";

// body.appendChild(chatBox);

let ws = new WebSocket("ws://localhost:8080");

ws.onopen = handleWSOpenCB;
ws.onconnect = handleConnectCB();
ws.onmessage = handleMessageCB;
ws.onclose = handleCloseCB;
ws.onerror = handleWSErrorCB;