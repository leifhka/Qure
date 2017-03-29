\set id_val 4
\set geo_name geo.dallas
\set bt_name qure.dallas_d13_k3_bc30_ms10_ns
\set len_s 6
\set len_v 25
\set len_b ((:len_s + :len_v)+1)
\set lh (1 << :len_s)-1
\timing

SELECT count(block) AS number_of_blocks
FROM :bt_name
WHERE gid = :id_val;


--SELECT COUNT(DISTINCT T2.gid)
SELECT DISTINCT T2.gid
FROM :bt_name AS T1, :bt_name AS T2,
     (VALUES (1), (2), (3), (4), (5), (6), (7), (8), (9), (10), (11), (12), (13),
             (14), (15), (16), (17), (18), (19), (20), (21), (22), (23), (24), (25)) AS V(n)
WHERE T1.gid = :id_val AND 
      ((T1.block & -2 <= T2.block AND
        ((((1 << ((:len_b - 1) - ((T1.block & :lh) >> 1))) - 1) | T1.block) >= T2.block)) OR
       (((T1.block & :lh) >> 1) >= V.n AND
        T2.block >= ((T1.block & ~((1 << ((:len_b - V.n) - 1)) - 1)) | (V.n<<1)) AND
        T2.block <= ((T1.block & ~((1 << ((:len_b - V.n) - 1)) - 1)) | ((V.n<<1)+1))) OR
       T2.block = 0);

SELECT DISTINCT T2.gid --COUNT(DISTINCT T2.gid) 
FROM :geo_name AS T1, :geo_name AS T2 
WHERE T1.gid = :id_val AND 
      ST_intersects(T1.geom, T2.geom);
