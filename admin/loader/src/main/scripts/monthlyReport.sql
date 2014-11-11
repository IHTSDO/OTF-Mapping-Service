-- add headers
select 'Name' name, 'Project', 'Value', 'Date', 'Count';

-- Monthly KPI status for reporting

-- Total mapped by project 
select 'Project productivity' name, mp.name, mr.workflowStatus value, DATE_FORMAT(from_unixtime(mr.lastModified/1000),'%Y-%m') date, count(*) ct
from map_records mr, map_projects mp 
where mr.mapProjectId = mp.id
and from_unixtime(lastModified/1000) > date('2014-06-01')
and from_unixtime(lastModified/1000) < date('2014-12-01')
and workflowStatus IN ('READY_FOR_PUBLICATION')
group by mp.name, mr.workflowStatus,
   DATE_FORMAT(from_unixtime(mr.lastModified/1000),'%Y-%m');

-- Number of errors by specialist
select 'Specialist errors' name, map_projects.name, map_users.userName value, DATE_FORMAT(timestamp,'%Y-%m') date, count(*) ct 
from feedbacks, map_users, feedback_recipients, map_projects, feedback_conversations
where isError = '1' 
and feedbacks.feedbackConversation_id = feedback_conversations.id
and map_projects.id = feedback_conversations.mapProjectId
and feedback_recipients.feedbacks_id = feedbacks.id 
and feedback_recipients.recipients_id = map_users.id 
and timestamp > date('2014-06-01')
and timestamp < date('2014-12-01')
group by map_projects.name, recipients_id,
   DATE_FORMAT(timestamp,'%Y-%m');

-- Total number of errors by type and specialist 
select 'Specialist errors by type' name, map_projects.name name, 
     concat(map_users.userName, ',', mapError) value, DATE_FORMAT(timestamp,'%Y-%m') date, count(*) ct
from feedbacks, map_users, feedback_recipients , map_projects, feedback_conversations
where isError = '1' 
and feedbacks.feedbackConversation_id = feedback_conversations.id
and map_projects.id = feedback_conversations.mapProjectId
and timestamp > date('2014-06-01')
and timestamp < date('2014-12-01')
and feedback_recipients.feedbacks_id = feedbacks.id  
and feedback_recipients.recipients_id = map_users.id 
group by map_projects.name, recipients_id, mapError,
   DATE_FORMAT(timestamp,'%Y-%m')
ORDER BY 2,3,4;

-- Total mapped by project and specialist
select 'Specialist productivity' name, mp.name, mu.userName value,
         DATE_FORMAT(from_unixtime(mra.lastModified/1000),'%Y-%m') date, 
     count(distinct mra.id) ct
from map_records mr, map_projects mp, 
     map_records_origin_ids mroi, map_records_AUD mra,
     map_users mu
where mra.mapProjectId = mp.id
and mr.id = mroi.id
and mroi.originIds = mra.id
and mra.owner_id = mu.id
and mr.workflowStatus IN ('READY_FOR_PUBLICATION')
and mu.username != 'loader'
and from_unixtime(mr.lastModified/1000) > date('2014-06-01')
and from_unixtime(mr.lastModified/1000) < date('2014-12-01')
group by mp.name, mu.userName,
   DATE_FORMAT(from_unixtime(mra.lastModified/1000),'%Y-%m')
ORDER BY 2,3,4;

-- Errors/total mapped by project and specialist
select 'Specialist error rate by error' name, mp.name project, 
  concat(total.userName,',',errors.mapError) value, 
  total.dateRange date, format(errors.ct*100/total.ct,2) pct, errors.ct, total.ct
