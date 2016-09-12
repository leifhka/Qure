CREATE OR REPLACE FUNCTION bmbt_po1_cw(bm text, bt_name text) RETURNS integer AS $$
DECLARE
  id_val integer;
BEGIN
  FOR id_val IN EXECUTE ('SELECT * FROM ' || bm) LOOP
    EXECUTE '
     SELECT DISTINCT T2.gid
     FROM ' || bt_name || ' T1 , ' || bt_name || ' T2
     WHERE 
       T1.gid = ' || id_val || ' AND
       T2.block % 2 != 0 AND
       T1.block & (-2)::bigint <= T2.block AND
       T2.block <= (((1::bigint << (63 - ((T1.block & 127)::int >> 1))) - 1) | T1.block);';
  END LOOP;
  RETURN 1;
END;
$$ LANGUAGE plpgsql;
