IF DB_ID(N'file_transfer_app') IS NULL
BEGIN
    CREATE DATABASE file_transfer_app;
END
GO

USE file_transfer_app;
GO

IF OBJECT_ID(N'dbo.transfer_log', N'U') IS NOT NULL DROP TABLE dbo.transfer_log;
IF OBJECT_ID(N'dbo.files', N'U') IS NOT NULL DROP TABLE dbo.files;
IF OBJECT_ID(N'dbo.users', N'U') IS NOT NULL DROP TABLE dbo.users;
GO

CREATE TABLE dbo.users (
  user_id       INT IDENTITY(1,1) PRIMARY KEY,
  username      NVARCHAR(50) NOT NULL UNIQUE,
  password_hash NVARCHAR(255) NOT NULL,
  full_name     NVARCHAR(100) NULL,
  status        NVARCHAR(20) NOT NULL CONSTRAINT DF_users_status DEFAULT N'ACTIVE',
  created_at    DATETIME2 NOT NULL CONSTRAINT DF_users_created_at DEFAULT SYSDATETIME(),
  CONSTRAINT CK_users_status CHECK (status IN (N'ACTIVE', N'BLOCKED'))
);
GO

CREATE TABLE dbo.files (
  file_id     INT IDENTITY(1,1) PRIMARY KEY,
  file_name   NVARCHAR(255) NOT NULL,
  file_size   BIGINT NOT NULL,
  sender_id   INT NOT NULL,
  receiver_id INT NOT NULL,
  server_path NVARCHAR(MAX) NOT NULL,
  sent_at     DATETIME2 NOT NULL CONSTRAINT DF_files_sent_at DEFAULT SYSDATETIME(),
  status      NVARCHAR(20) NOT NULL CONSTRAINT DF_files_status DEFAULT N'SENT',
  CONSTRAINT CK_files_status CHECK (status IN (N'SENT', N'RECEIVED', N'FAILED')),
  CONSTRAINT FK_files_sender FOREIGN KEY (sender_id) REFERENCES dbo.users(user_id),
  CONSTRAINT FK_files_receiver FOREIGN KEY (receiver_id) REFERENCES dbo.users(user_id)
);
GO

CREATE TABLE dbo.transfer_log (
  log_id      INT IDENTITY(1,1) PRIMARY KEY,
  action      NVARCHAR(30) NOT NULL,
  file_id     INT NULL,
  sender_id   INT NULL,
  receiver_id INT NULL,
  result      NVARCHAR(30) NOT NULL,
  message     NVARCHAR(MAX) NULL,
  created_at  DATETIME2 NOT NULL CONSTRAINT DF_log_created_at DEFAULT SYSDATETIME(),
  CONSTRAINT FK_log_file FOREIGN KEY (file_id) REFERENCES dbo.files(file_id),
  CONSTRAINT FK_log_sender FOREIGN KEY (sender_id) REFERENCES dbo.users(user_id),
  CONSTRAINT FK_log_receiver FOREIGN KEY (receiver_id) REFERENCES dbo.users(user_id)
);
GO
INSERT INTO dbo.users(username, password_hash, full_name, status)
VALUES
(N'admin', N'123456', N'Administrator', N'ACTIVE'),
(N'user1', N'123456', N'User One', N'ACTIVE'),
(N'user2', N'123456', N'User Two', N'ACTIVE');