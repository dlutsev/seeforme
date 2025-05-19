package main

import (
	"context"
	"flag"
	"log/slog"
	"net"
	"os"
	"os/signal"
	"seeforme/user/adapters/db"
	"seeforme/user/adapters/jwt"
	"seeforme/user/config"
	"seeforme/user/core"

	"google.golang.org/grpc"
	"google.golang.org/grpc/reflection"

	userpb "seeforme/proto/user"
	usergrpc "seeforme/user/adapters/grpc"
)

func main() {
 	var configPath string
 	flag.StringVar(&configPath, "config", "config.yaml", "server configuration file")
	flag.Parse()
	cfg := config.MustLoad(configPath)

	log := mustMakeLogger(cfg.LogLevel)

	log.Info("starting server")
	log.Debug("debug messages are enabled")
	
	storage, err := db.New(log, cfg.DBAddress)
	if err != nil {
		log.Error("failed to connect to database", "error", err)
		return
	}

	if err := storage.Migrate(); err != nil {
		log.Error("failed to migrate db", "error", err)
		return
	}

	jwt := jwt.New(cfg.JWT.Secret, cfg.JWT.TTL, log)

	userService := core.NewUserService(log, storage, jwt)

	listener, err := net.Listen("tcp", cfg.Address)
	if err != nil {
		log.Error("failed to listen", "error", err)
		return
	}

	s := grpc.NewServer()
	userpb.RegisterUserServer(s, usergrpc.NewServer(userService))
	reflection.Register(s)

	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt)
	defer stop()

	go func() {
		<-ctx.Done()
		log.Debug("shutting down server")
		s.GracefulStop()
	}()

	if err := s.Serve(listener); err != nil {
		log.Error("failed to serve", "erorr", err)
		return
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