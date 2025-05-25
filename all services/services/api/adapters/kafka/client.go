package kafka

import (
	"context"
	"log/slog"
	"strings"
	"time"

	"github.com/segmentio/kafka-go"
)

type Client struct {
	writer *kafka.Writer
	log    *slog.Logger
}

func NewClient(brokers []string, topic string, log *slog.Logger) (*Client, error) {
	// Преобразуем адреса брокеров в формат "host:port"
	var addresses []string
	for _, broker := range brokers {
		if !strings.Contains(broker, ":") {
			broker = broker + ":9092" // Добавляем порт по умолчанию, если его нет
		}
		addresses = append(addresses, broker)
	}

	log.Debug("connecting to kafka brokers", "addresses", addresses)

	writer := &kafka.Writer{
		Addr:         kafka.TCP(addresses...),
		Topic:        topic,
		Balancer:     &kafka.LeastBytes{},
		BatchTimeout: 10 * time.Millisecond,
		RequiredAcks: kafka.RequireAll, // Требуем подтверждения от всех реплик
		Async:        false,            // Синхронная отправка для отладки
	}

	return &Client{
		writer: writer,
		log:    log,
	}, nil
}

func (c *Client) SendMessage(ctx context.Context, key string, value []byte) error {
	message := kafka.Message{
		Key:   []byte(key),
		Value: value,
		Time:  time.Now(),
	}

	err := c.writer.WriteMessages(ctx, message)
	if err != nil {
		c.log.Error("failed to write message to kafka", "error", err)
		return err
	}

	c.log.Debug("message sent to kafka", "key", key)
	return nil
}

func (c *Client) Close() error {
	return c.writer.Close()
}
