CREATE OR REPLACE FUNCTION bmbt_po2_int(bm text, bt_name text) RETURNS integer AS $$
DECLARE
  id_val integer;
BEGIN
  FOR id_val IN EXECUTE ('SELECT * FROM ' || bm) LOOP
    EXECUTE '
     SELECT DISTINCT T2.gid
     FROM 
       ' || bt_name || ' T1, ' || bt_name || ' T2,
       (VALUES (1), (2), (3), (4), (5), (6), (7), (8), (9), (10), (11), (12), (13),
               (14), (15), (16), (17), (18), (19), (20), (21), (22), (23), (24), (25)) AS V(n)
     WHERE 
       T1.gid = ' || id_val || ' AND
       T1.block % 2 != 0 AND
       ((T1.block & 63) >> 1) >= V.n AND
        T2.block >= ((T1.block & ~((1 << ((31 - V.n) - 1)) - 1)) | (V.n<<1)) AND
        T2.block <= ((T1.block & ~((1 << ((31 - V.n) - 1)) - 1)) | ((V.n<<1)+1));';
  END LOOP;
  RETURN 1;
END;
$$ LANGUAGE plpgsql;
