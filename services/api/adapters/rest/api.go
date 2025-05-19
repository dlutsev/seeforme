package rest

import (
	"encoding/json"
	"fmt"
	"log/slog"
	"net/http"
	"seeforme/api/core"
)

func NewLoginHandler(log *slog.Logger, userservice core.User) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		email := r.FormValue("email")
		password := r.FormValue("password")

		if email == "" || password == "" {
			w.WriteHeader(http.StatusBadRequest)
			fmt.Fprint(w, core.ErrbadArguments.Error())
			return
		}

		role, token, err := userservice.Login(r.Context(), email, password)
		if err != nil {
			log.Error("failed to login user", "error", err)
			w.WriteHeader(http.StatusInternalServerError)
			fmt.Fprint(w, err.Error())
			return
		}

		response := map[string]interface{}{
			"token": token,
			"role":  role,
		}

		err = json.NewEncoder(w).Encode(response)
		if err != nil {
			log.Error("failed to encode response", "error", err)
			return
		}
	}
}

func NewRegisterHandler(log *slog.Logger, userservice core.User) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		email := r.FormValue("email")
		password := r.FormValue("password")
		role := r.FormValue("role") == "true"

		if email == "" || password == "" {
			w.WriteHeader(http.StatusBadRequest)
			fmt.Fprint(w, core.ErrbadArguments.Error())
			return
		}

		userID, err := userservice.Register(r.Context(), email, password, role)
		if err != nil {
			log.Error("failed to register user", "error", err)
			w.WriteHeader(http.StatusInternalServerError)
			fmt.Fprint(w, err.Error())
			return
		}

		response := map[string]interface{}{
			"userId": userID,
		}

		err = json.NewEncoder(w).Encode(response)
		if err != nil {
			log.Error("failed to encode response", "error", err)
			return
		}
	}
}

func NewCheckJWTHandler(log *slog.Logger, userservice core.User) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		token := r.Header.Get("Authorization")
		if token == "" {
			w.WriteHeader(http.StatusBadRequest)
			fmt.Fprint(w, core.ErrbadArguments.Error())
			return
		}

		err := userservice.CheckJWT(r.Context(), 0, token)
		if err != nil {
			log.Error("failed to check jwt", "error", err)
			w.WriteHeader(http.StatusInternalServerError)
			fmt.Fprint(w, err.Error())
			return
		}

		w.WriteHeader(http.StatusOK)
	}
}