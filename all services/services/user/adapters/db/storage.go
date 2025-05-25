package db

import (
	"context"
	"log/slog"
	"seeforme/user/core"

	_ "github.com/jackc/pgx/v5/stdlib"
	"github.com/jmoiron/sqlx"
)

type DB struct {
	log *slog.Logger
	conn *sqlx.DB
}

func New(log *slog.Logger, address string) (*DB, error) {
	db, err := sqlx.Connect("pgx", address)
	if err != nil {
		log.Error("connection problem", "address", address, "error", err)
		return nil, err
	}

	return &DB{
		log: log, 
		conn: db,
	}, nil
}

func (d *DB) SaveUser(ctx context.Context, user core.User) (int64, error) {
	var query string
	if !user.Role { // Если роль false, сохраняем в таблицу blind
		query = `INSERT INTO blind (email, password) VALUES ($1, $2) RETURNING id`
	} else { // Если роль true, сохраняем в таблицу volunteer
		query = `INSERT INTO volunteer (email, password) VALUES ($1, $2) RETURNING id`
	}

	var id int64
	if err := d.conn.QueryRowContext(ctx, query, user.Email, user.Password).Scan(&id); err != nil {
		d.log.Error("failed to save user", "error", err)
		return 0, core.ErrSaveUser
	}

	return id, nil
}

func (d *DB) GetUserByEmail(ctx context.Context, email string) (core.User, error) {
	var user core.User

	queryBlind := `SELECT id, email, password FROM blind WHERE email = $1`
	err := d.conn.GetContext(ctx, &user, queryBlind, email)
	if err == nil {
		user.Role = true
		d.log.Debug("user found", "email", email, "role", user.Role)
		return user, nil
	}

	queryVolunteer := `SELECT id, email, password FROM volunteer WHERE email = $1`
	err = d.conn.GetContext(ctx, &user, queryVolunteer, email)
	if err == nil {
		user.Role = false
		d.log.Debug("user found", "email", email, "role", user.Role)
		return user, nil
	}

	return core.User{}, core.ErrUserNotFound
}

func (d *DB) GetUserByID(ctx context.Context, id int64) (core.User, error) {
	var user core.User
	query := `
		SELECT id, email, password FROM volunteer WHERE id = $1
		UNION
		SELECT id, email, password FROM blind WHERE id = $1`
	err := d.conn.GetContext(ctx, &user, query, id)
	if err != nil {
		d.log.Error("failed to get user", "id", id, "error", err)
		return core.User{}, core.ErrUserNotFound
	}

	return user, nil
}

func (d *DB) GetUsersCount(ctx context.Context) (int64, int64, error) {
	var volunteersCount, blindCount int64

	// Получаем количество волонтеров
	err := d.conn.GetContext(ctx, &volunteersCount, "SELECT COUNT(*) FROM volunteer")
	if err != nil {
		d.log.Error("failed to get volunteers count", "error", err)
		return 0, 0, err
	}

	// Получаем количество слепых
	err = d.conn.GetContext(ctx, &blindCount, "SELECT COUNT(*) FROM blind")
	if err != nil {
		d.log.Error("failed to get blind count", "error", err)
		return 0, 0, err
	}

	return volunteersCount, blindCount, nil
}