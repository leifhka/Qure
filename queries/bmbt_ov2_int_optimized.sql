CREATE OR REPLACE FUNCTION bmbt_ov2_int_optimized(bm text, bt_name text) RETURNS integer AS $$
DECLARE
  id_val integer;
BEGIN
  FOR id_val IN EXECUTE ('SELECT * FROM ' || bm) LOOP
    EXECUTE '
	  WITH ov AS (
        SELECT DISTINCT B2.gid AS gid, GREATEST(B1.block, B2.block) AS block
        FROM ' || bt_name || ' AS B1, ' || bt_name || ' AS B2,
          (VALUES (1), (2), (3), (4), (5), (6), (7), (8), (9), (10), (11), (12), (13),
                  (14), (15), (16), (17), (18), (19), (20), (21), (22), (23), (24), (25)) AS V(n)
        WHERE B1.gid = ' || id_val || ' AND 
              ((B1.block & -2 <= B2.block AND
                (((1 << (31 - ((B1.block & 63) >> 1)) - 1) | B1.block) >= B2.block)) OR
               (((B1.block & 63) >> 1) >= V.n AND
                B2.block >= ((B1.block & ~((1 << ((32 - V.n) - 1)) - 1)) | (V.n<<1)) AND
                B2.block <= ((B1.block & ~((1 << ((32 - V.n) - 1)) - 1)) | ((V.n<<1)+1))) OR
                B2.block = 0)
        )
      SELECT DISTINCT T2.gid, T3.gid
      FROM 
       (
        VALUES (1), (2), (3), (4), (5), (6), (7), (8), (9), (10), (11), (12), (13),
               (14), (15), (16), (17), (18), (19), (20), (21), (22), (23), (24), (25)
       ) AS V(n),
       ov AS T2,
       ov AS T3
     WHERE T2.gid != ' || id_val || ' AND T3.gid != ' || id_val || ' AND T2.gid != T3.gid AND
           ((T2.block & -2 <= T3.block AND
             (((1 << (31 - ((T2.block & 63) >> 1)) - 1) | T2.block) >= T3.block)) OR
            (((T2.block & 63) >> 1) >= V.n AND
             T3.block >= ((T2.block & ~((1 << ((32 - V.n) - 1)) - 1)) | (V.n<<1)) AND
             T3.block <= ((T2.block & ~((1 << ((32 - V.n) - 1)) - 1)) | ((V.n<<1)+1))) OR
             T3.block = 0);
    ';
  END LOOP;
  RETURN 1;
END;
$$ LANGUAGE plpgsql;
