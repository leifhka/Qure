\set id_val 13
\set geo_name geo.osm_no
\set bt_name qure.osm_no_d20_k3_cwl
\set ov_name osmppov
\set len_s 7
\set len_v 56
\set len_b ((:len_s + :len_v)+1)
\set lh (1 << :len_s)-1
\timing

SELECT count(block) AS number_of_blocks
FROM :bt_name
WHERE gid = :id_val;

SELECT COUNT(DISTINCT T2.gid)
FROM :bt_name T1, :bt_name T2
WHERE 
  T1.gid = :id_val AND
  T2.block % 2 != 0 AND
  T1.block & -2 <= T2.block AND
  T2.block <= (((1::bigint << ((:len_b - 1) - ((T1.block & :lh)::int >> 1))) - 1) | T1.block);

--EXPLAIN ANALYZE
--SELECT count(*) FROM
--(
SELECT COUNT(DISTINCT T2.gid)
FROM :geo_name T1, :geo_name T2
WHERE T1.gid = :id_val AND ST_covers(T1.geom, T2.geom);
--) T;
