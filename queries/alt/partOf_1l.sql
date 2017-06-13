\set id_val 5
\set geo_name geo.tiny
\set bt_name qure.tiny_d1_simple3_bc1_ms10_cg
\set len_s 6
\set len_v 57
\set len_b ((:len_s + :len_v)+1)
\set lh ((1::bigint << :len_s)-1)
\timing

SELECT count(block) AS number_of_blocks
FROM :bt_name
WHERE gid = :id_val;

SELECT count(DISTINCT T2.gid)
FROM :bt_name T2, :bt_name T1
WHERE
  T1.gid = :id_val AND
  T2.block % 2 != 0 AND
  T1.block & -2 <= T2.block AND
  T2.block <= (((1::bigint << ((:len_b - 1) - ((T1.block & :lh)::int >> 1))) - 1) | T1.block);
 
--EXPLAIN ANALYZE
WITH
  possible AS (
    SELECT DISTINCT T2.gid, T2.block
    FROM :bt_name T2, :bt_name T1
    WHERE
      T1.gid = :id_val AND
      T2.block % 2 != 0 AND
      T1.block & (-2)::bigint <= T2.block AND
      T2.block <= (((1::bigint << ((:len_b - 1) - ((T1.block & :lh)::int >> 1))) - 1) | T1.block)
  ),
  bs AS (SELECT DISTINCT block FROM possible),
  rem AS (
    SELECT P.gid
    FROM (SELECT P.gid, T.block 
          FROM :bt_name AS T, (select distinct gid from possible) AS P
          WHERE T.block % 2 != 0 AND P.gid = T.gid) AS P
    LEFT OUTER JOIN bs B ON (P.block = B.block)
    WHERE B.block IS NULL
  )
SELECT count(DISTINCT P.gid)
FROM possible AS P LEFT OUTER JOIN rem R ON (P.gid = R.gid)
WHERE R.gid IS NULL; --gid NOT IN (SELECT gid FROM rem);


SELECT count(DISTINCT T2.gid)
FROM :geo_name T1, :geo_name T2
WHERE T1.gid = :id_val AND ST_covers(T1.geom, T2.geom);
