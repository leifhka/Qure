CREATE OR REPLACE FUNCTION bmgeo_po1_w(bm text, geo_name text) RETURNS integer AS $$
DECLARE
  id_val integer;
BEGIN
  FOR id_val IN EXECUTE ('SELECT * FROM ' || bm) LOOP
    EXECUTE '
    SELECT DISTINCT T2.gid 
    FROM ' || geo_name || ' AS T1, ' || geo_name || ' AS T2 
    WHERE T1.gid = ' || id_val || ' AND 
          ST_covers(T1.geom, T2.geom)';
  END LOOP;
  RETURN 1;
END;
$$ LANGUAGE plpgsql;
