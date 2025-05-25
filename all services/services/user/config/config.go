package config

import (
	"log"
	"time"
	"github.com/ilyakaznacheev/cleanenv"
)
type JWT struct {
	Secret string `yaml:"secret" env:"JWT_SECRET" env-default:"secret"`
	TTL    time.Duration `yaml:"ttl" env:"JWT_TTL" env-default:"24h"`
}

type Config struct {
	LogLevel string `yaml:"log_level" env:"LOG_LEVEL" env-default:"DEBUG"`
	Address      string `yaml:"user_address" env:"USER_ADDRESS" env-default:"localhost:80"`
	DBAddress    string `yaml:"db_address" env:"DB_ADDRESS" env-default:"localhost:82"`
	JWT JWT `yaml:"jwt"`
}

func MustLoad(configPath string) Config {
	var cfg Config
	if err := cleanenv.ReadConfig(configPath, &cfg); err != nil {
		if err := cleanenv.ReadEnv(&cfg); err != nil {
			log.Fatalf("cannot read config %q: %s", configPath, err)
		}
	}
	return cfg
}