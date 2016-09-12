CREATE OR REPLACE FUNCTION bmgeo_ov2_w(bm text, geo_name text) RETURNS integer AS $$
DECLARE
  id_val integer;
BEGIN
  FOR id_val IN EXECUTE ('SELECT * FROM ' || bm) LOOP
    EXECUTE '
    SELECT DISTINCT T2.gid, T3.gid
    FROM ' || geo_name || ' AS T1, ' || geo_name || ' AS T2, ' || geo_name || ' AS T3 
    WHERE T1.gid = ' || id_val || ' AND 
        ST_intersects(T1.geom, T2.geom) AND
        ST_intersects(T1.geom, T3.geom) AND
        ST_intersects(T3.geom, ST_buffer(ST_intersection(T1.geom, T2.geom),0))';

    -- EXECUTE 'SELECT block FROM qure.osm_no_d15_k3 WHERE gid = ' || gid;
  END LOOP;
  RETURN 1;
END;
$$ LANGUAGE plpgsql;
