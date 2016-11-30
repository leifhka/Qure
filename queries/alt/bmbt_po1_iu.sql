CREATE OR REPLACE FUNCTION bmbt_po1_iu(bm text, bt_name text) RETURNS integer AS $$
DECLARE
  id_val integer;
BEGIN
  FOR id_val IN EXECUTE ('SELECT * FROM ' || bm) LOOP
    EXECUTE '
     WITH
       possible AS (
         SELECT DISTINCT T2.gid, T2.block
         FROM ' || bt_name || ' T2, ' || bt_name || ' T1
         WHERE
           T1.gid = ' || id_val || ' AND
           T2.block % 2 != 0 AND
           T1.block & -2 <= T2.block AND
           T2.block <= (((1 << (31 - ((T1.block & 63) >> 1))) - 1) | T1.block)
       ),
       pbs AS (
         SELECT P.gid, T.block
         FROM possible P, ' || bt_name || ' T
         WHERE P.gid = T.gid AND T.block % 2 != 0
       ),
       rem AS (
         SELECT P.gid
         FROM pbs AS P LEFT OUTER JOIN possible B ON (P.gid = B.gid AND P.block = B.block)
         WHERE B.block IS NULL
       )
     SELECT count(DISTINCT P.gid)
     FROM possible AS P LEFT OUTER JOIN rem R ON (P.gid = R.gid)
     WHERE R.gid IS NULL;';
  END LOOP;
  RETURN 1;
END;
$$ LANGUAGE plpgsql;
