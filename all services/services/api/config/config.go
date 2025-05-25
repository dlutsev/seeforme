package config

import (
	"log"
	"strings"
	"time"

	"github.com/ilyakaznacheev/cleanenv"
)

type HTTPConfig struct {
	Address string        `yaml:"address" env:"API_ADDRESS" env-default:"localhost:28080"`
	Timeout time.Duration `yaml:"timeout" env:"API_TIMEOUT" env-default:"5s"`
}

type KafkaConfig struct {
	Brokers     []string `yaml:"brokers" env:"KAFKA_BROKERS" env-default:"localhost:9092"`
	HelpTopic   string   `yaml:"help_topic" env:"KAFKA_HELP_TOPIC" env-default:"help-request"`
}

type Config struct {
	LogLevel          string     `yaml:"log_level" env:"LOG_LEVEL" env-default:"DEBUG"`
	HTTPConfig        HTTPConfig `yaml:"api_server"`
	UserAddress       string     `yaml:"user_address" env:"USER_ADDRESS" env-default:"words:81"`
	KafkaConfig       KafkaConfig `yaml:"kafka"`
}

func MustLoad(configPath string) Config {
	var cfg Config
	if err := cleanenv.ReadConfig(configPath, &cfg); err != nil {
		if err := cleanenv.ReadEnv(&cfg); err != nil {
			log.Fatalf("cannot read config %q: %s", configPath, err)
		}
	}

	// Разбиваем строку с адресами брокеров Kafka на массив
	if len(cfg.KafkaConfig.Brokers) == 1 && strings.Contains(cfg.KafkaConfig.Brokers[0], ",") {
		cfg.KafkaConfig.Brokers = strings.Split(cfg.KafkaConfig.Brokers[0], ",")
	}

	return cfg
}