CREATE
  TABLE
    users(
      id UUID PRIMARY KEY,
      version INT NOT NULL,
      created_at TIMESTAMP NOT NULL,
      username VARCHAR(50) NOT NULL,
      password VARCHAR(50) NOT NULL,
      email VARCHAR(255) NOT NULL
    );