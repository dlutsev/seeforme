package jwt

import (
	"errors"
	"log/slog"
	"seeforme/user/core"
	"time"

	"github.com/golang-jwt/jwt"
)

type JWT struct {
	secret string
	ttl time.Duration
	log *slog.Logger
}

func New(secret string, ttl time.Duration, log *slog.Logger) *JWT {
	return &JWT{secret: secret, ttl: ttl, log: log}
}

func (j *JWT) GenerateToken(user core.User) (string, error) {
	token :=  jwt.New(jwt.SigningMethodHS256)

	claims := token.Claims.(jwt.MapClaims)
	claims["sub"] = user.ID
	claims["email"] = user.Email
	claims["role"] = user.Role
	claims["exp"] = time.Now().Add(j.ttl).Unix()

	tokenString, err := token.SignedString([]byte(j.secret))
	if err != nil {
		return "", core.ErrFailedGenerateToken
	}

	j.log.Debug("token generated", "token", tokenString)

	return tokenString, nil
}

func (j *JWT) VerifyToken(tokenString string, user core.User) bool {
    token, err := jwt.ParseWithClaims(tokenString, &jwt.MapClaims{}, func(token *jwt.Token) (interface{}, error) {
        if token.Method != jwt.SigningMethodHS256 {
            return nil, errors.New("unexpected signing method")
        }
        return []byte(j.secret), nil
    })
    if err != nil {
        return false
    }

    if claims, ok := token.Claims.(*jwt.MapClaims); ok && token.Valid {
        if exp, ok := (*claims)["exp"].(float64); ok {
            if time.Now().Unix() > int64(exp) {
                return false
            }
        } else {
            return false
        }

        if user.ID != int64((*claims)["id"].(float64)) || 
           user.Email != (*claims)["email"].(string) || 
           user.Role != (*claims)["role"].(bool) {
            return false
        }
        return true
    }
    return false
}
