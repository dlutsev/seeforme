package core

import (
	"context"
	"errors"
	"log/slog"

	"golang.org/x/crypto/bcrypt"
)

type Userservice struct {
	log *slog.Logger
	db DB
	jwt JWT
}

func NewUserService(log *slog.Logger, db DB, jwt JWT) *Userservice {
	return &Userservice{log: log, db: db, jwt: jwt}
}

func (s *Userservice) Register(ctx context.Context, email string, password string, role bool) (int64, error) {
	_, err := s.db.GetUserByEmail(ctx, email)
	if !errors.Is(err, ErrUserNotFound) {
		s.log.Error("user already exists")
		return 0, ErrUserAlreadyExists
	}

	passwordHash, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.MinCost)
	if err != nil {
		s.log.Error("failed to hash password")
		return 0, err
	}

	userID, err := s.db.SaveUser(ctx, User{
		Email: email,
		Password: passwordHash, 
		Role: role,
	})
	if err != nil {
		s.log.Error("failed to save user")
		return 0, ErrSaveUser
	}

	s.log.Info("user registered", "id", userID)

	return userID, nil
}

func (s *Userservice) Login(ctx context.Context, email string, password string) (bool, string, error) {
	user, err := s.db.GetUserByEmail(ctx, email)
	if err != nil {
		if errors.Is(err, ErrUserNotFound) {
			s.log.Error("user not found")
			return false, "", ErrUserNotFound
		}
		s.log.Error("failed to get user")
		return false, "", ErrGetUser
	}

	if err = bcrypt.CompareHashAndPassword(user.Password, []byte(password)); err != nil {
		s.log.Error("failed to compare password")
		return user.Role, "", ErrFailedToComparePassword
	}

	s.log.Info("user logged in", "id", user.ID)

	token, err := s.jwt.GenerateToken(user) 
	if err != nil {
		s.log.Error("failed to generate token")
		return false, "", ErrFailedGenerateToken
	}

	return user.Role, token, nil
}

func (s *Userservice) CheckJWT(ctx context.Context, userID int64, token string) (bool, error) {
	user, err := s.db.GetUserByID(ctx, userID)
	if err != nil {
		if errors.Is(err, ErrUserNotFound) {
			s.log.Error("user not found")
			return false, ErrUserNotFound
		}
		s.log.Error("failed to get user")
		return false, ErrGetUser
	}

	success := s.jwt.VerifyToken(token, user)
	if !success {
		s.log.Error("failed to verify token")
		return false, ErrInvalidCredentials
	}

	return success,nil
}



