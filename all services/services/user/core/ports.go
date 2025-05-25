package core

import "context"

type DB interface {
	GetUserByEmail(ctx context.Context, email string) (User, error)
	GetUserByID(ctx context.Context, id int64) (User, error)
	SaveUser(ctx context.Context, user User) (int64, error)
	GetUsersCount(ctx context.Context) (int64, int64, error) // returns (volunteers_count, blind_count, error)
}

type JWT interface {
	GenerateToken(user User) (string, error)
	VerifyToken(tokenString string, user User) bool
}

type UserService interface {
	Register(ctx context.Context, email string, password string, role bool) (int64, error)
	Login(ctx context.Context, email string, password string) (bool, string, error)
	CheckJWT(ctx context.Context, userID int64, token string) (bool, error)
	GetStatistics(ctx context.Context) (int64, int64, error) // returns (volunteers_count, blind_count, error)
}