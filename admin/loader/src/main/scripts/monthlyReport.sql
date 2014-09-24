
-- Monthly KPI status for reporting

-- Total mapped by project 
select 'Project productivity' name, mp.name, mr.workflowStatus value, DATE_FORMAT(from_unixtime(mr.lastModified/1000),'%Y-%m') date, count(*) 
from map_records mr, map_projects mp 
where mr.mapProjectId = mp.id
and from_unixtime(lastModified/1000) > date('2014-06-01')
and from_unixtime(lastModified/1000) < date('2014-12-01')
and workflowStatus IN ('READY_FOR_PUBLICATION')
group by mp.name, mr.workflowStatus,
   DATE_FORMAT(from_unixtime(mr.lastModified/1000),'%Y-%m');

-- Number of errors by specialist
select 'Specialist errors' name, userName, '' value, DATE_FORMAT(timestamp,'%Y-%m') date, count(*) 
from feedbacks, map_users, feedback_recipients 
where isError = '1' and feedback_recipients.feedbacks_id = feedbacks.id 
and feedback_recipients.recipients_id = map_users.id 
and timestamp > date('2014-06-01')
and timestamp < date('2014-12-01')
group by recipients_id,
   DATE_FORMAT(timestamp,'%Y-%m');

-- Total number of errors by type and specialist 
select 'Specialist errors by type' name, userName, mapError value, DATE_FORMAT(timestamp,'%Y-%m') date, count(*) 
from feedbacks, map_users, feedback_recipients where isError = '1' 
and timestamp > date('2014-06-01')
and timestamp < date('2014-12-01')
and feedback_recipients.feedbacks_id = feedbacks.id  
and feedback_recipients.recipients_id = map_users.id 
group by recipients_id, mapError,
   DATE_FORMAT(timestamp,'%Y-%m')
ORDER BY 2,3,4;

-- Principles being applied 
SELECT 'Map Principles' name, map_principles.id name, name value, DATE_FORMAT(from_unixtime(timestamp/1000),'%Y-%m') date, count(*) 
from map_principles, map_records_map_principles, map_records 
where mapPrinciples_id = map_principles.id 
and map_records_id = map_records.id 
and from_unixtime(timestamp/1000) > date('2014-06-01')
and from_unixtime(timestamp/1000) < date('2014-12-01')
group by name,
   DATE_FORMAT(from_unixtime(timestamp/1000),'%Y-%m')
ORDER BY 2,3,4;

-- Flagged for Map Lead Review by project
select 'Flagged for map lead review' name, map_projects.name name, '' value, DATE_FORMAT(from_unixtime(timestamp/1000),'%Y-%m') date, count(*)
from map_records, map_projects 
where flagForMapLeadReview is true
and map_records.mapProjectId = map_projects.id
and from_unixtime(lastModified/1000) > date('2014-06-01')
and from_unixtime(lastModified/1000) < date('2014-12-01')
group by mapProjectid,
   DATE_FORMAT(from_unixtime(lastModified/1000),'%Y-%m')
ORDER BY 2,3,4;

-- Flagged for Discrepancy Review by project
select 'Flagged for discrepancy review' name, map_projects.name name, '' value, DATE_FORMAT(lastModified,'%Y-%m') date, count(*)
from feedback_conversations, map_projects 
where isDiscrepancyReview is true 
and feedback_conversations.mapProjectId = map_projects.id
and lastModified > date('2014-06-01')
and lastModified < date('2014-12-01')
group by mapProjectid,
   DATE_FORMAT(lastModified,'%Y-%m')
ORDER BY 2,3,4;


-- Total mapped by map project (#concepts Published + #concepts in Ready for Publication)
select 'Total mapped' name, map_projects.name name, '' value, '' date, count(distinct conceptId)
from map_records, map_projects
where map_records.mapProjectId = map_projects.id
and workflowStatus = 'PUBLISHED' || workflowStatus = 'READY_FOR_PUBLICATION'
group by mapProjectid
ORDER BY 2,3,4;

-- Total waiting for leads to claim for confict resolution
select 'Available for conflict resolution' name, mp.name, '' value, '' date, count(*)
from tracking_records, map_projects mp
where tracking_records.mapProjectId = mp.id
and  userAndWorkflowStatusPairs LIKE '%CONFLICT_DETECTED%'
and  userAndWorkflowStatusPairs NOT LIKE '%CONFLICT_RESOLVED%'
and  userAndWorkflowStatusPairs NOT LIKE '%CONFLICT_IN_PROGRESS%'
and  userAndWorkflowStatusPairs NOT LIKE '%CONFLICT_NEW%'
group by mp.name;

-- Total concepts outstanding to be checked out
-- need help on logic (need to make distinct?)
select 'Available for checkout' name, mp.name, '' value, '' date, count(*)
from tracking_records, map_projects mp, map_records mr
where tracking_records.mapProjectId = mp.id
and mr.mapProjectId = mp.id
and workflowStatus not in ('READY_FOR_PUBLICATION', 'PUBLISHED')
and  mr.conceptId not in (select terminologyId from tracking_records)
group by mp.name;

 select distinct(conceptId) from tracking_records, map_records where workflowStatus not in ('READY_FOR_PUBLICATION', 'PUBLISHED') and conceptId not in (select terminologyId from tracking_records);

-- Total concepts that require finishing by specialists
select 'Require finishing by specialist' name, mp.name, '' value, '' date, count(*)
from tracking_records, map_projects mp
where tracking_records.mapProjectId = mp.id
and  (
(assignedUserCount = 1 and userAndWorkflowStatusPairs LIKE '%EDITING_IN_PROGRESS%')
or
(assignedUserCount = 1 and userAndWorkflowStatusPairs LIKE '%EDITING_DONE%')
or
(assignedUserCount = 2 and userAndWorkflowStatusPairs LIKE '%EDITING_IN_PROGRESS%')
)
group by mp.name;


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


-- Total concepts in this release (#concepts in Ready for Publication + # concepts in tracking_records) (make by map project)
-- need help on syntax
select  ( 
select count(distinct conceptId) 
from map_records 
where workflowStatus = 'READY_FOR_PUBLICATION'
) + ( 
select count(distinct terminologyId) 
from tracking_records) 
as count;

select 'Total concepts' name ( 
select count(distinct conceptId) 
from map_records , map_projects
where workflowStatus = 'READY_FOR_PUBLICATION'
and map_projects.id = map_records.mapProjectId
) + ( 
select count(distinct terminologyId) 
from tracking_records) 
as count
group by mapProjectId;

