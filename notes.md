
b1 = 0100110100000000
b2 = 0100110101100000

# bPartOf1(x, b):
  (b & (b-1)) <= x AND
  (b | (b-1)) >= x

# bPartOf2(b, x, v):
  x = ((b & ~(v-1)) | v)

# rangeQ():
  SELECT (1 << N.n) from values( (0) ... (block_size)) N(n)) as V.v
 
# tPartOf1approx(gid):

SELECT T1.gid, T1.block
FROM table T1,
     table T2,
WHERE T2.gid = gid AND
      T1.role & 1 != 0 AND
      bPartOf1(T1.block, T2.block);
 
# tPartOf1(gid):

WITH
  possible AS tPartOf1approx(gid),
  pbs AS (SELECT DISTINCT block FROM possible),
  pgs AS (SELECT DISTINCT gid FROM possible),
  rem AS (
    SELECT Pos.gid
    FROM (SELECT Pos.gid, T.block
          FROM table T, pgs Pos
          WHERE T.role & 1 != 0 AND Pos.gid = T.gid) AS Pos
    LEFT OUTER JOIN pbs B ON (Pos.block = B.block)
    WHERE B.block IS NULL
  )
SELECT DISTINCT P.gid
FROM pgs AS P LEFT OUTER JOIN rem AS R ON (P.gid = R.gid)
WHERE R.gid IS NULL;
