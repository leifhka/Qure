CREATE OR REPLACE FUNCTION makeIns(raw_table text, ins_table text) RETURNS integer AS $$
BEGIN
  EXECUTE 'create table qure.' || raw_table || '_ins(gid int, block bigint)'; 
  EXECUTE 'insert into qure.' || raw_table || '_ins (select gid, block from qure.' || raw_table || ' where gid not in (select gid from ins.' || ins_table || '))'; 
  EXECUTE 'create index ' || raw_table || '_inss_gid_index on qure.' || raw_table || '_ins(gid)'; 
  EXECUTE 'create index ' || raw_table || '_inss_block_index on qure.' || raw_table || '_ins(block)';
  RETURN 1;
END;
$$ LANGUAGE plpgsql;
