package core

import "context"

type User interface {
	Login(ctx context.Context, email string, password string) (bool, string, error)
	Register(ctx context.Context, email string, password string, role bool) (int64, error)
	CheckJWT(ctx context.Context, userID int64, token string) error
}