package user

import (
	"context"
	"log/slog"

	userpb "seeforme/proto/user"

	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"
)

type Client struct {
	log *slog.Logger
	client userpb.UserClient
}

func NewClient(address string, log *slog.Logger) (*Client, error) {
	conn, err := grpc.NewClient(address, grpc.WithTransportCredentials(insecure.NewCredentials()))
	if err != nil {
		return nil, err
	}
	return &Client{
		log:    log,
		client: userpb.NewUserClient(conn),
	}, nil
}

func (c *Client) Register(ctx context.Context, email string, password string, role bool) (int64, error) {
	response, err := c.client.Register(ctx, &userpb.RegisterRequest{
		Email:    email,
		Password: password,
		Role:     role,
	})
	if err != nil {
		c.log.Error("failed to register user", "error", err)
		return 0, err
	}
	return response.GetUserId(), nil
}

func (c *Client) Login(ctx context.Context, email string, password string) (bool, string, error) {
	response, err := c.client.Login(ctx, &userpb.LoginRequest{
		Email:    email,
		Password: password,
	})
	if err != nil {
		c.log.Error("failed to login user", "error", err)
		return false, "", err
	}
	return response.GetRole(), response.GetToken(), nil
}

func (c *Client) CheckJWT(ctx context.Context, userID int64, token string) (error) {
	_, err := c.client.CheckJWT(ctx, &userpb.CheckJWTRequest{
		UserId: userID,
		Token:  token,
	})
	if err != nil {
		c.log.Error("failed to check jwt", "error", err)
		return err
	}
	return nil
}