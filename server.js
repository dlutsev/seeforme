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
    readyUsers.push(name);

    ws.send(JSON.stringify({ type: 'login', success: true, role: ws.role }));
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
  if (ws.role !== 'caller') {
    ws.send(JSON.stringify({ type: 'error', message: 'Only the caller can send an offer.' }));
    return;
  }
  if (!callInProgress && users[data.target]) {
    users[data.target].send(JSON.stringify({
      type: 'offer',
      offer: data.offer,
      name: ws.name,
    }));
    callInProgress = true;
    console.log(`Offer sent from ${ws.name} to ${data.target}.`);
  } else {
    ws.send(JSON.stringify({ type: 'error', message: 'Target user not ready or call already in progress.' }));
  }
}

function handleAnswer(ws, data) {
  if (ws.role !== 'callee') {
    ws.send(JSON.stringify({ type: 'error', message: 'Only the callee can send an answer.' }));
    return;
  }
  if (users[data.target]) {
    users[data.target].send(JSON.stringify({
      type: 'answer',
      answer: data.answer,
      name: ws.name,
    }));
    console.log(`Answer sent from ${ws.name} to ${data.target}.`);
  } else {
    ws.send(JSON.stringify({ type: 'error', message: `User ${data.target} is not connected.` }));
    console.error(`Failed to send answer: Target ${data.target} not connected.`);
  }
}

function handleCandidate(ws, data) {
  if (users[data.target]) {
    users[data.target].send(JSON.stringify({
      type: 'candidate',
      candidate: data.candidate,
      name: ws.name,
    }));
    console.log(`Candidate sent from ${ws.name} to ${data.target}.`);
  }
}

function handleLeave(ws, data) {
  if (users[data.target]) {
    users[data.target].send(JSON.stringify({
      type: 'leave',
      name: ws.name,
    }));
    console.log(`${ws.name} has left the call with ${data.target}.`);
  }
  callInProgress = false;
}

