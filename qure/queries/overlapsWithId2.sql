\set id_val 12
\set geo_name geo.npd
\set bt_name qure.npd_d15_k3_wb
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
  SELECT DISTINCT T2.gid, T3.gid
  FROM :bt_name AS T3,
       (
        VALUES (1), (2), (3), (4), (5), (6), (7), (8), (9), (10), (11), (12), (13),
               (14), (15), (16), (17), (18), (19), (20), (21), (22), (23), (24), (25),
               (26), (27), (28), (29), (30), (31), (32), (33), (34), (35), (36), (37),
               (38), (39), (40), (41), (42), (43), (44), (45), (46), (47), (48), (49),
               (50), (51), (52), (53), (54), (55), (56)
        ) AS V(n),
        (
         SELECT DISTINCT B2.gid AS gid, GREATEST(B1.block, B2.block) AS block
         FROM :bt_name AS B1, :bt_name AS B2,
              (VALUES (1), (2), (3), (4), (5), (6), (7), (8), (9), (10), (11), (12), (13),
                      (14), (15), (16), (17), (18), (19), (20), (21), (22), (23), (24), (25),
                      (26), (27), (28), (29), (30), (31), (32), (33), (34), (35), (36), (37),
                      (38), (39), (40), (41), (42), (43), (44), (45), (46), (47), (48), (49),
                      (50), (51), (52), (53), (54), (55), (56)) AS W(n)
         WHERE B1.gid = :id_val AND 
               ((B1.block <= B2.block AND
                 (((1::bigint << ((:len_b - 1) - (B1.block & :lh)::int)) - 1) | B1.block) >= B2.block) OR
                ((B1.block & :lh) >= W.n AND
                 B2.block = ((B1.block & ~((1::bigint << ((:len_b - W.n) - 1)) - 1)) | W.n)) OR
                B2.block = 0)
        ) AS T2
    WHERE ((T2.block <= T3.block AND
            (((1::bigint << ((:len_b - 1) - (T2.block & :lh)::int)) - 1) | T2.block) >= T3.block) OR
           ((T2.block & :lh) >= V.n AND
            T3.block = ((T2.block & ~((1::bigint << ((:len_b - V.n) - 1)) - 1)) | V.n)) OR
           T3.block = 0)

 ) T;

--EXPLAIN ANALYZE
SELECT count(*) AS geometries FROM
 (
  SELECT DISTINCT T2.gid, T3.gid
  FROM :geo_name AS T1, :geo_name AS T2, :geo_name AS T3
  WHERE T1.gid = :id_val AND 
        ST_intersects(T1.geom, T2.geom) AND
        ST_intersects(T1.geom, T3.geom) AND
        ST_intersects(T2.geom, ST_intersection(T1.geom, T3.geom))
 ) T;

--  SELECT count(*) AS explicit FROM
--   (
--    SELECT DISTINCT gid2
--    FROM :ov_name
--    WHERE gid1 = :id_val
--   ) T;
