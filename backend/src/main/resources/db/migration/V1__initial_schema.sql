CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE users (
  id UUID PRIMARY KEY, github_id BIGINT NOT NULL UNIQUE, github_username VARCHAR(255) NOT NULL UNIQUE,
  display_name VARCHAR(100) NOT NULL, avatar_url VARCHAR(255), access_token TEXT NOT NULL,
  token_scopes VARCHAR(500), created_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE git_repositories (
  id UUID PRIMARY KEY, user_id UUID NOT NULL REFERENCES users(id), github_repo_id BIGINT,
  owner VARCHAR(100) NOT NULL, name VARCHAR(100) NOT NULL, full_name VARCHAR(255) NOT NULL,
  default_branch VARCHAR(255), is_private BOOLEAN NOT NULL, html_url VARCHAR(512), description VARCHAR(2000),
  processing_status VARCHAR(32) NOT NULL, processing_error VARCHAR(2000), indexed_commit_sha VARCHAR(40),
  indexed_branch VARCHAR(255), files_discovered INTEGER NOT NULL, files_processed INTEGER NOT NULL,
  files_skipped INTEGER NOT NULL, files_failed INTEGER NOT NULL, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT uq_git_repositories_user_owner_name UNIQUE (user_id, owner, name)
);
CREATE TABLE repository_processing_jobs (
  id UUID PRIMARY KEY, repository_id UUID NOT NULL REFERENCES git_repositories(id), commit_sha VARCHAR(40) NOT NULL,
  branch_name VARCHAR(255) NOT NULL, status VARCHAR(32) NOT NULL, files_discovered INTEGER NOT NULL,
  files_processed INTEGER NOT NULL, files_skipped INTEGER NOT NULL, files_failed INTEGER NOT NULL,
  error_message VARCHAR(2000), created_at TIMESTAMPTZ NOT NULL, started_at TIMESTAMPTZ, completed_at TIMESTAMPTZ
);
CREATE TABLE repository_files (
  id UUID PRIMARY KEY, repository_id UUID NOT NULL REFERENCES git_repositories(id), commit_sha VARCHAR(40) NOT NULL,
  branch_name VARCHAR(255) NOT NULL, path VARCHAR(2048) NOT NULL, file_name VARCHAR(512) NOT NULL,
  extension VARCHAR(32), language VARCHAR(64), size_bytes BIGINT, blob_sha VARCHAR(40) NOT NULL,
  ingest_status VARCHAR(32) NOT NULL, error_message VARCHAR(1000), content TEXT,
  created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT uq_repository_files_commit_path UNIQUE (repository_id, commit_sha, path)
);
CREATE TABLE code_chunks (
  id UUID PRIMARY KEY, repository_id UUID NOT NULL REFERENCES git_repositories(id), repository_file_id UUID NOT NULL REFERENCES repository_files(id),
  commit_sha VARCHAR(40) NOT NULL, path VARCHAR(2048) NOT NULL, language VARCHAR(64), chunk_index INTEGER NOT NULL,
  content TEXT NOT NULL, start_line INTEGER NOT NULL, end_line INTEGER NOT NULL, symbol_name VARCHAR(512), chunk_type VARCHAR(64),
  imports_json TEXT, metadata_json TEXT, created_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT uq_code_chunks_file_index UNIQUE (repository_file_id, chunk_index)
);
CREATE INDEX idx_chunks_repository_commit ON code_chunks(repository_id, commit_sha);
CREATE INDEX idx_chunks_path ON code_chunks(repository_id, path);
CREATE TABLE code_chunk_embeddings (
  chunk_id UUID PRIMARY KEY REFERENCES code_chunks(id) ON DELETE CASCADE, embedding vector(1536) NOT NULL
);
CREATE INDEX idx_chunk_embeddings_cosine ON code_chunk_embeddings USING hnsw (embedding vector_cosine_ops);
CREATE TABLE conversations (
  id UUID PRIMARY KEY, repository_id UUID NOT NULL REFERENCES git_repositories(id), user_id UUID NOT NULL REFERENCES users(id),
  title VARCHAR(200) NOT NULL, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_conversations_repository_user ON conversations(repository_id, user_id);
CREATE TABLE chat_messages (
  id UUID PRIMARY KEY, conversation_id UUID NOT NULL REFERENCES conversations(id), role VARCHAR(16) NOT NULL,
  content TEXT NOT NULL, citations_json TEXT, created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_messages_conversation_created ON chat_messages(conversation_id, created_at);
