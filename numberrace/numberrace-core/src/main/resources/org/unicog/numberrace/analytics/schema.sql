-- Estrutura do banco SQLite de eventos de interação do Number Race
-- Versão de armazenamento 1 (eventos no modelo 1.x). Especificação: persistencia-eventos.md
-- O arquivo JSON Lines de cada sessão é a fonte oficial dos dados; este banco é uma cópia
-- para consulta, preenchida durante o jogo (melhor esforço) e completada pela importação.

PRAGMA foreign_keys = ON;

-- Informações sobre o próprio banco
CREATE TABLE IF NOT EXISTS meta (
    key   TEXT PRIMARY KEY,
    value TEXT NOT NULL
);
INSERT OR IGNORE INTO meta (key, value) VALUES ('storage_version', '1');
INSERT OR IGNORE INTO meta (key, value) VALUES ('event_schema', '1.x');

-- Um registro por evento: a parte comum em colunas e o evento completo em JSON
CREATE TABLE IF NOT EXISTS event (
    event_id        TEXT    PRIMARY KEY,
    schema_version  TEXT    NOT NULL,
    event_type      TEXT    NOT NULL,
    timestamp       TEXT    NOT NULL,
    session_id      TEXT    NOT NULL,
    participant_id  TEXT    NOT NULL,
    sequence_number INTEGER NOT NULL CHECK (sequence_number >= 1),
    activity        TEXT,
    game_number     INTEGER CHECK (game_number IS NULL OR game_number >= 1),
    turn_number     INTEGER CHECK (turn_number IS NULL OR turn_number >= 1),
    event_json      TEXT    NOT NULL CHECK (json_valid(event_json)),
    UNIQUE (session_id, sequence_number)
);

CREATE INDEX IF NOT EXISTS idx_event_participant ON event (participant_id);
CREATE INDEX IF NOT EXISTS idx_event_type ON event (event_type);
