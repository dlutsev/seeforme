const WebSocket = require('ws');

const wss = new WebSocket.Server({ port: 3000 }, () => {
  console.log('Signaling server is running on ws://localhost:3000');
});

// Храним информацию о пользователях и их статусе
let users = {}; // Все подключенные пользователи
let volunteers = []; // Список доступных волонтеров
let blindUsers = []; // Список слепых пользователей, ожидающих помощи
let callPairs = {}; // Пары активных звонков (user: volunteer)

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

        case 'request_call':
          handleCallRequest(ws, data);
          break;

        case 'volunteer_ready':
          handleVolunteerReady(ws, data);
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
      
      // Удаляем пользователя из соответствующих списков
      if (ws.role === 'volunteer') {
        volunteers = volunteers.filter(name => name !== ws.name);
      } else if (ws.role === 'blind') {
        blindUsers = blindUsers.filter(name => name !== ws.name);
      }
      
      // Если был в активном звонке, уведомляем другую сторону
      if (callPairs[ws.name]) {
        const partner = callPairs[ws.name];
        if (users[partner]) {
          users[partner].send(JSON.stringify({
            type: 'call_ended',
            reason: 'partner_disconnected'
          }));
        }
        delete callPairs[ws.name];
        delete callPairs[partner];
      }

      delete users[ws.name];
      
      // Проверяем очередь после отключения
      processQueue();
    }
  });

  ws.on('error', (err) => {
    console.error(`Error with client ${ws.name || 'unknown'}:`, err.message);
  });
});

function handleLogin(ws, data) {
  const name = data.name;
  const role = data.role; // 'volunteer' или 'blind'
  
  if (users[name]) {
    ws.send(JSON.stringify({ type: 'login', success: false, message: 'Username is taken' }));
  } else {
    ws.name = name;
    ws.role = role;
    users[name] = ws;
    
    ws.send(JSON.stringify({ type: 'login', success: true, role: role }));
    console.log(`User ${name} logged in as ${role}`);
    
    if (role === 'volunteer') {
      // По умолчанию волонтер не готов принимать звонки, пока не сообщит о готовности
      console.log(`Volunteer ${name} registered but not ready yet`);
    } else if (role === 'blind') {
      // Слепой пользователь должен вручную запросить звонок
      console.log(`Blind user ${name} registered`);
    }
  }
}

function handleCallRequest(ws, data) {
  if (ws.role !== 'blind') {
    ws.send(JSON.stringify({ type: 'error', message: 'Only blind users can request calls' }));
    return;
  }
  
  // Добавляем пользователя в очередь
  if (!blindUsers.includes(ws.name)) {
    blindUsers.push(ws.name);
    console.log(`User ${ws.name} added to call queue`);
    
    ws.send(JSON.stringify({ 
      type: 'queued', 
      position: blindUsers.indexOf(ws.name) + 1 
    }));
    
    // Проверяем, доступны ли волонтеры
    processQueue();
  }
}

function handleVolunteerReady(ws, data) {
  if (ws.role !== 'volunteer') {
    ws.send(JSON.stringify({ type: 'error', message: 'Only volunteers can report ready status' }));
    return;
  }
  
  // Добавляем волонтера в список доступных
  if (!volunteers.includes(ws.name)) {
    volunteers.push(ws.name);
    console.log(`Volunteer ${ws.name} is now ready to take calls`);
    
    // Проверяем, ожидают ли пользователи
    processQueue();
  }
}

function processQueue() {
  // Если есть ожидающие пользователи и доступные волонтеры, соединяем их
  while (blindUsers.length > 0 && volunteers.length > 0) {
    const blindUser = blindUsers.shift();
    const volunteer = volunteers.shift();
    
    if (users[blindUser] && users[volunteer]) {
      // Создаем пару для звонка
      callPairs[blindUser] = volunteer;
      callPairs[volunteer] = blindUser;
      
      // Сообщаем обеим сторонам о соединении
      users[blindUser].send(JSON.stringify({
        type: 'call_matched',
        target: volunteer
      }));
      
      users[volunteer].send(JSON.stringify({
        type: 'call_request',
        from: blindUser
      }));
      
      console.log(`Matched: blind user ${blindUser} with volunteer ${volunteer}`);
    } else {
      console.error(`Error: User ${blindUser} or volunteer ${volunteer} not found`);
      // Возвращаем в очередь, если что-то пошло не так
      if (users[blindUser]) blindUsers.unshift(blindUser);
      if (users[volunteer]) volunteers.unshift(volunteer);
      break;
    }
  }
}

function handleOffer(ws, data) {
  const targetUser = data.target;
  
  if (!users[targetUser]) {
    ws.send(JSON.stringify({ type: 'error', message: `User ${targetUser} not found` }));
    return;
  }
  
  users[targetUser].send(JSON.stringify({
    type: 'offer',
    offer: data.offer,
    name: ws.name,
  }));
  
  console.log(`Offer sent from ${ws.name} to ${targetUser}`);
}

function handleAnswer(ws, data) {
  const targetUser = data.target;
  
  if (!users[targetUser]) {
    ws.send(JSON.stringify({ type: 'error', message: `User ${targetUser} not found` }));
    return;
  }
  
  users[targetUser].send(JSON.stringify({
    type: 'answer',
    answer: data.answer,
    name: ws.name,
  }));
  
  console.log(`Answer sent from ${ws.name} to ${targetUser}`);
}

function handleCandidate(ws, data) {
  const targetUser = data.target;
  
  if (!users[targetUser]) {
    ws.send(JSON.stringify({ type: 'error', message: `User ${targetUser} not found` }));
    return;
  }
  
  users[targetUser].send(JSON.stringify({
    type: 'candidate',
    candidate: data.candidate,
    name: ws.name,
  }));
  
  console.log(`Candidate sent from ${ws.name} to ${targetUser}`);
}

function handleLeave(ws, data) {
  const targetUser = data.target;
  
  if (users[targetUser]) {
    users[targetUser].send(JSON.stringify({
      type: 'leave',
      name: ws.name,
    }));
    
    // Освобождаем пару звонка
    if (callPairs[ws.name]) {
      delete callPairs[targetUser];
      delete callPairs[ws.name];
      
      // Если это волонтер, возвращаем его в список доступных
      if (ws.role === 'volunteer') {
        volunteers.push(ws.name);
        // Проверяем очередь снова
        processQueue();
      }
    }
    
    console.log(`${ws.name} has left the call with ${targetUser}`);
  }
}

