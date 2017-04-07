\set id_val 1
\set geo_name geo.small_physical_poly
\set bt_name qure.small_physical_poly_d15_k3_bc30_ms10_rcc8_fix3
\set len_r 3
\set len_s (5+:len_r)
\set len_b 31
\set lh ((1 << :len_s)-1)
\timing

select :len_s, :len_b, :lh;

SELECT count(block) AS number_of_blocks
FROM :bt_name
WHERE gid = :id_val;


SELECT COUNT(DISTINCT T2.gid)
--SELECT DISTINCT T2.gid
FROM :bt_name AS T1, :bt_name AS T2,
     (VALUES (1), (2), (3), (4), (5), (6), (7), (8), (9), (10), (11), (12), (13),
             (14), (15), (16), (17), (18), (19), (20), (21), (22)) AS V(n)
WHERE T1.gid = :id_val AND 
      ((T1.block & -2 <= T2.block AND
        ((((1 << (:len_b - ((T1.block & :lh) >> :len_r))) - 1) | T1.block) >= T2.block)) OR
       (((T1.block & :lh) >> :len_r) >= V.n AND
        T2.block >= ((T1.block & ~((1 << (:len_b - V.n)) - 1)) | (V.n << :len_r)) AND
        T2.block <= ((T1.block & ~((1 << (:len_b - V.n)) - 1)) | ((V.n << :len_r) + 1))) OR
       T2.block = 0);

SELECT COUNT(DISTINCT T2.gid)
--SELECT DISTINCT T2.gid 
FROM :geo_name AS T1, :geo_name AS T2 
WHERE T1.gid = :id_val AND 
      ST_intersects(T1.geom, T2.geom);
