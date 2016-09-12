CREATE OR REPLACE FUNCTION bmbt_ov2_w(bm text, bt_name text) RETURNS integer AS $$
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
  FOR id_val IN EXECUTE ('SELECT * FROM ' || bm) LOOP
    EXECUTE '
     SELECT DISTINCT T2.gid, T3.gid
     FROM 
      (
       VALUES (1), (2), (3), (4), (5), (6), (7), (8), (9), (10), (11), (12), (13),
              (14), (15), (16), (17), (18), (19), (20), (21), (22), (23), (24), (25),
              (26), (27), (28), (29), (30), (31), (32), (33), (34), (35), (36), (37),
              (38), (39), (40), (41), (42), (43), (44), (45), (46), (47), (48), (49),
              (50), (51), (52), (53), (54), (55), (56)
      ) AS V(n),
      (
       SELECT DISTINCT B2.gid AS gid, GREATEST(B1.block, B2.block) AS block
       FROM ' || bt_name || ' AS B1, ' || bt_name || ' AS B2,
         (VALUES (1), (2), (3), (4), (5), (6), (7), (8), (9), (10), (11), (12), (13),
                 (14), (15), (16), (17), (18), (19), (20), (21), (22), (23), (24), (25),
                 (26), (27), (28), (29), (30), (31), (32), (33), (34), (35), (36), (37),
                 (38), (39), (40), (41), (42), (43), (44), (45), (46), (47), (48), (49),
                 (50), (51), (52), (53), (54), (55), (56)) AS V(n)
       WHERE B1.gid = ' || id_val || ' AND 
             ((B1.block <= B2.block AND
               (((1::bigint << ((' || len_b || ' - 1) - (B1.block & ' || lh ||')::int)) - 1) | B1.block) >= B2.block) OR
              ((B1.block & ' || lh ||') >= V.n AND
               B2.block = ((B1.block & ~((1::bigint << ((' || len_b || ' - V.n) - 1)) - 1)) | V.n)) OR
              B2.block = 0)
     ) AS T2,
     (
      SELECT DISTINCT B2.gid AS gid, GREATEST(B1.block, B2.block) AS block
      FROM ' || bt_name || ' AS B1, ' || bt_name || ' AS B2,
        (VALUES (1), (2), (3), (4), (5), (6), (7), (8), (9), (10), (11), (12), (13),
                (14), (15), (16), (17), (18), (19), (20), (21), (22), (23), (24), (25),
                (26), (27), (28), (29), (30), (31), (32), (33), (34), (35), (36), (37),
                (38), (39), (40), (41), (42), (43), (44), (45), (46), (47), (48), (49),
                (50), (51), (52), (53), (54), (55), (56)) AS V(n)
      WHERE B1.gid = ' || id_val || ' AND 
            ((B1.block <= B2.block AND
              (((1::bigint << ((' || len_b || ' - 1) - (B1.block & ' || lh ||')::int)) - 1) | B1.block) >= B2.block) OR
             ((B1.block & ' || lh ||') >= V.n AND
              B2.block = ((B1.block & ~((1::bigint << ((' || len_b || ' - V.n) - 1)) - 1)) | V.n)) OR
             B2.block = 0)
     ) AS T3
    WHERE ((T3.block <= T2.block AND
            (((1::bigint << ((' || len_b || ' - 1) - (T3.block & ' || lh ||')::int)) - 1) | T3.block) >= T2.block) OR
           ((T3.block & ' || lh ||') >= V.n AND
            T2.block = ((T3.block & ~((1::bigint << ((' || len_b || ' - V.n) - 1)) - 1)) | V.n)) OR
           T2.block = 0)';
  END LOOP;
  RETURN 1;
END;
$$ LANGUAGE plpgsql;
