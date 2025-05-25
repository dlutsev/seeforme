package core

import "errors"

var (
	ErrInvalidCredentials 		= errors.New("invalid credentials")
	ErrUserAlreadyExists  		= errors.New("user already exists")
	ErrUserNotFound       		= errors.New("user not found")
	ErrSaveUser           		= errors.New("failed to save user")
	ErrGetUser            		= errors.New("failed to get user")
	ErrFailedToComparePassword 	= errors.New("failed to compare password")
	ErrFailedGenerateToken 		= errors.New("failed to generate token")
)
