\set id_val 10
\set geo_name geo.tiny
\set bt_name qure.tiny_d1_simple2_bc1_ms10_cg2
\set len_s 6
\set len_v 57
\set len_b ((:len_s + :len_v)+1)
\set lh ((1::bigint << :len_s)-1)
\timing

 WITH ov AS (
   SELECT DISTINCT B2.gid AS gid, GREATEST(B1.block, B2.block) AS block
   FROM :bt_name AS B1, :bt_name AS B2,
     (VALUES (1), (2), (3), (4), (5), (6), (7), (8), (9), (10), (11), (12), (13),
             (14), (15), (16), (17), (18), (19), (20), (21), (22), (23), (24), (25),
             (26), (27), (28), (29), (30), (31), (32), (33), (34), (35), (36), (37),
             (38), (39), (40), (41), (42), (43), (44), (45), (46), (47), (48), (49),
             (50), (51), (52), (53), (54), (55), (56)) AS V(n)
   WHERE B1.gid = :id_val AND 
         ((B1.block & (-2)::bigint <= B2.block AND
           ((((1::bigint << ((:len_b-1) - ((B1.block & :lh)::int >> 1))) - 1) | B1.block) >= B2.block)) OR
          (((B1.block & :lh) >> 1) >= V.n AND
           B2.block >= ((B1.block & ~((1::bigint << ((:len_b - V.n) - 1)) - 1)) | (V.n<<1)) AND
           B2.block <= ((B1.block & ~((1::bigint << ((:len_b - V.n) - 1)) - 1)) | ((V.n<<1)+1))) OR
           B2.block = 0)
 ) 
 SELECT count(DISTINCT(T2.gid, T3.gid))
 FROM 
  (
   VALUES (1), (2), (3), (4), (5), (6), (7), (8), (9), (10), (11), (12), (13),
          (14), (15), (16), (17), (18), (19), (20), (21), (22), (23), (24), (25),
          (26), (27), (28), (29), (30), (31), (32), (33), (34), (35), (36), (37),
          (38), (39), (40), (41), (42), (43), (44), (45), (46), (47), (48), (49),
          (50), (51), (52), (53), (54), (55), (56)) AS V(n),
  ov AS T2,
  ov AS T3
WHERE T2.gid != :id_val AND T3.gid != :id_val AND T2.gid != T3.gid AND
      ((T2.block & (-2)::bigint <= T3.block AND
        ((((1::bigint << ((:len_b-1) - ((T2.block & :lh)::int >> 1))) - 1) | T2.block) >= T3.block)) OR
       (((T2.block & :lh) >> 1) >= V.n AND
        T3.block >= ((T2.block & ~((1::bigint << ((:len_b - V.n) - 1)) - 1)) | (V.n<<1)) AND
        T3.block <= ((T2.block & ~((1::bigint << ((:len_b - V.n) - 1)) - 1)) | ((V.n<<1)+1))) OR
        T3.block = 0);

 SELECT count(DISTINCT(T2.gid, T3.gid))
 FROM :geo_name T1, :geo_name T2, :geo_name T3
 WHERE T1.gid = :id_val AND T2.gid != T1.gid AND T3.gid != T1.gid AND T2.gid != T3.gid AND
       ST_intersects(T1.geom, T2.geom) AND
       ST_intersects(T1.geom, T3.geom) AND
       ST_intersects(T2.geom, ST_intersection(T1.geom, T3.geom));
