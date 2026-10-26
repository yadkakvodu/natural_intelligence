-- The dump was produced on an Aiven cluster and references these roles as owners.
-- They are created as NOLOGIN roles so that ALTER ... OWNER TO ... statements succeed.
SELECT 'CREATE ROLE avnadmin NOLOGIN' WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'avnadmin') \gexec
SELECT 'CREATE ROLE postgres NOLOGIN' WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'postgres') \gexec
SELECT 'CREATE ROLE _avnadmin_monitor NOLOGIN' WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = '_avnadmin_monitor') \gexec
