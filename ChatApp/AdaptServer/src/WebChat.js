"use strict";
///////////////////////////////////////////////////////////////////////////////////////////////////////////////
//CB functions

function handleWSOpenCB(){
    console.log("Web socket open");
}

function handleConnectCB(){
    console.log ("WS connected");
}

function handleWSErrorCB() {
    console.log("A WS error occurred");
}

// Displays message when message comes back from server
function handleMessageCB(event){

    console.log("In handle message: " + event.data);

    let toParse = JSON.parse(event.data);
    let user = toParse.user;
    console.log("USER IS: " + user);

    // Div to hold all message components
    let allContainer = document.createElement('div');

    // Div to contain messages
    let toPostDiv = document.createElement('div');
    toPostDiv.id = "toPostDiv";
    toPostDiv.style.borderRadius = "25px";
    toPostDiv.style.overflowWrap = "break-word";
    toPostDiv.style.display= "inline-block";
    toPostDiv.style.marginRight = "auto";
    toPostDiv.style.marginLeft = '15px';
    toPostDiv.style.padding = "10px, 5px, 5px, 10 px";

    // Blockquote to contain username - change alignment based on user
    let toPost = document.createElement('blockquote');
    let messageText = document.createTextNode (toParse.message);

    toPost.appendChild(messageText);
    toPostDiv.appendChild(toPost);

    // Div to hold username
    let userContainer = document.createElement('div');
    userContainer.style.fontSize = "small";

    // let userText = document.createTextNode(toParse.message);
    let userText = document.createTextNode(user);
    // let userText = document.createTextNode(toParse.user);

    userContainer.style.color = "grey";

    userContainer.append(userText);

    // Div to hold time of message sent
    let timeDiv = document.createElement('div');
    let date = new Date();

    let time = date.toTimeString();
    time = time.substring(0, 9);

    let timeText = document.createTextNode(time);
    timeDiv.style.fontSize = "small";
    timeDiv.style.color = "grey";

    timeDiv.appendChild(timeText);

    allContainer.appendChild(userContainer);
    allContainer.appendChild(timeDiv);
    allContainer.appendChild(toPostDiv);
    allContainer.appendChild(lineBreakContainer);

    // Formatting different messages depending on if you are sending or receiving the message (send blue messages
    // on the right, and recieve grey messages on the left like iMessage

    if (user == socketUser && toParse.user != join){
        toPost.style.color = "white";

        toPostDiv.style.backgroundColor = "#1982FC";
        toPostDiv.style.textAlign = "left";

        userContainer.setAttribute("align", "right");

        timeDiv.setAttribute("align", "right");

        allContainer.setAttribute("align", "right");
    }
    else {

        toPost.style.color = "black";

        toPostDiv.style.backgroundColor = "lightgrey";
        toPostDiv.style.textAlign = "right";

        userContainer.setAttribute("align", "left");

        timeDiv.setAttribute("align", "left");

        allContainer.setAttribute("align", "left");
    }

    chatBox.appendChild(allContainer);

    toPostDiv.scrollIntoView();
}

// Displays server information if server gets disconnected
function handleCloseCB(){

    console.log ("WS closed");

    let serverClosedText = document.createTextNode("Server Closed: Cannot send messages.");
    let lineBreak = document.createElement('br');

    // Create blockquote container so we can change color of the text
    let container = document.createElement('blockquote');
    container.style.color = "gray";
    container.style.fontWeight = "bold";
    container.style.textAlign = "center";

    container.appendChild(serverClosedText);
    container.appendChild(lineBreak);
    chatBox.appendChild(container);

    container.scrollIntoView();

    // Adds/removes Text that has Green circle/Red x and displays status of server connection depending on whether
    // server is connected or not

    connectContainer.removeChild(visualContainerConnected);
    connectContainer.removeChild(connectedText);

    connectContainer.appendChild(visualContainerDisconnected);
    connectContainer.appendChild(disconnectedText);
}

// Determines whether the user entered an acceptable room name - all lowercase characters, no spaces
function validRoomName(roomNameValue){
    for (let i = 0; i < roomNameValue.length; i++) {
        let char = roomNameValue[i];
        if (char < 'a' || char > 'z'){
            return false;
        }
    }
    return true;
}

// When the join button is pressed in the client, checks to see if the room name is valid, lets the user know
// they have joined the room, and sends join information back to the server
function handleJoinPressCB(){

    // User can only join the room if they have not already joined one
    if (!userNotJoined){

        // User cannot join the room unless the name is valid
        if (!validRoomName(roomTA.value)){
            alert("Please type in all lowercase letters");
            roomTA.value = "<Enter letters>";
            roomTA.select();
            return;
        }


        else{
            roomName = roomTA.value;

            let joinMessageContainer = document.createElement('blockquote');
            joinMessageContainer.style.fontWeight = "bold";
            joinMessageContainer.style.textAlign = "center";
            joinMessageContainer.style.color = "grey";

            let myDivText = document.createTextNode("You have entered the chat room: \"" + roomName + "\"!");

            joinMessageContainer.appendChild(myDivText);

            chatBox.appendChild(joinMessageContainer);

            joinMessageContainer.scrollIntoView();

            // The user can now type messages in the message box
            messageBox.disabled = false;

            // Send message to server that the client is joining a room
            ws.send("join " + roomName);

            // Reset user joined status so that they can't rejoin the room
            userNotJoined = true;
            // Reset user left status so that they can leave the room
            userNotLeft = false;

            // Get the username from the username box so that we can display the messages correctly to the users
            socketUser = usernameBox.value + ":";
        }
    }

    else if (userNotJoined){
        // Let the user know that they can't rejoin the room
        alert("You have already joined a chat room.");
        console.log("Room already joined.");
    }

}

