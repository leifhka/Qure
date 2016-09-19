#Qure

Welcome to Qure, a program that constructs qualitatively
correct bintree representations of spatial objects
(geometries, time intervals, etc.). The bintrees
constructed are correct with respect to
containment and overlaps-queries (up to
a user-specified arity).

To use the program, make a Config-object with
the desired parameters. To query the
resulting bintrees, use the SQL-queries
in the queries-directory.

To compile, simply run

    make compile

and to run, execute

    make run
