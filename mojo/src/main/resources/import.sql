INSERT INTO map_users VALUES (1, 'VIEWER', 'test@example.com', 'Loader', '','loader');
INSERT INTO map_users VALUES (2, 'VIEWER', 'test@example.com', 'QA Path', '','qa');
INSERT INTO map_users VALUES (3, 'VIEWER', 'test@example.com', 'Guest', '','guest');
--INSERT INTO map_users VALUES (4, 'VIEWER', 'test@example.com', 'Demo_Lead', '','demo_lead');

-- Need to insert history too (use revision 0)
INSERT INTO REVINFO values (1,0);
INSERT INTO map_users_AUD VALUES (1,1,0, 'VIEWER', 'test@example.com', 'Loader', '','loader');
INSERT INTO map_users_AUD VALUES (2,1,0, 'VIEWER', 'test@example.com', 'QA Path', '','qa');
INSERT INTO map_users_AUD VALUES (3,1,0, 'VIEWER', 'test@example.com', 'Guest', '','guest');
--INSERT INTO map_users_AUD VALUES (4,1,0, 'VIEWER', 'test@example.com', 'Demo Lead', '','demo_lead');

-- For performance of searching map record history
-- we need an index on the owner_id.  The framework 
-- does not support creating indexes on audit tables
-- via annotations.
-- NOTE: this works with Oracle and MySQL but may not work for other environments.
CREATE INDEX x_map_records_AUD_1 on map_records_AUD (lastModifiedBy_id);

-- NOTE: this works with Oracle and MySQL but may not work for other environments.
CREATE INDEX x_map_records_AUD_2 on map_records_AUD (conceptId);
CREATE INDEX x_map_records_2 on map_records (conceptId);

-- NOTE: this works with Oracle and MySQL but may not work for other environments.
CREATE INDEX x_map_records_AUD_3 on map_records_AUD (mapProjectId);
CREATE INDEX x_map_records on map_records(mapProjectId);

-- NOTE: this works with Oracle and MySQL but may not work for other environments.
CREATE INDEX x_map_records_AUD_4 on map_records_AUD (id);

-- NOTE: this works with Oracle and MySQL but may not work for other environments.
CREATE INDEX x_map_entries_AUD_1 on map_entries_AUD (mapRecord_id);

-- NOTE: this works with Oracle and MySQL but may not work for other environments.
CREATE INDEX x_map_records_origin_AUD_1 on map_records_origin_ids_AUD (id);

-- For performance of searching tree positions by ancestorPath is needed
-- NOTE: this works with MySQL but may not work for other environments.
CREATE INDEX x_tree_positions_1 on tree_positions (ancestorPath(255));

CREATE INDEX x_tree_positions_2 on tree_positions (terminology);

CREATE INDEX x_map_records_map_notes_AUD on map_records_map_notes_AUD(map_records_id);



