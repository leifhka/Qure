\set id_val 1763315 --124903 --1742107 --1377897 --597913 --
\set geo_name geo.osm_no
\set bt_name qure.osm_no_d15_k3_bc30_ms10_ups2
\set len_s 6
\set len_v 25
\set len_b ((:len_s + :len_v)+1)
\set lh (1 << :len_s)-1
\timing

SELECT count(block) AS number_of_blocks
FROM :bt_name
WHERE gid = :id_val;

--EXPLAIN ANALYZE
with
  possible as (
    select distinct t2.gid, t1.block
    from 
      :bt_name t1, :bt_name t2,
      (values (1), (2), (3), (4), (5), (6), (7), (8), (9), (10), (11), (12), (13),
              (14), (15), (16), (17), (18), (19), (20), (21), (22), (23), (24), (25)) as v(n)
    where 
      t1.gid = :id_val and
      t1.block % 2 != 0 and
      (t1.block & :lh)>>1 >= v.n and
      t2.block >= ((t1.block & ~((1 << ((:len_b - v.n) - 1)) - 1)) | (v.n<<1)) and
      t2.block <= ((t1.block & ~((1 << ((:len_b - v.n) - 1)) - 1)) | ((v.n<<1)+1))
    ),
  rem as (
    select b.gid
    from (select t.block 
          from :bt_name as t
          where t.gid = :id_val and t.block % 2 != 0) as p
    left outer join possible b on (p.block = b.block)
    where b.block is null
  )
select count(distinct p.gid)
from possible as p left outer join rem r on (p.gid = r.gid)
where r.gid is null; --gid not in (select gid from rem);

SELECT count(DISTINCT T2.gid)
FROM :geo_name T1, :geo_name T2
WHERE T1.gid = :id_val AND ST_coveredBy(T1.geom, T2.geom);
