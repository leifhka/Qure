\set table_name geo.synth7
\set epsilon 0.001

select avg(c)
from (
  select t1.gid, count(t2.gid) as c
  from :table_name t1, 
       :table_name t2,
       (select gid from :table_name where random()<:epsilon) t3
  where t1.gid = t3.gid and st_intersects(t1.geom,t2.geom) group by t1.gid) T;
