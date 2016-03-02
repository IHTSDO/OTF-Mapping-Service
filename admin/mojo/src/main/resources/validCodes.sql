
# find all leaf nodes
drop table tbac;
create table tbac as
select terminologyId from concepts where terminology='ICD10' and id not in (select destinationConcept_id from relationships 

where typeId=7 and terminology='ICD10');

# handle M14
insert into tbac select distinct substr(terminologyId,1,5) from tbac where terminologyId like 'M14%';
delete from tbac where terminologyId like 'M14.__';


# handle W
insert into tbac select distinct substr(terminologyId,1,3) from tbac where terminologyId like 'W%';
delete from tbac where terminologyId like 'W__.__';
delete from tbac where terminologyId like 'W__._';

# handle X
insert into tbac select distinct substr(terminologyId,1,3) from tbac where terminologyId like 'X%'
and terminologyId not like 'X34%' and terminologyId not like 'X59%';
delete from tbac where terminologyId like 'X__.__';
delete from tbac where terminologyId like 'X__._'
and terminologyId not like 'X34%' and terminologyId not like 'X59%';

# Handle Y
insert into tbac select distinct substr(terminologyId,1,3) from tbac where terminologyId like 'Y0%'
and terminologyId not like 'Y06%' 
and terminologyId not like 'Y07%';
insert into tbac select distinct substr(terminologyId,1,3) from tbac where terminologyId like 'Y1%';
insert into tbac select distinct substr(terminologyId,1,3) from tbac where terminologyId like 'Y2%';
insert into tbac select distinct substr(terminologyId,1,3) from tbac where terminologyId like 'Y30%';
insert into tbac select distinct substr(terminologyId,1,3) from tbac where terminologyId like 'Y31%';
insert into tbac select distinct substr(terminologyId,1,3) from tbac where terminologyId like 'Y32%';
insert into tbac select distinct substr(terminologyId,1,3) from tbac where terminologyId like 'Y33%';
insert into tbac select distinct substr(terminologyId,1,3) from tbac where terminologyId like 'Y34%';

delete from tbac where terminologyId like 'Y__.__';
delete from tbac where terminologyId like 'Y0_._'
and terminologyId not like 'Y06%' and terminologyId not like 'Y07%';
delete from tbac where terminologyId like 'Y1_._';
delete from tbac where terminologyId like 'Y2_._';
delete from tbac where terminologyId like 'Y30._';
delete from tbac where terminologyId like 'Y31._';
delete from tbac where terminologyId like 'Y32._';
delete from tbac where terminologyId like 'Y33._';
delete from tbac where terminologyId like 'Y34._';
