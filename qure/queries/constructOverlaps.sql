\set ov_name ex.npd_ov_k2
\set bt_name qure.npd_d15_k3_wb
\set len_s 6
\set len_v 57
\set len_b ((:len_s + :len_v)+1)
\set lh (1 << :len_s)-1
\timing

--EXPLAIN --ANALYZE
INSERT INTO :ov_name
 (
  SELECT DISTINCT T1.gid, T2.gid 
  FROM :bt_name AS T1, :bt_name AS T2,
       (VALUES (1), (2), (3), (4), (5), (6), (7), (8), (9), (10), (11), (12), (13),
               (14), (15), (16), (17), (18), (19), (20), (21), (22), (23), (24), (25),
               (26), (27), (28), (29), (30), (31), (32), (33), (34), (35), (36), (37),
               (38), (39), (40), (41), (42), (43), (44), (45), (46), (47), (48), (49),
               (50), (51), (52), (53), (54), (55), (56)) AS V(n)
  WHERE ((T1.block <= T2.block AND
          (((1::bigint << ((:len_b - 1) - (T1.block & :lh)::int)) - 1) | T1.block) >= T2.block) OR
         ((T1.block & :lh) >= V.n AND
          T2.block = ((T1.block & ~((1::bigint << ((:len_b - V.n) - 1)) - 1)) | V.n)) OR
         T2.block = 0)
 );