from map_projects mp, 
 (select fc.mapProjectId, mu.userName, f.mapError,
         DATE_FORMAT(f.timestamp,'%Y-%m') dateRange, count(distinct fc.terminologyId) ct
  from feedbacks f, map_users mu, feedback_recipients fr, feedback_conversations fc
  where isError = '1' 
  and f.feedbackConversation_id = fc.id
  and timestamp > date('2014-06-01')
  and timestamp < date('2014-12-01')
  and fr.feedbacks_id = f.id  
  and fr.recipients_id = mu.id 
  group by fc.mapProjectId, mu.userName, f.mapError, DATE_FORMAT(timestamp,'%Y-%m')) errors,
 (select mr.mapProjectId, mu.userName, 
    DATE_FORMAT(from_unixtime(mra.lastModified/1000),'%Y-%m') dateRange, count(distinct mr.conceptId) ct
  from map_records mr, map_records_origin_ids mroi, map_records_AUD mra, map_users mu
  where mr.id = mroi.id
  and mroi.originIds = mra.id
  and mra.owner_id = mu.id
  and mr.workflowStatus IN ('READY_FOR_PUBLICATION')
  and mu.username != 'loader'
  and from_unixtime(mr.lastModified/1000) > date('2014-06-01')
  and from_unixtime(mr.lastModified/1000) < date('2014-12-01')
  group by mr.mapProjectId, mu.userName, DATE_FORMAT(from_unixtime(mra.lastModified/1000),'%Y-%m')) total
where mp.id = errors.mapProjectId
  and mp.id = total.mapProjectId
  and errors.userName = total.userName
  and errors.dateRange = total.dateRange;

-- Principles being applied 
SELECT 'Map Principles' name, map_projects.name name, concat(map_principles.principleId, ',', map_principles.name) value, DATE_FORMAT(from_unixtime(timestamp/1000),'%Y-%m') date, count(*) ct
from map_principles, map_records_map_principles, map_records, map_projects
where mapPrinciples_id = map_principles.id 
and map_records_id = map_records.id 
and map_records.mapProjectId = map_projects.id
and from_unixtime(timestamp/1000) > date('2014-06-01')
and from_unixtime(timestamp/1000) < date('2014-12-01')
group by map_projects.name, map_principles.principleId, 
   DATE_FORMAT(from_unixtime(timestamp/1000),'%Y-%m')
ORDER BY 2,3,4;

-- Flagged for Map Lead Review by project
select 'Flagged for map lead review' name, map_projects.name name, '' value, DATE_FORMAT(from_unixtime(timestamp/1000),'%Y-%m') date, count(*) ct
from map_records, map_projects 
where flagForMapLeadReview is true
and map_records.mapProjectId = map_projects.id
and from_unixtime(lastModified/1000) > date('2014-06-01')
and from_unixtime(lastModified/1000) < date('2014-12-01')
group by mapProjectid,
   DATE_FORMAT(from_unixtime(lastModified/1000),'%Y-%m')
ORDER BY 2,3,4;

-- Flagged for Discrepancy Review by project
select 'Flagged for discrepancy review' name, map_projects.name name, '' value, DATE_FORMAT(lastModified,'%Y-%m') date, count(*) ct
from feedback_conversations, map_projects 
where isDiscrepancyReview is true 
and feedback_conversations.mapProjectId = map_projects.id
and lastModified > date('2014-06-01')
and lastModified < date('2014-12-01')
group by mapProjectid,
   DATE_FORMAT(lastModified,'%Y-%m')
ORDER BY 2,3,4;


