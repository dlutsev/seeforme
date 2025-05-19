package kafka

import (
	"encoding/json"
	"strconv"
	"time"
)

// HelpRequest представляет собой запрос на помощь
type HelpRequest struct {
	CreatedAt        time.Time `json:"createdAt"`
	RequestCreatorId string    `json:"requestCreatorId"`
	Question         string    `json:"question,omitempty"`
}

// NewHelpRequest создает новый запрос на помощь
func NewHelpRequest(userID, question string) HelpRequest {
	return HelpRequest{
		CreatedAt:        time.Now(),
		RequestCreatorId: userID,
		Question:         question,
	}
}

// ToJSON конвертирует запрос в JSON
func (h HelpRequest) ToJSON() ([]byte, error) {
	// Преобразуем CreatedAt в Unix timestamp в секундах (строка)
	timestamp := strconv.FormatInt(h.CreatedAt.Unix(), 10)

	// Создаем временную структуру для сериализации
	type HelpRequestJSON struct {
		CreatedAt        string `json:"createdAt"`
		RequestCreatorId string `json:"requestCreatorId"`
		Question         string `json:"question,omitempty"`
	}

	jsonStruct := HelpRequestJSON{
		CreatedAt:        timestamp,
		RequestCreatorId: h.RequestCreatorId,
		Question:         h.Question,
	}

	return json.Marshal(jsonStruct)
}

// HelpResponse представляет собой ответ на запрос помощи
type HelpResponse struct {
	RequestID string    `json:"request_id"`
	Answer    string    `json:"answer"`
	Timestamp time.Time `json:"timestamp"`
}

// NewHelpResponse создает новый ответ на запрос помощи
func NewHelpResponse(requestID, answer string) HelpResponse {
	return HelpResponse{
		RequestID: requestID,
		Answer:    answer,
		Timestamp: time.Now(),
	}
}

// ToJSON конвертирует ответ в JSON
func (h HelpResponse) ToJSON() ([]byte, error) {
	return json.Marshal(h)
}
