CREATE OR REPLACE FUNCTION constructOverlaps2_prep(ov_name text, prep_name text) RETURNS integer AS $$
DECLARE
  id_val integer;
  len_s integer;
  len_v integer;
  len_b integer;
  lh integer;
BEGIN
  len_s := 6;
  len_v := 57;
  len_b := ((len_s + len_v)+1);
  lh := (1 << len_s)-1;
  FOR id_val IN EXECUTE ('SELECT DISTINCT gid1 FROM ' || prep_name) LOOP
    EXECUTE '
          INSERT INTO ' || ov_name || '
           (
            SELECT DISTINCT ' || id_val || ', T2.gid2, T3.gid2
            FROM 
             (
              VALUES (1), (2), (3), (4), (5), (6), (7), (8), (9), (10), (11), (12), (13),
                     (14), (15), (16), (17), (18), (19), (20), (21), (22), (23), (24), (25),
                     (26), (27), (28), (29), (30), (31), (32), (33), (34), (35), (36), (37),
                     (38), (39), (40), (41), (42), (43), (44), (45), (46), (47), (48), (49),
                     (50), (51), (52), (53), (54), (55), (56)
             ) AS V(n),
             ' || prep_name || ' AS T2,
             ' || prep_name || ' AS T3
            WHERE T2.gid1 = ' || id_val || ' AND T3.gid1 = ' || id_val || ' AND
                  ((T2.block <= T3.block AND
                   (((1::bigint << ((' || len_b || ' - 1) - (T2.block & ' || lh || ')::int)) - 1) | T2.block) >= T3.block) OR
                  ((T2.block & ' || lh || ') >= V.n AND
                   T3.block = ((T2.block & ~((1::bigint << ((' || len_b || ' - V.n) - 1)) - 1)) | V.n)) OR
                  T3.block = 0)
          
           )';
  END LOOP;
  RETURN 1;
END;
$$ LANGUAGE plpgsql;
