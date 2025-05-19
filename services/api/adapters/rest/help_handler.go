package rest

import (
	"encoding/json"
	"log/slog"
	"net/http"
	"seeforme/api/adapters/kafka"
	"strings"
)

type HelpRequest struct {
	Question string `json:"question"`
}

type HelpHandler struct {
	log        *slog.Logger
	kafkaClient *kafka.Client
}

func NewHelpHandler(log *slog.Logger, kafkaClient *kafka.Client) http.Handler {
	return &HelpHandler{
		log:        log,
		kafkaClient: kafkaClient,
	}
}

func (h *HelpHandler) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	var req HelpRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		h.log.Error("failed to decode request", "error", err)
		http.Error(w, "Invalid request format", http.StatusBadRequest)
		return
	}

	// 
	//изменить !!!!!!
	authHeader := r.Header.Get("Authorization")
	userID := "1" // По умолчанию используем ID 1
	if authHeader != "" {
		parts := strings.Split(authHeader, " ")
		if len(parts) == 2 && parts[0] == "Bearer" {
			userID = parts[1] 
		}
	}

	helpRequest := kafka.NewHelpRequest(userID, req.Question)
	data, err := helpRequest.ToJSON()
	if err != nil {
		h.log.Error("failed to marshal help request", "error", err)
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	if err := h.kafkaClient.SendMessage(r.Context(), userID, data); err != nil {
		h.log.Error("failed to send message to kafka", "error", err)
		http.Error(w, "Failed to process help request", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusAccepted)
	json.NewEncoder(w).Encode(map[string]string{
		"status":  "success",
		"message": "Your help request has been received and will be processed shortly",
	})
} 