package core

type User struct {
	ID       int64
	Email    string
	Password []byte
	Role     bool
}