// When the leave button is pressed in the client, lets the user know they have joined the room
// and sends join information back to the server

function handleLeavePressCB(){

    // User can only leave the room if they have not already left one
    if (!userNotLeft){

        roomName = roomTA.value;

        let leaveMessageContainer = document.createElement('blockquote');
        leaveMessageContainer.style.fontWeight = "bold";
        leaveMessageContainer.style.textAlign = "center";
        leaveMessageContainer.style.color = "grey";

        let myDivText = document.createTextNode("You have left the chat room: \"" + roomName + "\".");

        leaveMessageContainer.appendChild(myDivText);

        chatBox.appendChild(leaveMessageContainer);

        leaveMessageContainer.scrollIntoView();

        // The user can no longer type messages in the message box
        messageBox.disabled = true;

        // Send message to server that the client is leaving a room
        ws.send("leave " + roomName);

        // Reset user left status so that they can't re-leave the room
        userNotLeft = true;
        // Reset user joined status so that they can now join a room
        userNotJoined = false;

    }
    else if (userNotLeft){
        // Let the user know that they can't re-leave the room
        alert("You are not currently in a chat room.");
        console.log("Room already left");
    }
}

function handleSendMessageCB(event){

    // works for both enter and clicking the send button
    if (event.keyCode == 13 || event.type == "click"){

        console.log(usernameBox.value);
        console.log(messageBox.value);
        console.log(usernameBox.value + " " + messageBox.value);

        let message = messageBox.value;
        message = usernameBox.value + ": " + message;
        console.log("Message to send: " + message);

        ws.send(message);

        // Clear message box
        messageBox.value = "";

        event.preventDefault();
    }
}

///////////////////////////////////////////////////////////////////////////////////////////////////////////////

let body = document.body;
body.style.textAlign = "center";

let roomTA = document.getElementById('roomTA');
let usernameBox = document.getElementById('userTA');

// Variables to be used throughout the js
let roomName;
let userNotJoined = false;
let userNotLeft = false;
let socketUser;

// Join button
let join = document.getElementById('button');
join.addEventListener("click", handleJoinPressCB);

// Leave button
let leave = document.getElementById('leavebutton');
leave.addEventListener("click", handleLeavePressCB);

// Where chat messages will be posted
let chatBox = document.getElementById('chatDiv');
chatBox.style.backgroundColor = "white";
chatBox.style.border = "thick solid grey";
chatBox.style.borderWidth = "5px";
chatBox.style.width = '400px';
chatBox.style.height = '400px';
chatBox.style.position = "relative";
chatBox.style.padding = ("15px 15px 15px 15px");
chatBox.style.textAlign = "left";

// Let user know if the server has disconnected
let connectContainer = document.getElementById('paragraph1');

let disconnectedText = document.createTextNode(" Server Disconnected");
let connectedText = document.createTextNode(" Server Connected");

let visualContainerConnected = document.createElement("span");
let visualContainerDisconnected = document.createElement("span");

// For green connected circle
let connectedVisual = document.createTextNode("O");
visualContainerConnected.appendChild(connectedVisual);
visualContainerConnected.style.color = "lightgreen";
visualContainerConnected.style.fontWeight = "bold";

// For red disconnected X
let disconnectedVisual = document.createTextNode("X");
visualContainerDisconnected.appendChild(disconnectedVisual);
visualContainerDisconnected.style.color = "red";
visualContainerDisconnected.style.fontWeight = "bold";

connectContainer.appendChild(visualContainerConnected);
connectContainer.appendChild(connectedText);

// Box for user to type their message
let messageBox = document.getElementById('messageTA');
messageBox.addEventListener("keypress", handleSendMessageCB);

// Gives a send button option in addition to pressing enter
let sendButton = document.getElementById('sendButton');
sendButton.addEventListener("click", handleSendMessageCB);

// For adding line breaks
let lineBreakContainer = document.createElement('blockquote');
let myLineBreak = document.createElement('br');
lineBreakContainer.append(myLineBreak);

// Create Web Socket
let ws = new WebSocket("ws://localhost:8080");
// let ws = new WebSocket("ws://10.17.174.89:8080");

ws.onopen = handleWSOpenCB;
ws.onconnect = handleConnectCB();
ws.onmessage = handleMessageCB;
ws.onclose = handleCloseCB;
ws.onerror = handleWSErrorCB;