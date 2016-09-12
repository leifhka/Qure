CREATE OR REPLACE FUNCTION bmbt_po2_w(bm text, bt_name text) RETURNS integer AS $$
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
    SELECT DISTINCT OV.gid 
    FROM
     (
      SELECT DISTINCT T2.gid 
      FROM ' || bt_name || ' AS T1, ' || bt_name || ' AS T2,
           (VALUES (1), (2), (3), (4), (5), (6), (7), (8), (9), (10), (11), (12), (13),
                   (14), (15), (16), (17), (18), (19), (20), (21), (22), (23), (24), (25),
                   (26), (27), (28), (29), (30), (31), (32), (33), (34), (35), (36), (37),
                   (38), (39), (40), (41), (42), (43), (44), (45), (46), (47), (48), (49),
                   (50), (51), (52), (53), (54), (55), (56)) AS V(n)
      WHERE T1.gid = ' || id_val || ' AND 
            ((T1.block <= T2.block AND
              (((1::bigint << ((' || len_b || ' - 1) - (T1.block & ' || lh ||')::int)) - 1) | T1.block) >= T2.block) OR
             ((T1.block & ' || lh ||') >= V.n AND
              T2.block = ((T1.block & ~((1::bigint << ((' || len_b || ' - V.n) - 1)) - 1)) | V.n)) OR
             T2.block = 0)
     ) AS OV
    WHERE NOT EXISTS
      (
       SELECT TID.block
       FROM ' || bt_name || ' AS TID
       WHERE TID.gid = ' || id_val || ' AND
             NOT EXISTS
             (
              SELECT TOV.block
              FROM ' || bt_name || ' AS TOV,
                   (VALUES (1), (2), (3), (4), (5), (6), (7), (8), (9), (10), (11), (12), (13),
                           (14), (15), (16), (17), (18), (19), (20), (21), (22), (23), (24), (25),
                           (26), (27), (28), (29), (30), (31), (32), (33), (34), (35), (36), (37),
                           (38), (39), (40), (41), (42), (43), (44), (45), (46), (47), (48), (49),
                           (50), (51), (52), (53), (54), (55), (56)) AS V(n)
              WHERE TOV.gid = OV.gid AND
                    (TID.block & ' || lh || ') >= V.n AND
                    TOV.block = ((TID.block & ~((1::bigint << ((' || len_b || ' - V.n) - 1)) - 1)) | V.n)
             )
      )';
  END LOOP;
  RETURN 1;
END;
$$ LANGUAGE plpgsql;
