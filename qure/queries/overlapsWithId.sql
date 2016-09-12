\set id_val 100
\set geo_name geo.osm_no
\set bt_name qure.osm_no_d20_k3_cw
\set ov_name osmppov
\set len_s 6
\set len_v 57
\set len_b ((:len_s + :len_v)+1)
\set lh (1 << :len_s)-1
\timing

SELECT count(block) AS number_of_blocks
FROM :bt_name
WHERE gid = :id_val;

--EXPLAIN ANALYZE
SELECT count(*) AS bintrees FROM
 (
  SELECT DISTINCT T2.gid 
  FROM :bt_name AS T1, :bt_name AS T2,
       (VALUES (1), (2), (3), (4), (5), (6), (7), (8), (9), (10), (11), (12), (13),
               (14), (15), (16), (17), (18), (19), (20), (21), (22), (23), (24), (25),
               (26), (27), (28), (29), (30), (31), (32), (33), (34), (35), (36), (37),
               (38), (39), (40), (41), (42), (43), (44), (45), (46), (47), (48), (49),
               (50), (51), (52), (53), (54), (55), (56)) AS V(n)
  WHERE T1.gid = :id_val AND 
        ((T1.block <= T2.block AND
          (((1::bigint << ((:len_b - 1) - (T1.block & :lh)::int)) - 1) | T1.block) >= T2.block) OR
         ((T1.block & :lh) >= V.n AND
          T2.block = ((T1.block & ~((1::bigint << ((:len_b - V.n) - 1)) - 1)) | V.n)) OR
         T2.block = 0)
 ) T;

--EXPLAIN ANALYZE
SELECT count(*) AS geometries FROM
(
  SELECT DISTINCT T2.gid 
  FROM :geo_name AS T1, :geo_name AS T2 
  WHERE T1.gid = :id_val AND 
        ST_intersects(T1.geom, T2.geom)
) T;

--  SELECT count(*) AS explicit FROM
--   (
--    SELECT DISTINCT gid2
--    FROM :ov_name
--    WHERE gid1 = :id_val
--   ) T;
