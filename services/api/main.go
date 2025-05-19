package main

import (
	"context"
	"errors"
	"flag"
	"log/slog"
	"net/http"
	"os"
	"os/signal"
	"seeforme/api/adapters/kafka"
	"seeforme/api/adapters/rest"
	"seeforme/api/adapters/user"
	"seeforme/api/config"
)

func main() {
	var configPath string
	flag.StringVar(&configPath, "config", "config.yaml", "server configuration file")
	flag.Parse()

	cfg := config.MustLoad(configPath)
	log := mustMakeLogger(cfg.LogLevel)

	log.Info("starting server")
	log.Debug("debug messages are enabled")

	userservice, err := user.NewClient(cfg.UserAddress, log)
	if err != nil {
		log.Error("failed to init user adapter", "error", err)
		os.Exit(1)
	}

	// Инициализация клиента Kafka
	kafkaClient, err := kafka.NewClient(cfg.KafkaConfig.Brokers, cfg.KafkaConfig.HelpTopic, log)
	if err != nil {
		log.Error("failed to init kafka client", "error", err)
		os.Exit(1)
	}
	defer kafkaClient.Close()

	mux := http.NewServeMux()
	mux.Handle("POST /login", rest.NewLoginHandler(log, userservice))
	mux.Handle("POST /register", rest.NewRegisterHandler(log, userservice))
	mux.Handle("POST /checkjwt", rest.NewCheckJWTHandler(log, userservice))
	
	mux.Handle("POST /help", rest.NewHelpHandler(log, kafkaClient))

	server := http.Server{
		Addr:    cfg.HTTPConfig.Address,
		ReadTimeout: cfg.HTTPConfig.Timeout,
		Handler:     mux,
	}

	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt)
	defer stop()

	go func() {
		<-ctx.Done()
		log.Debug("shutting down server")
		if err := server.Shutdown(context.Background()); err != nil {
			log.Error("erroneous shutdown", "error", err)
		}
	}()

	log.Info("Running HTTP server", "address", cfg.HTTPConfig.Address)
	if err := server.ListenAndServe(); err != nil {
		if !errors.Is(err, http.ErrServerClosed) {
			log.Error("server closed unexpectedly", "error", err)
			return
		}
	}	
}

func mustMakeLogger(logLevel string) *slog.Logger {
	var level slog.Level
	switch logLevel {
	case "DEBUG":
		level = slog.LevelDebug
	case "INFO":
		level = slog.LevelInfo
	case "ERROR":
		level = slog.LevelError
	default:
		panic("unknown log level: " + logLevel)
	}
	handler := slog.NewTextHandler(os.Stderr, &slog.HandlerOptions{Level: level})
	return slog.New(handler)
}
