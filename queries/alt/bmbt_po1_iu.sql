CREATE OR REPLACE FUNCTION bmbt_po1_iu(bm text, bt_name text) RETURNS integer AS $$
DECLARE
  id_val integer;
BEGIN
  FOR id_val IN EXECUTE ('SELECT * FROM ' || bm) LOOP
    EXECUTE '
     WITH
       possible AS (
         SELECT T2.gid, T2.block
         FROM ' || bt_name || ' T2, ' || bt_name || ' T1
         WHERE
           T1.gid = ' || id_val || ' AND
           T2.block % 2 != 0 AND
           T1.block & -2 <= T2.block AND
           T2.block <= (((1 << (31 - ((T1.block & 63) >> 1))) - 1) | T1.block)
       ),
       ids AS (SELECT DISTINCT gid FROM possible),
       bs AS (SELECT DISTINCT block FROM possible),
       rem AS (
         SELECT DISTINCT P.gid
         FROM (SELECT P.gid, T.block 
               FROM ' || bt_name || ' AS T, ids AS P
               WHERE T.block % 2 != 0 AND T.gid = P.gid) AS P
         LEFT OUTER JOIN bs B ON (P.block = B.block)
         WHERE B.block IS NULL
       )
     SELECT DISTINCT P.gid
     FROM ids AS P LEFT OUTER JOIN rem R ON (P.gid = R.gid)
     WHERE R.gid IS NULL;';
  END LOOP;
  RETURN 1;
END;
$$ LANGUAGE plpgsql;
