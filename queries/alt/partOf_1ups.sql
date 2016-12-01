\set id_val 597913 --1742107 
\set geo_name geo.osm_no
\set bt_name qure.osm_no_d15_k3_bc30_ms10_ups2
\set len_s 6
\set len_v 25
\set len_b ((:len_s + :len_v)+1)
\set lh (1 << :len_s)-1
\timing

SELECT count(block) AS number_of_blocks
FROM :bt_name
WHERE gid = :id_val;

-- EXPLAIN
-- WITH
--   possible AS (
--     SELECT T2.gid, T1.block
--     FROM
--       :bt_name AS T2 INNER JOIN :bt_name AS T1 ON (
--          T1.gid = :id_val AND
--          T2.block % 2 != 0 AND
--          T1.block & -2 <= T2.block AND
--          T2.block <= (((1 << ((:len_b - 1) - ((T1.block & :lh) >> 1))) - 1) | T1.block)
--       )
--   ),
--   possibleR AS (
--     SELECT T2.gid, T1.block
--     FROM
--       :bt_name AS T2 LEFT OUTER JOIN :bt_name AS T1 ON (
--          T1.gid = :id_val AND
--          T2.block % 2 != 0 AND
--          T1.block & -2 <= T2.block AND
--          T2.block <= (((1 << ((:len_b - 1) - ((T1.block & :lh) >> 1))) - 1) | T1.block)
--       )
--   )
-- SELECT DISTINCT P.gid
-- FROM possible P
-- WHERE
--   NOT EXISTS (
--     SELECT 1
--     FROM possibleR R
--     WHERE R.gid = P.gid AND R.block IS NULL);
    
-- SELECT DISTINCT T2.gid
-- FROM :bt_name T2, :bt_name T1
-- WHERE
--   T1.gid = :id_val AND
--   T2.block % 2 != 0 AND
--   T1.block & -2 <= T2.block AND
--   T2.block <= (((1 << ((:len_b - 1) - ((T1.block & :lh) >> 1))) - 1) | T1.block) AND
--   NOT EXISTS
--    (SELECT 1
--     FROM :bt_name T
--     WHERE T.gid = T2.gid AND NOT EXISTS
--       (SELECT 1
--        FROM :bt_name TT
--        WHERE
--          TT.gid = :id_val AND
--          T.block % 2 != 0 AND
--          TT.block & -2 <= T.block AND
--          T.block <= (((1 << ((:len_b - 1) - ((TT.block & :lh) >> 1))) - 1) | TT.block)));

SELECT count(DISTINCT T2.gid)
FROM :bt_name T2, :bt_name T1
WHERE
  T1.gid = :id_val AND
  T2.block % 2 != 0 AND
  T1.block & -2 <= T2.block AND
  T2.block <= (((1 << ((:len_b - 1) - ((T1.block & :lh) >> 1))) - 1) | T1.block);

--EXPLAIN ANALYZE
-- WITH
--   possible AS (
--     SELECT DISTINCT T2.gid, T2.block
--     FROM :bt_name T2, :bt_name T1
--     WHERE
--       T1.gid = :id_val AND
--       T2.block % 2 != 0 AND
--       T1.block & -2 <= T2.block AND
--       T2.block <= (((1 << ((:len_b - 1) - ((T1.block & :lh) >> 1))) - 1) | T1.block)
--   )
-- SELECT count(DISTINCT P.gid)
-- FROM possible P
-- WHERE NOT EXISTS
--  (SELECT 1
--   FROM :bt_name T
--   WHERE P.gid = T.gid AND T.block % 2 != 0 AND
--         NOT EXISTS (SELECT 1 FROM possible WHERE gid=P.gid AND block=T.block));
-- 
--with
--  possibleW as (
--    select distinct t2.gid, t1.block
--    from 
--      :bt_name t1, :bt_name t2,
--      (values (1), (2), (3), (4), (5), (6), (7), (8), (9), (10), (11), (12), (13),
--              (14), (15), (16), (17), (18), (19), (20), (21), (22), (23), (24), (25)) as v(n)
--    where 
--      t1.gid = P.gid and
--      t1.block % 2 != 0 and
--      (t1.block & :lh)>>1 >= v.n and
--      t2.block >= ((t1.block & ~((1 << ((:len_b - v.n) - 1)) - 1)) | (v.n<<1)) and
--      t2.block <= ((t1.block & ~((1 << ((:len_b - v.n) - 1)) - 1)) | ((v.n<<1)+1))
--    ),
--  rem as (
--    select b.gid
--    from (select t.block 
--          from :bt_name as t
--          where t.gid = P.gid and t.block % 2 != 0) as p
--    left outer join possible b on (p.block = b.block)
--    where b.block is null
--  )
--select count(distinct pW.gid)
--from possibleW as pW left outer join remW rW on (pW.gid = rW.gid)
--where rW.gid is null; --gid not in (select gid from rem);

-- EXPLAIN ANALYZE
-- WITH
--   possible AS (
--     SELECT DISTINCT T2.gid
--     FROM :bt_name T2, :bt_name T1
--     WHERE
--       T1.gid = :id_val AND
--       T2.block % 2 != 0 AND
--       T1.block & -2 <= T2.block AND
--       T2.block <= (((1 << ((:len_b - 1) - ((T1.block & :lh) >> 1))) - 1) | T1.block)
--   ),
--   idbs as (select block from :bt_name where gid = :id_val),
--   bs as (select distinct t.block from :bt_name t, possible p where t.gid = p.gid and t.block % 2 != 0),
--   rbs as (select t.block from bs t
--           where not exists
--            (select 1
--             from idbs I
--             where I.block & -2 <= t.block and
--                   t.block <= (((1 << ((:len_b - 1) - ((I.block & :lh) >> 1))) - 1) | I.block))),
--   rem as (
--     select p.gid
--     from (select distinct t.gid 
--           from possible P, :bt_name T, rbs R
--           where P.gid = T.gid and R.block = T.block) as p
--   )
-- SELECT count(DISTINCT P.gid)
-- FROM possible AS P LEFT OUTER JOIN rem R ON (P.gid = R.gid)
-- WHERE R.gid IS NULL;

 
--EXPLAIN ANALYZE
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
    FROM (SELECT P.gid, T.block 
          FROM :bt_name AS T, (select distinct gid from possible) AS P
          WHERE T.block % 2 != 0 AND P.gid = T.gid) AS P
    LEFT OUTER JOIN possible B ON (P.gid = B.gid AND P.block = B.block)
    WHERE B.block IS NULL
  )
SELECT count(DISTINCT P.gid)
FROM possible AS P LEFT OUTER JOIN rem R ON (P.gid = R.gid)
WHERE R.gid IS NULL; --gid NOT IN (SELECT gid FROM rem);


SELECT count(DISTINCT T2.gid)
FROM :geo_name T1, :geo_name T2
WHERE T1.gid = :id_val AND ST_covers(T1.geom, T2.geom);
