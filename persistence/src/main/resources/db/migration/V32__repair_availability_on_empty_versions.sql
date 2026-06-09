-- After V23/V24, item-level availability was attached to one version while the app
-- resolves the latest version by MAX(created_at). Some items ended up with windows
-- on an older version and none on newer empty versions.
--
-- For each item that has versions without availability, take the newest version that
-- still has rows and insert copies onto every version on that item with none.

WITH versions_with_counts AS (
    SELECT
        v.item_id,
        v.id         AS version_id,
        v.created_at,
        COUNT(a.id)  AS avail_count
    FROM version v
    LEFT JOIN availability a ON a.version_id = v.id
    GROUP BY v.item_id, v.id, v.created_at
),
source AS (
    SELECT DISTINCT ON (item_id)
        item_id,
        version_id AS source_version_id
    FROM versions_with_counts
    WHERE avail_count > 0
    ORDER BY item_id, created_at DESC, version_id DESC
),
targets AS (
    SELECT
        item_id,
        version_id AS target_version_id
    FROM versions_with_counts
    WHERE avail_count = 0
),
to_insert AS (
    SELECT
        t.target_version_id,
        a.weekday,
        a.start_time,
        a.end_time
    FROM targets t
    JOIN source s ON s.item_id = t.item_id
    JOIN availability a ON a.version_id = s.source_version_id
)
INSERT INTO availability (version_id, weekday, start_time, end_time)
SELECT
    ti.target_version_id,
    ti.weekday,
    ti.start_time,
    ti.end_time
FROM to_insert ti
WHERE NOT EXISTS (
    SELECT 1
    FROM availability existing
    WHERE existing.version_id = ti.target_version_id
      AND existing.weekday = ti.weekday
      AND existing.start_time = ti.start_time
      AND existing.end_time = ti.end_time
);
