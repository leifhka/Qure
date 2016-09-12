CREATE OR REPLACE FUNCTION blockContains(b1 bigint, b2 bigint) RETURNS boolean AS $$
  BEGIN
  RETURN b1 <= b2 AND
         (((1::bigint << (5 - (b1 & ((1 << 6)-1))::int)) - 1) | b1) >= b2;
  END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION blockContainedBy(b1 bigint, b2 bigint, v integer) RETURNS boolean AS $$
  BEGIN
  RETURN ((b1 & 6) >= v AND
          b2 = ((b1 & ~((1::bigint << ((64 - v) - 1)) - 1)) | v)) OR
         b2 = 0;
 END;
$$ LANGUAGE plpgsql;


