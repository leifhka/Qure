CREATE OR REPLACE FUNCTION bmbt_po2_iu(bm text, bt_name text) RETURNS integer AS $$
DECLARE
  id_val integer;
BEGIN
  FOR id_val IN EXECUTE ('SELECT * FROM ' || bm) LOOP
    EXECUTE '
     WITH
       possible AS (
         SELECT DISTINCT T2.gid, T1.block
         FROM 
           ' || bt_name || ' T1, ' || bt_name || ' T2,
           (VALUES (1), (2), (3), (4), (5), (6), (7), (8), (9), (10), (11), (12), (13),
                   (14), (15), (16), (17), (18), (19), (20), (21), (22), (23), (24), (25)) AS V(n)
         WHERE 
           T1.gid = ' || id_val || ' AND
           T1.block % 2 != 0 AND
           (T1.block & 63)>>1 >= V.n AND
           T2.block >= ((T1.block & ~((1 << ((31 - V.n) - 1)) - 1)) | (V.n<<1)) AND
           T2.block <= ((T1.block & ~((1 << ((31 - V.n) - 1)) - 1)) | ((V.n<<1)+1))
         ),
       rem AS (
         SELECT B.gid
         FROM (SELECT T.block 
               FROM ' || bt_name || ' AS T
               WHERE T.gid = ' || id_val || ' AND T.block % 2 != 0) AS P
         LEFT OUTER JOIN possible B ON (P.block = B.block)
         WHERE B.block IS NULL
       )
     SELECT DISTINCT P.gid
     FROM possible AS P LEFT OUTER JOIN rem R ON (P.gid = R.gid)
     WHERE R.gid IS NULL;';
  END LOOP;
  RETURN 1;
END;
$$ LANGUAGE plpgsql;
