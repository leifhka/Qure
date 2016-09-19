CREATE OR REPLACE FUNCTION bmgeo_ov_w(bm text, geo_name text) RETURNS integer AS $$
DECLARE
  id_val integer;
BEGIN
  FOR id_val IN EXECUTE ('SELECT * FROM ' || bm) LOOP
    EXECUTE '
    SELECT DISTINCT T2.gid 
    FROM ' || geo_name || ' AS T1, ' || geo_name || ' AS T2 
    WHERE T1.gid = ' || id_val || ' AND 
          ST_intersects(T1.geom, T2.geom)';

    -- EXECUTE 'SELECT block FROM qure.osm_no_d15_k3 WHERE gid = ' || gid;
  END LOOP;
  RETURN 1;
END;
$$ LANGUAGE plpgsql;
