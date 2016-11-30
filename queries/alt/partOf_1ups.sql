\set id_val 13
\set geo_name geo.osm_no
\set bt_name qure.osm_no_d15_k3_bc30_ms10_ups
\set len_s 6
\set len_v 25
\set len_b ((:len_s + :len_v)+1)
\set lh (1 << :len_s)-1
\timing

SELECT count(block) AS number_of_blocks
FROM :bt_name
WHERE gid = :id_val;

WITH
  possible AS (
    SELECT DISTINCT T2.gid, T2.block
    FROM :bt_name T2, :bt_name T1
    WHERE
      T1.gid = :id_val AND
      T2.block % 2 != 0 AND
      T1.block & -2 <= T2.block AND
      T2.block <= (((1 << ((:len_b - 1) - ((T1.block & :lh) >> 1))) - 1) | T1.block)
  )
SELECT count(DISTINCT P.gid)
FROM possible P
WHERE NOT EXISTS
 (SELECT 1
  FROM :bt_name T
  WHERE P.gid = T.gid AND T.block % 2 != 0 AND T.block NOT IN (SELECT block FROM possible WHERE gid=P.gid));

EXPLAIN ANALYZE
WITH
  possible AS (
    SELECT DISTINCT T2.gid, T2.block
    FROM :bt_name T2, :bt_name T1
    WHERE
      T1.gid = :id_val AND
      T2.block % 2 != 0 AND
      T1.block & -2 <= T2.block AND
      T2.block <= (((1 << ((:len_b - 1) - ((T1.block & :lh) >> 1))) - 1) | T1.block)
  ),
  rem AS (
    SELECT P.gid
    FROM (SELECT P.gid, T.block FROM possible P, :bt_name T WHERE P.gid = T.gid AND P.block % 2 != 0) AS P
    LEFT OUTER JOIN possible B ON (P.gid = B.gid AND P.block = B.block)
    WHERE B.block IS NULL
  )
SELECT count(DISTINCT P.gid)
FROM possible AS P LEFT OUTER JOIN rem R ON (P.gid = R.gid)
WHERE R.gid IS NULL; --gid NOT IN (SELECT gid FROM rem);


SELECT count(DISTINCT T2.gid)
FROM :geo_name T1, :geo_name T2
WHERE T1.gid = :id_val AND ST_covers(T1.geom, T2.geom);
