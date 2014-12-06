CREATE TABLE IF NOT EXISTS fingerprints (location TEXT, fingerprint TEXT);
CREATE UNIQUE INDEX IF NOT EXISTS fingerprint_unique ON fingerprints(location, fingerprint);

.exit
