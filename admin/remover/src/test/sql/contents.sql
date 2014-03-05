--
-- This script is used to verify that removers properly clean
-- up the coresponding data they are intended to without orphans.
--
-- Written for MYSQL or Oracle
--
-- NOTE: "_aud" tables are note checked because some removers use
--      the framework.  The only way to completely get rid of audit
--      trail info is to delete the entire database and reload from scratch
--

-- terminology data
select 'attribute_value_refset_members', count(*) from attribute_value_refset_members;
select 'complex_map_refset_members', count(*) from complex_map_refset_members;
select 'concepts', count(*) from concepts;
select 'descriptions', count(*) from descriptions;
select 'language_refset_members', count(*) from language_refset_members;
select 'relationships', count(*) from relationships;
select 'simple_map_refset_members', count(*) from simple_map_refset_members;
select 'simple_refset_members', count(*) from simple_refset_members;

-- map project data
select 'map_notes', count(*) from map_notes;
select 'map_advices', count(*) from map_advices;
select 'map_principles', count(*) from map_principles;
select 'map_users', count(*) from map_users;
select 'map_projects', count(*) from map_projects;
select 'map_projects_aud', count(*) from map_projects_aud;
select 'map_projects_map_advices', count(*) from map_projects_map_advices;
select 'map_projects_map_leads', count(*) from map_projects_map_leads;
select 'map_projects_map_principles', count(*) from map_projects_map_principles;
select 'map_projects_map_specialists', count(*) from map_projects_map_specialists;
select 'map_projects_rule_preset_age_ranges', count(*) from map_projects_rule_preset_age_ranges;

-- map records data
select 'map_entries', count(*) from map_entries;
select 'map_entries_map_advices', count(*) from map_entries_map_advices;
select 'map_entries_map_notes', count(*) from map_entries_map_notes;
select 'map_entries_map_principles', count(*) from map_entries_map_principles;
select 'map_records', count(*) from map_records;
select 'map_records_map_notes', count(*) from map_records_map_notes;
select 'map_records_map_principles', count(*) from map_records_map_principles;
