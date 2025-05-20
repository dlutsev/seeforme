const WebSocket = require('ws');


const wss = new WebSocket.Server({ port: 3000 }, () => {
  console.log('Signaling server is running on ws://localhost:3000');
});

let users = {};
let readyUsers = [];
let callInProgress = false;

wss.on('connection', (ws) => {
  console.log('New client connected');

  ws.on('message', (message) => {
    try {
      const data = JSON.parse(message.toString());
      console.log(`Received message from ${ws.name || 'unknown'}:`, data);

      switch (data.type) {
        case 'login':
          handleLogin(ws, data);
          break;

        case 'offer':
          handleOffer(ws, data);
          break;

        case 'answer':
          handleAnswer(ws, data);
          break;

        case 'candidate':
          handleCandidate(ws, data);
          break;

        case 'leave':
          handleLeave(ws, data);
          break;

        default:
          ws.send(JSON.stringify({ type: 'error', message: 'Unknown message type' }));
          break;
      }
    } catch (e) { console.error('Error processing message:', e.message);}
  });

  ws.on('close', () => {
    if (ws.name) {
      console.log(`Client ${ws.name} disconnected`);
      delete users[ws.name];
      readyUsers = readyUsers.filter(name => name !== ws.name);
      callInProgress = false;
    }
  });

  ws.on('error', (err) => {
    console.error(`Error with client ${ws.name || 'unknown'}:`, err.message);
  });
});

function handleLogin(ws, data) {
  const name = data.name;
  if (users[name]) {
    ws.send(JSON.stringify({ type: 'login', success: false, message: 'Username is taken' }));
  } else {
    ws.name = name;
    users[name] = ws;
    ws.role = readyUsers.length === 0 ? 'caller' : 'callee';
    ws.userType = data.userType || 'user';
    readyUsers.push(name);

    ws.send(JSON.stringify({ 
      type: 'login', 
      success: true, 
      role: ws.role,
      userType: ws.userType
    }));
    console.log(`User ${name} logged in as ${ws.role}`);

    if (readyUsers.length === 2) {
      console.log('Both users are ready. Call can start.');
      readyUsers.forEach((userName) => {
        users[userName].send(JSON.stringify({ type: 'ready' }));
      });
    }
  }
}

function handleOffer(ws, data) {
  const target = data.target;
  if (users[target]) {
    users[target].send(JSON.stringify({
      type: 'offer',
      offer: data.offer,
      name: ws.name,
    }));
    callInProgress = true;
    console.log(`Offer sent from ${ws.name} to ${target}.`);
  } else {
    ws.send(JSON.stringify({ type: 'error', message: 'Target user not connected.' }));
    console.error(`Failed to send offer: Target ${target} not connected.`);
  }
}

function handleAnswer(ws, data) {
  const target = data.target;
  if (users[target]) {
    users[target].send(JSON.stringify({
      type: 'answer',
      answer: data.answer,
      name: ws.name,
    }));
    console.log(`Answer sent from ${ws.name} to ${target}.`);
  } else {
    ws.send(JSON.stringify({ type: 'error', message: `User ${target} is not connected.` }));
    console.error(`Failed to send answer: Target ${target} not connected.`);
  }
}

function handleCandidate(ws, data) {
  const target = data.target;
  if (users[target]) {
    users[target].send(JSON.stringify({
      type: 'candidate',
      candidate: data.candidate,
      name: ws.name,
    }));
    console.log(`Candidate sent from ${ws.name} to ${target}.`);
  }
}

function handleLeave(ws, data) {
  const target = data.target;
  if (users[target]) {
    users[target].send(JSON.stringify({
      type: 'leave',
      name: ws.name,
    }));
    console.log(`${ws.name} has left the call with ${target}.`);
    users[target].send(JSON.stringify({
      type: 'call_ended',
      message: 'Другой участник завершил звонок',
    }));
    console.log(`Sent call_ended notification to ${target}`);
  }
  callInProgress = false;
}
