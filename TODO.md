# High prioriy TODOs:

* Implement toSQL for before-relation.
* Implement new algorithm without redundant witnesses and split index from wit.

# Low prioriy TODOs:

* Implement compression of graphs on role-parts with equal relationships, e.g. set pi(i,a) = pi(b,a) if has same relationships
  and does not introduce new tuple in relations, or set overlaps-node o for ov(pi(i,a), c) equal to overlaps-node o' for
  ov(pi(b,a), c), if ov(pi(i,a),pi(b,a)) does not introduce new tuple in rels.
* Add functionaliy for roles on Blocks
* Implement alternate balancing based on tree-balancing rather than balanced splitting
