package grpc

import (
	"context"
	"errors"

	userpb "seeforme/proto/user"
	"seeforme/user/core"

	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
	"google.golang.org/protobuf/types/known/emptypb"
)

type Server struct {
	userpb.UnimplementedUserServer
	userService core.UserService
}

func NewServer(service core.UserService) *Server {
	return &Server{
		userService: service,
	}
}

func (s *Server) Register(ctx context.Context, req *userpb.RegisterRequest) (*userpb.RegisterResponse, error) {
	email := req.GetEmail()
	password := req.GetPassword()
	role := req.GetRole()

	userID, err := s.userService.Register(ctx, email, password, role)
	if err != nil {
		if errors.Is(err, core.ErrUserAlreadyExists) {
			return nil, status.Error(codes.AlreadyExists, "user already exists")
		}
		return nil, status.Error(codes.Internal, "failed to register user")
	}

	return &userpb.RegisterResponse{UserId: userID}, nil
}

func (s *Server) Login(ctx context.Context, req *userpb.LoginRequest) (*userpb.LoginResponse, error) {
	email := req.GetEmail()
	password := req.GetPassword()

	role, token, err := s.userService.Login(ctx, email, password)
	if err != nil {
		if errors.Is(err, core.ErrUserNotFound) {
			return nil, status.Error(codes.NotFound, "invalid credentials")
		}
		return nil, status.Error(codes.Internal, "failed to login user")
	}

	return &userpb.LoginResponse{Token: token, Role: role}, nil
}

func (s *Server) CheckJWT(ctx context.Context, req *userpb.CheckJWTRequest) (*emptypb.Empty, error) {
	userID := req.GetUserId()
	token := req.GetToken()

	ok, err := s.userService.CheckJWT(ctx, userID, token)
	if err != nil {
		if errors.Is(err, core.ErrInvalidCredentials) {
			return nil, status.Error(codes.PermissionDenied, "invalid credentials")
		}
		if errors.Is(err, core.ErrUserNotFound) {
			return nil, status.Error(codes.NotFound, "user not found")
		}
		return nil, status.Error(codes.Internal, "failed to check jwt")
	}
	if !ok {
		return nil, status.Error(codes.PermissionDenied, "invalid credentials")
	}

	return &emptypb.Empty{}, nil

}