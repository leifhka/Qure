# High prioriy TODOs:

* Implement toSQL for before-relation.
* Implement compression of graphs on role-parts with equal relationships, e.g. set pi(i,a) = pi(b,a) if has same relationships
  and does not introduce new tuple in relations, or set overlaps-node o for ov(pi(i,a), c) equal to overlaps-node o' for
  ov(pi(b,a), c), if ov(pi(i,a),pi(b,a)) does not introduce new tuple in rels.

# Low prioriy TODOs:

* Add functionaliy for roles on Blocks
* Implement alternate balancing based on tree-balancing rather than balanced splitting
* Check whether it is more efficient to not intersect spaces with blocks when splitting

# Not to be done (I think):

* Implement new algorithm without redundant witnesses and split index from wit.
  * (5) For each split, send other node's overlapping to each node so that it knows which SIDs overlap unvisited nodes
  * (10) Keep graph from one leaf-node to next
  * (10) Only make representations for and delete (from RelationshipGraph) each SID not overlapping any unvisited leaf-nodes
  * (10) Add covering SIDs to overlaps-set, but also set to overlap all other overlapping SIDs.
  * (10) Compute only relationships for tuples containing at least one new SID (not overlapping visited leaf-nodes)
  * (10) Whenever an overlaps-node is added to a representation (according to prev. point) keep it in graph as long as it has
    parents, but mark it as added, and remove the removed SID from its parents. Remove the 'added' mark if overlap becomes
    extended with new parents. Do not add an added node to a rep.
  * (5) Store blocks for index structure while traversing
