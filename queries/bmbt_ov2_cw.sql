CREATE OR REPLACE FUNCTION bmbt_ov2_cw(bm text, bt_name text) RETURNS integer AS $$
DECLARE
  id_val integer;
BEGIN
  FOR id_val IN EXECUTE ('SELECT * FROM ' || bm) LOOP
    EXECUTE '
     SELECT DISTINCT T2.gid, T3.gid
     FROM 
      (
       VALUES (1), (2), (3), (4), (5), (6), (7), (8), (9), (10), (11), (12), (13),
              (14), (15), (16), (17), (18), (19), (20), (21), (22), (23), (24), (25),
              (26), (27), (28), (29), (30), (31), (32), (33), (34), (35), (36), (37),
              (38), (39), (40), (41), (42), (43), (44), (45), (46), (47), (48), (49),
              (50), (51), (52), (53), (54)
      ) AS V(n),
      (
       SELECT DISTINCT B2.gid AS gid, GREATEST(B1.block, B2.block) AS block
       FROM ' || bt_name || ' AS B1, ' || bt_name || ' AS B2,
         (VALUES (1), (2), (3), (4), (5), (6), (7), (8), (9), (10), (11), (12), (13),
                 (14), (15), (16), (17), (18), (19), (20), (21), (22), (23), (24), (25),
                 (26), (27), (28), (29), (30), (31), (32), (33), (34), (35), (36), (37),
                 (38), (39), (40), (41), (42), (43), (44), (45), (46), (47), (48), (49),
                 (50), (51), (52), (53), (54)) AS V(n)
       WHERE B1.gid = ' || id_val || ' AND 
             ((B1.block & -2 <= B2.block AND
               (((1::bigint << (63 - ((B1.block & 127)::int >> 1)) - 1) | B1.block) >= B2.block)) OR
              (((B1.block & 127) >> 1) >= V.n AND
               B2.block >= ((B1.block & ~((1::bigint << ((64 - V.n) - 1)) - 1)) | (V.n<<1)) AND
               B2.block <= ((B1.block & ~((1::bigint << ((64 - V.n) - 1)) - 1)) | ((V.n<<1)+1))) OR
               B2.block = 0)
     ) AS T2,
     (
      SELECT DISTINCT B2.gid AS gid, GREATEST(B1.block, B2.block) AS block
      FROM ' || bt_name || ' AS B1, ' || bt_name || ' AS B2,
        (VALUES (1), (2), (3), (4), (5), (6), (7), (8), (9), (10), (11), (12), (13),
                (14), (15), (16), (17), (18), (19), (20), (21), (22), (23), (24), (25),
                (26), (27), (28), (29), (30), (31), (32), (33), (34), (35), (36), (37),
                (38), (39), (40), (41), (42), (43), (44), (45), (46), (47), (48), (49),
                (50), (51), (52), (53), (54)) AS V(n)
       WHERE B1.gid = ' || id_val || ' AND 
             ((B1.block & -2 <= B2.block AND
               (((1::bigint << (63 - ((B1.block & 127)::int >> 1)) - 1) | B1.block) >= B2.block)) OR
              (((B1.block & 127) >> 1) >= V.n AND
               B2.block >= ((B1.block & ~((1::bigint << ((64 - V.n) - 1)) - 1)) | (V.n<<1)) AND
               B2.block <= ((B1.block & ~((1::bigint << ((64 - V.n) - 1)) - 1)) | ((V.n<<1)+1))) OR
               B2.block = 0)
     ) AS T3
    WHERE ((T2.block & -2 <= T3.block AND
            (((1::bigint << (63 - ((T2.block & 127)::int >> 1)) - 1) | T2.block) >= T3.block)) OR
           (((T2.block & 127) >> 1) >= V.n AND
            T3.block >= ((T2.block & ~((1::bigint << ((64 - V.n) - 1)) - 1)) | (V.n<<1)) AND
            T3.block <= ((T2.block & ~((1::bigint << ((64 - V.n) - 1)) - 1)) | ((V.n<<1)+1))) OR
            T3.block = 0)

';
  END LOOP;
  RETURN 1;
END;
$$ LANGUAGE plpgsql;
