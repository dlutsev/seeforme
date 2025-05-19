package db

import (
	"embed"

	"github.com/golang-migrate/migrate/v4"
	"github.com/golang-migrate/migrate/v4/database/pgx"
	"github.com/golang-migrate/migrate/v4/source/iofs"
)

//go:embed migrations/*.sql
var migrationFiles embed.FS

func (db *DB) Migrate() error {
    db.log.Info("running migration")
    files, err := iofs.New(migrationFiles, "migrations")
    if err != nil {
        db.log.Error("failed to create migration source", "error", err)
        return err
    }
    entries, _ := migrationFiles.ReadDir("migrations")
    for _, entry := range entries {
        db.log.Info("found migration file", "name", entry.Name())
    }
    driver, err := pgx.WithInstance(db.conn.DB, &pgx.Config{})
    if err != nil {
        db.log.Error("failed to create driver", "error", err)
        return err
    }
    m, err := migrate.NewWithInstance("iofs", files, "pgx", driver)
    if err != nil {
        db.log.Error("failed to init migration instance", "error", err)
        return err
    }
    db.log.Info("applying migrations")
    err = m.Up()
    if err != nil {
        if err != migrate.ErrNoChange {
            db.log.Error("migration failed", "error", err)
            return err
        }
        db.log.Info("no new migrations to apply")
    } else {
        db.log.Info("migrations applied successfully")
    }
    db.log.Debug("migration finished")
    return nil
}
