\timing on
EXPLAIN ANALYZE
SELECT DISTINCT v0, v1
FROM (
  SELECT DISTINCT T0.gid AS v0, T1.gid AS v1, T0.role
  FROM qure.osm_ice_d13_RCC8_bc30_ms10_wnr AS T0, qure.osm_ice_d13_RCC8_bc30_ms10_wnr AS T1,
    qure.bitPositionInt AS V
  WHERE T1.gid = 41359 AND 
  (T1.role = 2 OR T1.role = 3) AND
   ((T0.block > (T1.block & (T1.block-1)) AND 
     T0.block <= (T1.block | (T1.block-1))) OR 
    (T1.block != T1.block & ~(V.v-1) AND
     T0.block = ((T1.block & ~(V.v-1)) | V.v)))
) T
WHERE
  (role = 2 OR role = 3)
