\set id_val 9080542
\set geo_name divvy
\set bt_name qure.divvy_d18_k3_bc30_ms10_f3
\set len_s 7
\set len_v 56
\set len_b ((:len_s + :len_v)+1)
\set lh (1 << :len_s)-1
\timing

SELECT count(block) AS number_of_blocks
FROM :bt_name
WHERE gid = :id_val;

SELECT count(DISTINCT T2.gid)
FROM :bt_name AS T1, :bt_name AS T2,
     (VALUES (1), (2), (3), (4), (5), (6), (7), (8), (9), (10), (11), (12), (13),
             (14), (15), (16), (17), (18), (19), (20), (21), (22), (23), (24), (25),
             (26), (27), (28), (29), (30), (31), (32), (33), (34), (35), (36), (37),
             (38), (39), (40), (41), (42), (43), (44), (45), (46), (47), (48), (49),
             (50), (51), (52), (53), (54), (55), (56)) AS V(n)
WHERE T1.gid = :id_val AND 
      ((T1.block & -2 <= T2.block AND
        (((1::bigint << ((:len_b - 1) - ((T1.block & :lh)::int >> 1)) - 1) | T1.block) >= T2.block)) OR
       (((T1.block & :lh) >> 1) >= V.n AND
        T2.block >= ((T1.block & ~((1::bigint << ((:len_b - V.n) - 1)) - 1)) | (V.n<<1)) AND
        T2.block <= ((T1.block & ~((1::bigint << ((:len_b - V.n) - 1)) - 1)) | ((V.n<<1)+1))) OR
        T2.block = 0);

SELECT count(DISTINCT T2.trip_id)
FROM :geo_name AS T1, :geo_name AS T2 
WHERE T1.trip_id = :id_val AND 
      ((T1.starttime, T1.stoptime) OVERLAPS (T2.starttime, T2.stoptime)
       OR T1.stoptime = T2.starttime
       OR T2.stoptime = T1.starttime);
