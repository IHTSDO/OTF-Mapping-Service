
-- Monthly KPI status for reporting

-- For August 2014
select count(*), b.name, workflowStatus
from map_records a, map_projects b
where a.mapProjectId = b.id
and from_unixtime(timestamp/1000) > date('2014-08-01')
and from_unixtime(timestamp/1000) < date('2014-09-01')
and workflowStatus IN ('READY_FOR_PUBLICATION')
group by b.name, workflowStatus;




