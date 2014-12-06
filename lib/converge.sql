CREATE TABLE IF NOT EXISTS fingerprints (id integer primary key, location TEXT, fingerprint TEXT, timestamp INTEGER);
CREATE UNIQUE INDEX IF NOT EXISTS location_fingerprint ON fingerprints(location, fingerprint);

.exit
