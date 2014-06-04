-- For performance of searching map record history
-- we need an index on the owner_id.  The framework 
-- does not support creating indexes on audit tables
-- via annotations.
-- NOTE: this works with Oracle and MySQL but may not work for other environments.
CREATE INDEX x_map_records_AUD_1 on map_records_AUD (lastModifiedBy_id);

-- For performance of searching tree positions by ancestorPath is needed
-- NOTE: this works with MySQL but may not work for other environments.
CREATE INDEX x_tree_positions_1 on tree_positions (ancestorPath(255));

