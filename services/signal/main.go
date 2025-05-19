package main

import (
	"log"
	"net/http"
	"sync"

	"github.com/gorilla/websocket"
)

var upgrader = websocket.Upgrader{
	CheckOrigin: func(r *http.Request) bool { return true },
}

type User struct {
	conn *websocket.Conn
	name string
	role string
}

type Message struct {
	Type      string      `json:"type"`
	Name      string      `json:"name,omitempty"`
	Target    string      `json:"target,omitempty"`
	Success   bool        `json:"success,omitempty"`
	Message   string      `json:"message,omitempty"`
	Role      string      `json:"role,omitempty"`
	Offer     interface{} `json:"offer,omitempty"`
	Answer    interface{} `json:"answer,omitempty"`
	Candidate interface{} `json:"candidate,omitempty"`
}

var (
	users          = make(map[string]*User)
	readyUsers     []string
	callInProgress bool
	mu             sync.Mutex
)

func main() {
	http.HandleFunc("/", handleConnections)
	log.Println("Signaling server is running on ws://localhost:3000")
	log.Fatal(http.ListenAndServe(":3000", nil))
}

func handleConnections(w http.ResponseWriter, r *http.Request) {
	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Println("Upgrade error:", err)
		return
	}
	defer conn.Close()

	log.Println("New client connected")

	for {
		var msg Message
		err := conn.ReadJSON(&msg)
		if err != nil {
			log.Printf("Read error: %v", err)
			handleDisconnect(conn)
			return
		}

		log.Printf("Received message from %s: %+v", msg.Name, msg)

		switch msg.Type {
		case "login":
			handleLogin(conn, msg)
		case "offer":
			handleOffer(conn, msg)
		case "answer":
			handleAnswer(conn, msg)
		case "candidate":
			handleCandidate(conn, msg)
		case "leave":
			handleLeave(conn, msg)
		default:
			conn.WriteJSON(Message{Type: "error", Message: "Unknown message type"})
		}
	}
}

func handleDisconnect(conn *websocket.Conn) {
	mu.Lock()
	defer mu.Unlock()

	for name, user := range users {
		if user.conn == conn {
			log.Printf("Client %s disconnected", name)
			delete(users, name)
			removeFromReadyUsers(name)
			callInProgress = false
			return
		}
	}
}

func removeFromReadyUsers(name string) {
	for i, n := range readyUsers {
		if n == name {
			readyUsers = append(readyUsers[:i], readyUsers[i+1:]...)
			break
		}
	}
}

func handleLogin(conn *websocket.Conn, msg Message) {
	mu.Lock()
	defer mu.Unlock()

	name := msg.Name
	if _, exists := users[name]; exists {
		conn.WriteJSON(Message{Type: "login", Success: false, Message: "Username is taken"})
		return
	}

	role := "caller"
	if len(readyUsers) > 0 {
		role = "callee"
	}

	users[name] = &User{conn: conn, name: name, role: role}
	readyUsers = append(readyUsers, name)

	conn.WriteJSON(Message{
		Type:    "login",
		Success: true,
		Role:    role,
	})
	log.Printf("User %s logged in as %s", name, role)

	if len(readyUsers) == 2 {
		log.Println("Both users are ready. Call can start.")
		for _, userName := range readyUsers {
			users[userName].conn.WriteJSON(Message{Type: "ready"})
		}
	}
}

func handleOffer(conn *websocket.Conn, msg Message) {
	mu.Lock()
	defer mu.Unlock()

	user, exists := users[msg.Name]
	if !exists || user.role != "caller" {
		conn.WriteJSON(Message{Type: "error", Message: "Only the caller can send an offer."})
		return
	}

	if callInProgress || users[msg.Target] == nil {
		conn.WriteJSON(Message{Type: "error", Message: "Target user not ready or call already in progress."})
		return
	}

	users[msg.Target].conn.WriteJSON(Message{
		Type:  "offer",
		Offer: msg.Offer,
		Name:  msg.Name,
	})
	callInProgress = true
	log.Printf("Offer sent from %s to %s", msg.Name, msg.Target)
}

func handleAnswer(conn *websocket.Conn, msg Message) {
	mu.Lock()
	defer mu.Unlock()

	user, exists := users[msg.Name]
	if !exists || user.role != "callee" {
		conn.WriteJSON(Message{Type: "error", Message: "Only the callee can send an answer."})
		return
	}

	if users[msg.Target] == nil {
		conn.WriteJSON(Message{Type: "error", Message: "Target user not connected."})
		log.Printf("Failed to send answer: Target %s not connected", msg.Target)
		return
	}

	users[msg.Target].conn.WriteJSON(Message{
		Type:   "answer",
		Answer: msg.Answer,
		Name:   msg.Name,
	})
	log.Printf("Answer sent from %s to %s", msg.Name, msg.Target)
}

func handleCandidate(conn *websocket.Conn, msg Message) {
	mu.Lock()
	defer mu.Unlock()

	if users[msg.Target] != nil {
		users[msg.Target].conn.WriteJSON(Message{
			Type:      "candidate",
			Candidate: msg.Candidate,
			Name:      msg.Name,
		})
		log.Printf("Candidate sent from %s to %s", msg.Name, msg.Target)
	}
}

func handleLeave(conn *websocket.Conn, msg Message) {
	mu.Lock()
	defer mu.Unlock()

	if users[msg.Target] != nil {
		users[msg.Target].conn.WriteJSON(Message{
			Type: "leave",
			Name: msg.Name,
		})
		log.Printf("%s has left the call with %s", msg.Name, msg.Target)
	}
	callInProgress = false
}