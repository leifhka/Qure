
create table geo.tiny(gid int, geom geometry);

insert into geo.tiny (values (0, ST_GeomFromText('Polygon((0 0, 2 0, 2 2, 0 2, 0 0))')),
                             (1, ST_GeomFromText('Polygon((1 1, 4 1, 4 4, 1 4, 1 1))')),
                             (2, ST_GeomFromText('Point(3 3)')),
                             (3, ST_GeomFromText('Polygon((2 2, 5 2, 5 5, 2 5, 2 2))')),
                             (4, ST_GeomFromText('Polygon((2.9 2.9, 2.9 4.9, 4.9 4.9, 4.9 2.9, 2.9 2.9))')),
                             (5, ST_GeomFromText('Polygon((6 1, 9 1, 9 4, 6 4, 6 1))')),
                             (6, ST_GeomFromText('Polygon((8 1, 11 1, 11 4, 8 4, 8 1))')),
                             (7, ST_GeomFromText('Polygon((7 3, 10 3, 10 6, 7 6, 7 3))')),
                             (8, ST_GeomFromText('Polygon((9 5, 12 5, 12 8, 9 8, 9 5))')),
                             (9, ST_GeomFromText('Polygon((14 9, 16 9, 16 7, 17 7, 17 9, 19 9, 19 10, 14 10, 14 9))')),
                             (10, ST_GeomFromText('Polygon((14 6, 19 6, 19 7, 17 7, 17 8, 16 8, 16 7, 14 7, 14 6))')),
                             (11, ST_GeomFromText('Polygon((14 6, 15 6, 15 10, 14 10, 14 6))')),
                             (12, ST_GeomFromText('Polygon((18 6, 19 6, 19 10, 18 10, 18 6))')));
