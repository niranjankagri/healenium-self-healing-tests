-- Run once by Postgres when the volume is new: the schema hlm-backend expects (same as Healenium's db/sql/init.sql).
CREATE SCHEMA healenium AUTHORIZATION healenium_user;
GRANT USAGE ON SCHEMA healenium TO healenium_user;