-- Total mapped by map project (#concepts Published + #concepts in Ready for Publication)
select 'Total mapped' name, map_projects.name name,
   '' value, '' date, count(distinct conceptId)
from map_records, map_projects
where map_records.mapProjectId = map_projects.id
and workflowStatus in ('PUBLISHED','READY_FOR_PUBLICATION')
group by mapProjectid
ORDER BY 2,3,4;

-- Total waiting for leads to claim for confict resolution
select 'Available for conflict resolution' name, mp.name, '' value, '' date, count(*) ct
from tracking_records, map_projects mp
where tracking_records.mapProjectId = mp.id
and  userAndWorkflowStatusPairs LIKE '%CONFLICT_DETECTED%'
and  userAndWorkflowStatusPairs NOT LIKE '%CONFLICT_RESOLVED%'
and  userAndWorkflowStatusPairs NOT LIKE '%CONFLICT_IN_PROGRESS%'
and  userAndWorkflowStatusPairs NOT LIKE '%CONFLICT_NEW%'
group by mp.name;

-- Total concepts that require finishing by specialists
-- NOTE: this assumes NON_LEGACY_PATH or FIX_ERROR_PATH
select 'Requires specialist to finish editing' name, mp.name, '' value, '' date, count(*) ct
from tracking_records, map_projects mp
where tracking_records.mapProjectId = mp.id
and assignedUserCount > 0
and userAndWorkflowStatusPairs not like '%CONFLICT_DETECTED%'
and userAndWorkflowStatusPairs not like '%REVIEW_NEEDED%'
group by mp.name;


-- Total concepts in this release (by map project)
--  (#concepts finished + #concepts in workflow)
--  (#concepts in Ready for Publication + # concepts in tracking_records) 
-- need help on syntax

select 'Total concepts by workflow status' name,
   mp.name, workflowStatus value, '' date, count(*) ct
from map_projects mp,
(select mapProjectId, conceptId, 'Ready for publication' workflowStatus
 from map_records where workflowStatus = 'READY_FOR_PUBLICATION'
 union
 select mapProjectId, conceptId, 'Published'
 from map_records where workflowStatus = 'PUBLISHED'
 union
 select mapProjectId, terminologyId, 'In progress'
 from tracking_records) data
where mp.id = data.mapProjectId
group by mp.id, workflowStatus;

select 'Total concepts edited' name,
   mp.name, '' value, '' date, count(*) ct
from map_projects mp,
(select mapProjectId, conceptId
 from map_records where workflowStatus = 'READY_FOR_PUBLICATION'
 union
 select mapProjectId, terminologyId
 from tracking_records) data
where mp.id = data.mapProjectId
group by mp.id;


-- Total concepts outstanding to be mapped
-- This is essentially the same as the available work count
-- look for tracking records where assignedUserCount = 0
select 'Total concepts outstanding to be mapped' name,
    mp.name project, '' value, '' date, ifnull(ct,0) ct
from map_projects mp
left outer join 
(select count(*) ct, mapProjectId
 from tracking_records 
 where assignedUserCount = 0
 group by mapProjectId) tr
on tr.mapProjectId = mp.id;

-- Concepts flagged for editorial
select 'Concepts flagged for editorial' name, 
    mp.name project, '' value, '' date, ifnull(ct,0) ct
from map_projects mp
left outer join 
(select count(distinct conceptId) ct, mapProjectId
 from map_records_AUD
 where flagForEditorialReview = 1
 group by mapProjectId) tr
on tr.mapProjectId = mp.id;

-- Concepts with MAPPING GUIDANCE FROM WHO IS AMBIGUOUS
select 'Concepts with MAPPING GUIDANCE FROM WHO IS AMBIGUOUS' name, 
    mp.name project, '' value, '' date, ifnull(ct,0) ct
from map_projects mp
left outer join 
(select count(distinct conceptId) ct, mapProjectId
 from map_records_AUD mr, map_entries_AUD me, map_relations rel
 where mr.id = me.mapRecord_Id
   and me.mapRelation_id = rel.id
   and rel.name = 'MAPPING GUIDANCE FROM WHO IS AMBIGUOUS' 
 group by mapProjectId) tr
on tr.mapProjectId = mp.id;

-- Concepts with MAPPING GUIDANCE FROM WHO IS AMBIGUOUS
select 'Concepts with SOURCE SNOMED CONCEPT IS AMBIGUOUS' name, 
    mp.name project, '' value, '' date, ifnull(ct,0) ct
from map_projects mp
left outer join 
(select count(distinct conceptId) ct, mapProjectId
 from map_records_AUD mr, map_entries_AUD me, map_relations rel
 where mr.id = me.mapRecord_Id
   and me.mapRelation_id = rel.id
   and rel.name = 'SOURCE SNOMED CONCEPT IS AMBIGUOUS'
 group by mapProjectId) tr
on tr.mapProjectId = mp.id;

select 'Mapped concepts in Unmapped SNOMED to ICD10' name,
    mp.name project, '' value, '' date, count(distinct mr.conceptId)
from map_projects mp, map_records mr
where mapProjectId = mp.id
and mp.id = 10
group by mp.name;

select now();
